<template>
  <div class="container">
    <div class="header">
      <div class="back-button">
        <el-button plain @click="goBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
      </div>
      
      <h2>模型详情</h2>
    </div>

    <el-card v-loading="loading">
      <div v-if="modelDetail">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="ID">{{ modelDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ modelDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ modelDetail.description }}</el-descriptions-item>
          <el-descriptions-item label="数据集">{{ modelDetail.datasetName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(modelDetail.status)">
              {{ getStatusText(modelDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(modelDetail.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section">
          <h3>数据集信息</h3>
          <div class="action-buttons">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleDownloadDataset(modelDetail.id)"
              :loading="downloadDatasetLoading"
            >
              下载数据集
            </el-button>
          </div>
        </div>

        <!-- 如果模型训练成功，显示训练指标和结果 -->
        <template v-if="modelDetail.status === 2 || modelDetail.status === 4">
          <div class="section">
            <h3>训练超参数</h3>
            <el-descriptions :column="3" border v-if="hyperparams">
              <el-descriptions-item label="输入图像大小">{{ hyperparams.inputSize }}</el-descriptions-item>
              <el-descriptions-item label="输入通道数">{{ hyperparams.inChannels }}</el-descriptions-item>
              <el-descriptions-item label="类别数量">{{ hyperparams.numClasses }}</el-descriptions-item>
              <el-descriptions-item label="训练轮数">{{ hyperparams.epochs }}</el-descriptions-item>
              <el-descriptions-item label="批量大小">{{ hyperparams.batchSize }}</el-descriptions-item>
              <el-descriptions-item label="学习率">{{ hyperparams.learningRate }}</el-descriptions-item>
              <el-descriptions-item label="数据增强级别">{{ hyperparams.augLevel }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="section">
            <h3>训练结果</h3>
            <div class="action-buttons">
              <el-button 
                type="success" 
                size="small" 
                @click="handleDownloadModel(modelDetail.id)"
                :loading="downloadModelLoading"
              >
                下载模型权重文件
              </el-button>
              
              <el-button 
                v-if="modelDetail.status === 2" 
                type="primary" 
                size="small" 
                @click="handlePublishModel(modelDetail.id)"
              >
                发布模型
              </el-button>
            </div>

            <div v-if="trainMetrics" class="metrics-container">
              <div class="best-metrics">
                <h4>最佳训练结果</h4>
                <el-descriptions :column="3" border>
                  <el-descriptions-item 
                    v-for="(value, key) in trainMetrics.best_metrics" 
                    :key="key" 
                    :label="formatMetricName(String(key))"
                  >
                    {{ formatMetricValue(value) }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <div class="training-charts">
                <h4>训练过程</h4>
                <div class="chart-container">
                  <div class="chart">
                    <h5>损失曲线</h5>
                    <div ref="lossChartRef" class="echarts-container"></div>
                  </div>
                  <div class="chart">
                    <h5>评价指标</h5>
                    <div ref="metricsChartRef" class="echarts-container"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 如果模型训练失败，显示错误信息 -->
        <template v-if="modelDetail.status === 3">
          <div class="section error-section">
            <h3>训练失败</h3>
            <el-alert
              title="训练过程中出现错误"
              type="error"
              :closable="false"
              show-icon
            >
              <div class="error-message">{{ modelDetail.errorMsg }}</div>
            </el-alert>
          </div>
        </template>

        <!-- 如果模型正在训练，显示状态 -->
        <template v-if="modelDetail.status === 1">
          <div class="section">
            <h3>训练状态</h3>
            <div class="training-status">
              <el-progress type="circle" :percentage="100" status="warning" :indeterminate="true" />
              <p>模型正在训练中，请稍后刷新页面查看结果...</p>
            </div>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { 
  getModelDetail, 
  publishModel,
  trainModel,
  deleteModel
} from '@/api/model'
import { fileRequest } from '@/api/file_request'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

// 状态相关
const STATUS_UNTRAINED = 0
const STATUS_TRAINING = 1
const STATUS_TRAINED_SUCCESS = 2
const STATUS_TRAINED_FAILED = 3
const STATUS_PUBLISHED = 4

// 获取状态文本
const getStatusText = (status: number) => {
  switch (status) {
    case STATUS_UNTRAINED: return '未训练'
    case STATUS_TRAINING: return '训练中'
    case STATUS_TRAINED_SUCCESS: return '训练成功'
    case STATUS_TRAINED_FAILED: return '训练失败'
    case STATUS_PUBLISHED: return '已发布'
    default: return '未知状态'
  }
}

// 获取状态类型
const getStatusType = (status: number) => {
  switch (status) {
    case STATUS_UNTRAINED: return 'info'
    case STATUS_TRAINING: return 'warning'
    case STATUS_TRAINED_SUCCESS: return 'success'
    case STATUS_TRAINED_FAILED: return 'danger'
    case STATUS_PUBLISHED: return 'success'
    default: return 'info'
  }
}

// 数据
const loading = ref(false)
const modelDetail = ref<any>(null)
const hyperparams = ref<any>(null)
const trainMetrics = ref<any>(null)
const downloadModelLoading = ref(false)
const downloadDatasetLoading = ref(false)

// 图表引用
const lossChartRef = ref<HTMLElement | null>(null)
const metricsChartRef = ref<HTMLElement | null>(null)
const lossChart = ref<echarts.ECharts | null>(null)
const metricsChart = ref<echarts.ECharts | null>(null)

// 定义emit向父组件发送事件
const emit = defineEmits(['openTrainDialog'])

// 获取模型详情
const fetchModelDetail = async () => {
  const modelId = Number(route.params.id)
  if (!modelId) {
    ElMessage.error('无效的模型ID')
    return
  }

  try {
    loading.value = true
    const res = await getModelDetail(modelId)
    modelDetail.value = res
    
    // 解析训练超参数
    if (modelDetail.value.trainHyperparams) {
      try {
        hyperparams.value = JSON.parse(modelDetail.value.trainHyperparams)
      } catch (e) {
        console.error('解析训练超参数失败', e)
      }
    }

    // 解析训练指标
    if (modelDetail.value.trainMetrics) {
      try {
        trainMetrics.value = JSON.parse(modelDetail.value.trainMetrics)
        // 图表初始化需要在DOM元素渲染后进行
        setTimeout(() => {
          initCharts()
        }, 100)
      } catch (e) {
        console.error('解析训练指标失败', e)
      }
    }
  } catch (error: any) {
    ElMessage.error('获取模型详情失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 初始化图表
const initCharts = () => {
  if (!trainMetrics.value || !trainMetrics.value.history) {
    console.error('训练指标数据为空或格式不正确')
    return
  }

  // 初始化损失曲线图表
  if (lossChartRef.value) {
    lossChart.value = echarts.init(lossChartRef.value)
    
    // 确保历史数据存在并有效
    const trainLoss = trainMetrics.value.history.train_loss || []
    const valLoss = trainMetrics.value.history.val_loss || []
    
    // 检查数据数组是否为空
    if (trainLoss.length === 0 && valLoss.length === 0) {
      console.warn('训练和验证损失数据为空')
      return
    }
    
    // 使用最长的数组长度来确定x轴
    const maxLength = Math.max(trainLoss.length, valLoss.length)
    const xAxis = Array.from({ length: maxLength }, (_, i) => i + 1)

    // 确保数据不为空，并且每个数据点都有效
    const safeTrainLoss = trainLoss.map((val: any) => val === null || val === undefined ? '-' : val)
    const safeValLoss = valLoss.map((val: any) => val === null || val === undefined ? '-' : val)

    const option = {
      animation: true,
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          animation: true,
          lineStyle: {
            color: '#6a7985',
            width: 1,
            type: 'dashed'
          }
        },
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        borderColor: '#ccc',
        borderWidth: 1,
        textStyle: {
          color: '#333'
        },
        formatter: function(params: any) {
          let result = `第 ${params[0].dataIndex + 1} 轮<br/>`
          params.forEach((param: any) => {
            const value = param.value !== null && param.value !== '-' ? param.value.toFixed(4) : '无数据'
            const color = param.color
            result += `<span style="display:inline-block;margin-right:4px;border-radius:50%;width:10px;height:10px;background-color:${color}"></span>${param.seriesName}: ${value}<br/>`
          })
          return result
        }
      },
      legend: {
        data: ['训练损失', '验证损失'],
        selectedMode: false // 禁用图例点击交互
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xAxis,
        name: '轮次',
        boundaryGap: false
      },
      yAxis: {
        type: 'value',
        name: '损失',
        scale: true
      },
      series: [
        {
          name: '训练损失',
          type: 'line',
          data: safeTrainLoss,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: true,
          sampling: 'average',
          itemStyle: {
            color: '#FF6B5E'
          },
          emphasis: {
            focus: 'series',
            itemStyle: {
              borderWidth: 2
            }
          }
        },
        {
          name: '验证损失',
          type: 'line',
          data: safeValLoss,
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: true,
          sampling: 'average',
          itemStyle: {
            color: '#5470C6'
          },
          emphasis: {
            focus: 'series',
            itemStyle: {
              borderWidth: 2
            }
          }
        }
      ]
    }
    
    try {
      lossChart.value.setOption(option, true)
    } catch (error) {
      console.error('设置损失图表选项时出错:', error)
    }
  }

  // 初始化评价指标图表
  if (metricsChartRef.value && trainMetrics.value.history.metrics) {
    try {
      metricsChart.value = echarts.init(metricsChartRef.value)
      
      // 确保指标数据存在
      const metrics = trainMetrics.value.history.metrics || []
      
      // 检查指标数组是否为空
      if (metrics.length === 0) {
        console.warn('评价指标数据为空')
        return
      }
      
      // 提取dice和iou值
      const diceValues = metrics.map((m: any) => {
        if (!m || typeof m.dice === 'undefined') return null
        const diceValue = parseFloat(m.dice)
        return isNaN(diceValue) ? null : diceValue
      })
      
      const iouValues = metrics.map((m: any) => {
        if (!m || typeof m.iou === 'undefined') return null
        const iouValue = parseFloat(m.iou)
        return isNaN(iouValue) ? null : iouValue
      })
      
      // 计算y轴的最大值和最小值
      const allValues = [...diceValues, ...iouValues].filter(v => v !== null)
      const minValue = Math.min(...allValues)
      const maxValue = Math.max(...allValues)
      // 扩大y轴范围，使曲线更好地占据图表空间
      const padding = (maxValue - minValue) * 0.2 // 使用20%的padding
      const yAxisMin = Math.max(0, minValue - padding) // 最小值不小于0
      const yAxisMax = Math.min(1, maxValue + padding) // 最大值不超过1
      
      const xAxis = Array.from({ length: metrics.length }, (_, i) => i + 1)

      const option = {
        animation: true,
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross',
            animation: true,
            lineStyle: {
              color: '#6a7985',
              width: 1,
              type: 'dashed'
            }
          },
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#ccc',
          borderWidth: 1,
          textStyle: {
            color: '#333'
          },
          formatter: function(params: any) {
            let result = `第 ${params[0].dataIndex + 1} 轮<br/>`
            params.forEach((param: any) => {
              const value = param.value !== null ? param.value.toFixed(4) : '无数据'
              const percent = param.value !== null ? (param.value * 100).toFixed(2) + '%' : '无数据'
              const color = param.color
              result += `<span style="display:inline-block;margin-right:4px;border-radius:50%;width:10px;height:10px;background-color:${color}"></span>${param.seriesName}: ${value} (${percent})<br/>`
            })
            return result
          }
        },
        legend: {
          data: ['Dice系数', 'IoU系数'],
          selectedMode: false // 禁用图例点击交互
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xAxis,
          name: '轮次',
          boundaryGap: false
        },
        yAxis: {
          type: 'value',
          name: '评价指标',
          min: yAxisMin,
          max: yAxisMax,
          scale: true,
          axisLabel: {
            formatter: (value: number) => value.toFixed(2) // 格式化y轴标签，只显示两位小数
          }
        },
        series: [
          {
            name: 'Dice系数',
            type: 'line',
            data: diceValues,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            showSymbol: true,
            sampling: 'average',
            itemStyle: {
              color: '#67C23A'
            },
            emphasis: {
              focus: 'series',
              itemStyle: {
                borderWidth: 2
              }
            },
            connectNulls: true
          },
          {
            name: 'IoU系数',
            type: 'line',
            data: iouValues,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            showSymbol: true,
            sampling: 'average',
            itemStyle: {
              color: '#E6A23C'
            },
            emphasis: {
              focus: 'series',
              itemStyle: {
                borderWidth: 2
              }
            },
            connectNulls: true
          }
        ]
      }
      
      metricsChart.value.setOption(option, true)
    } catch (error) {
      console.error('初始化评价指标图表失败:', error)
    }
  }

  // 添加窗口大小变化时自动调整图表大小
  window.addEventListener('resize', handleResize)
}

// 处理窗口大小变化
const handleResize = () => {
  if (lossChart.value) {
    try {
      lossChart.value.resize()
    } catch (e) {
      console.error('调整损失图表大小失败:', e)
    }
  }
  
  if (metricsChart.value) {
    try {
      metricsChart.value.resize()
    } catch (e) {
      console.error('调整评价指标图表大小失败:', e)
    }
  }
}

// 格式化指标名称
const formatMetricName = (key: string) => {
  const nameMap: Record<string, string> = {
    'dice': 'Dice系数',
    'iou': 'IoU'
  }
  return nameMap[key] || key
}

// 格式化指标值
const formatMetricValue = (value: any) => {
  // 检查值是否为数字或可以转换为数字
  if (value === null || value === undefined) {
    return '—'; // 使用破折号表示缺失值
  }
  
  if (typeof value === 'number') {
    return value.toFixed(4);
  } 
  
  if (typeof value === 'string' && !isNaN(Number(value))) {
    return Number(value).toFixed(4);
  }
  
  if (Array.isArray(value)) {
    return '[Array]'; // 数组显示为特殊标记
  }
  
  if (typeof value === 'object') {
    return '{Object}'; // 对象显示为特殊标记
  }
  
  // 其他类型直接转为字符串
  return String(value);
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 下载模型
const handleDownloadModel = async (modelId: number) => {
  try {
    downloadModelLoading.value = true
    
    // 先获取模型详情，获取模型关联的权重文件信息
    const modelInfo = await getModelDetail(modelId)
    
    if (!modelInfo.modelBucket || !modelInfo.modelObjectKey) {
      ElMessage.error('未找到模型权重文件信息')
      return
    }
    
    // 使用file_request直接下载文件
    const response = await fileRequest.get(modelInfo.modelBucket, modelInfo.modelObjectKey)
    
    // 创建下载链接
    const blob = new Blob([response], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `model_${modelId}.pth`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('模型下载已开始')
  } catch (error: any) {
    ElMessage.error('下载模型失败: ' + error.message)
  } finally {
    downloadModelLoading.value = false
  }
}

// 下载数据集
const handleDownloadDataset = async (modelId: number) => {
  try {
    downloadDatasetLoading.value = true
    
    // 先获取模型详情，获取模型关联的数据集信息
    const modelInfo = await getModelDetail(modelId)
    
    if (!modelInfo.datasetBucket || !modelInfo.datasetObjectKey) {
      ElMessage.error('未找到模型关联的数据集信息')
      return
    }
    
    // 使用file_request直接下载文件
    const response = await fileRequest.get(modelInfo.datasetBucket, modelInfo.datasetObjectKey)
    
    // 创建下载链接
    const blob = new Blob([response], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `dataset_for_model_${modelId}.zip`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('数据集下载已开始')
  } catch (error: any) {
    ElMessage.error('下载数据集失败: ' + error.message)
  } finally {
    downloadDatasetLoading.value = false
  }
}

// 发布模型
const handlePublishModel = async (modelId: number) => {
  try {
    await publishModel(modelId)
    ElMessage.success('模型发布成功')
    fetchModelDetail() // 刷新详情
  } catch (error: any) {
    ElMessage.error('发布模型失败: ' + error.message)
  }
}

// 生命周期钩子
onMounted(() => {
  fetchModelDetail()
})

onBeforeUnmount(() => {
  // 清理图表实例
  window.removeEventListener('resize', handleResize)
  lossChart.value?.dispose()
  metricsChart.value?.dispose()
})
</script>

<style scoped>
.container {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.back-button {
  margin-right: 20px;
}

.section {
  margin-top: 30px;
}

.action-buttons {
  margin: 10px 0 20px;
  display: flex;
  gap: 10px;
}

.metrics-container {
  margin-top: 20px;
}

.training-charts {
  margin-top: 30px;
}

.chart-container {
  display: flex;
  gap: 20px;
  margin-top: 10px;
}

.chart {
  flex: 1;
  min-width: 0;
}

.echarts-container {
  height: 300px;
  width: 100%;
}

.training-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px 0;
  gap: 20px;
}

.error-section {
  margin-top: 20px;
}

.error-message {
  margin-top: 10px;
  white-space: pre-line;
  font-family: monospace;
  background: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
}
</style>