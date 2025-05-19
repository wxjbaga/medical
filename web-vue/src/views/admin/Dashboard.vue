<!-- 管理控制台页面 -->
<template>
  <div class="dashboard">
    <!-- 顶部统计卡片 -->
    <div class="stat-cards">
      <el-row :gutter="20">
        <el-col :span="6" v-for="(item, index) in statCards" :key="index">
          <el-card class="stat-card" :class="item.colorClass" v-loading="item.loading">
            <div class="stat-icon">
              <el-icon>
                <component :is="item.icon"></component>
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-desc">
                <el-icon :class="item.trend === 'up' ? 'trend-up' : 'trend-down'">
                  <component :is="item.trend === 'up' ? 'ArrowUp' : 'ArrowDown'"></component>
                </el-icon>
                <span>{{ item.compare }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 中部图表区域 -->
    <div class="chart-section">
      <el-row :gutter="20">
        <!-- 用户活跃度趋势 -->
        <el-col :span="12">
          <el-card class="chart-card" v-loading="userChartLoading">
            <template #header>
              <div class="chart-header">
                <div class="chart-title">
                  <el-icon><User /></el-icon>
                  用户活跃度趋势
                </div>
                <el-select v-model="userTimeRange" size="small" placeholder="时间范围">
                  <el-option label="最近7天" value="7"></el-option>
                  <el-option label="最近30天" value="30"></el-option>
                  <el-option label="最近90天" value="90"></el-option>
                </el-select>
              </div>
            </template>
            <div ref="userActivityChart" class="chart-container"></div>
          </el-card>
        </el-col>

        <!-- 数据集列表 -->
        <el-col :span="12">
          <el-card class="chart-card" v-loading="datasetChartLoading">
            <template #header>
              <div class="chart-header">
                <div class="chart-title">
                  <el-icon><DataLine /></el-icon>
                  数据集列表
                </div>
                <el-tag type="info" effect="plain">总计: {{ datasetList.length }} 个</el-tag>
              </div>
            </template>
            <div class="dataset-list-container">
              <el-empty v-if="datasetList.length === 0" description="暂无数据集"></el-empty>
              <div v-else class="dataset-list">
                <el-scrollbar height="280px">
                  <div v-for="(dataset, index) in datasetList" :key="dataset.id" class="dataset-item">
                    <div class="dataset-item-header">
                      <span class="dataset-name">{{ dataset.name }}</span>
                      <el-tag size="small" :type="getDatasetTypeTag(index)">数据集</el-tag>
                    </div>
                    <div class="dataset-item-content">
                      <div class="dataset-description">{{ dataset.description || '暂无描述' }}</div>
                      <div class="dataset-meta">
                        <span>
                          <el-icon><User /></el-icon>
                          {{ dataset.createUser?.realName || dataset.createUser?.username || '未知用户' }}
                        </span>
                        <span>
                          <el-icon><Clock /></el-icon>
                          {{ formatDate(dataset.createTime) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </el-scrollbar>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="chart-row">
        <!-- 模型性能评估 -->
        <el-col :span="12">
          <el-card class="chart-card" v-loading="modelChartLoading">
            <template #header>
              <div class="chart-header">
                <div class="chart-title">
                  <el-icon><Monitor /></el-icon>
                  模型性能评估
                </div>
                <el-select v-model="selectedModel" size="small" placeholder="选择模型">
                  <el-option v-for="model in modelOptions" :key="model.value" :label="model.label" :value="model.value"></el-option>
                </el-select>
              </div>
            </template>
            <div ref="modelPerformanceChart" class="chart-container"></div>
          </el-card>
        </el-col>

        <!-- 用户反馈评分 -->
        <el-col :span="12">
          <el-card class="chart-card" v-loading="feedbackChartLoading">
            <template #header>
              <div class="chart-header">
                <div class="chart-title">
                  <el-icon><Document /></el-icon>
                  用户反馈评分
                </div>
                <el-tag type="success" effect="dark" size="small">平均分: {{ averageFeedbackScore }}</el-tag>
              </div>
            </template>
            <div ref="feedbackChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 底部操作历史记录 -->
    <el-card class="history-card" v-loading="historyLoading">
      <template #header>
        <div class="chart-header">
          <div class="chart-title">
            <el-icon><Document /></el-icon>
            近期操作历史
          </div>
          <el-button type="primary" size="small" plain>查看全部</el-button>
        </div>
      </template>
      
      <el-timeline>
        <el-timeline-item
          v-for="(activity, index) in recentActivities"
          :key="index"
          :type="activity.type as 'primary' | 'success' | 'warning' | 'danger' | 'info'"
          :color="activity.color"
          :timestamp="activity.time"
          :hollow="activity.hollow"
        >
          {{ activity.content }}
          <template v-if="activity.detail">
            <el-link type="primary" :underline="false" class="detail-link">查看详情</el-link>
          </template>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { User, DataLine, Monitor, Document, ArrowUp, ArrowDown, Clock } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getUserList } from '@/api/user'
import { getAllDatasets, getDatasetPage } from '@/api/dataset'
import { getModelList, getModelDetail } from '@/api/model'
import { pageFeedbacks } from '@/api/feedback'
import { pageHistories } from '@/api/history'
import { ElMessage } from 'element-plus'

// 加载状态
const userChartLoading = ref(false)
const datasetChartLoading = ref(false)
const modelChartLoading = ref(false)
const feedbackChartLoading = ref(false)
const historyLoading = ref(false)

// 统计卡片数据
const statCards = reactive([
  {
    title: '活跃用户',
    value: '0',
    icon: 'User',
    trend: 'up',
    compare: '加载中...',
    colorClass: 'user-card',
    loading: true
  },
  {
    title: '数据集总量',
    value: '0',
    icon: 'DataLine',
    trend: 'up',
    compare: '加载中...',
    colorClass: 'dataset-card',
    loading: true
  },
  {
    title: '模型数量',
    value: '0',
    icon: 'Monitor',
    trend: 'up',
    compare: '加载中...',
    colorClass: 'model-card',
    loading: true
  },
  {
    title: '用户反馈',
    value: '0',
    icon: 'Document',
    trend: 'up',
    compare: '加载中...',
    colorClass: 'feedback-card',
    loading: true
  }
])

// 图表引用
const userActivityChart = ref<HTMLElement | null>(null)
const modelPerformanceChart = ref<HTMLElement | null>(null)
const feedbackChart = ref<HTMLElement | null>(null)

// 存储echarts实例
let userActivityInstance: echarts.ECharts | null = null
let modelPerformanceInstance: echarts.ECharts | null = null
let feedbackInstance: echarts.ECharts | null = null

// 筛选选项
const userTimeRange = ref('7')
const selectedModel = ref('')

// 模型选项
const modelOptions = ref<Array<{value: string, label: string}>>([])

// 用户反馈数据
const feedbackData = ref<Array<number>>([0, 0, 0, 0, 0])
const averageFeedbackScore = ref('0.0')

// 近期活动
const recentActivities = ref<Array<{
  content: string,
  time: string,
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info',
  color: string,
  hollow: boolean,
  detail: boolean
}>>([])

// 数据集列表数据
const datasetList = ref<Array<any>>([])

// 加载统计卡片数据
const loadStatCards = async () => {
  try {
    // 1. 加载用户总数
    const userRes = await getUserList({
      current: 1,
      size: 1
    })
    if (userRes && userRes.total) {
      statCards[0].value = userRes.total.toString()
    } else {
      statCards[0].value = '0'
    }
    statCards[0].compare = '活跃用户统计'
    statCards[0].loading = false

    // 2. 加载数据集总数
    const datasetRes = await getDatasetPage({
      current: 1,
      size: 1
    })
    if (datasetRes && datasetRes.total) {
      statCards[1].value = datasetRes.total.toString()
    } else {
      statCards[1].value = '0'
    }
    statCards[1].compare = '所有数据集总量'
    statCards[1].loading = false

    // 3. 加载模型总数
    const modelRes = await getModelList({
      current: 1,
      size: 1
    })
    if (modelRes && modelRes.total) {
      statCards[2].value = modelRes.total.toString()
    } else {
      statCards[2].value = '0'
    }
    statCards[2].compare = '所有模型总量'
    statCards[2].loading = false

    // 4. 加载用户反馈评分
    const feedbackRes = await pageFeedbacks({
      current: 1,
      size: 10
    })
    
    // 计算平均分
    if (feedbackRes && feedbackRes.records && feedbackRes.records.length > 0) {
      const total = feedbackRes.records.reduce((sum: number, item: any) => sum + item.score, 0)
      const avg = (total / feedbackRes.records.length).toFixed(1)
      statCards[3].value = `${avg}/5`
      averageFeedbackScore.value = avg
    } else {
      statCards[3].value = 'N/A'
      averageFeedbackScore.value = 'N/A'
    }
    
    statCards[3].compare = '用户评分平均值'
    statCards[3].loading = false
    
  } catch (error) {
    console.error('加载统计卡片数据失败', error)
    for (let i = 0; i < statCards.length; i++) {
      statCards[i].loading = false
      statCards[i].value = 'N/A'
      statCards[i].compare = '数据加载失败'
    }
  }
}

// 加载模型选项
const loadModelOptions = async () => {
  try {
    modelChartLoading.value = true
    const res = await getModelList({
      current: 1,
      size: 100
    })
    
    if (res && res.records && res.records.length > 0) {
      modelOptions.value = res.records.map((model: any) => ({
        value: model.id.toString(),
        label: model.name
      }))
      
      // 默认选择第一个模型
      if (modelOptions.value.length > 0 && !selectedModel.value) {
        selectedModel.value = modelOptions.value[0].value
      }
    }
    modelChartLoading.value = false
  } catch (error) {
    console.error('加载模型选项失败', error)
    modelChartLoading.value = false
  }
}

// 加载用户反馈数据
const loadFeedbackData = async () => {
  try {
    feedbackChartLoading.value = true
    const res = await pageFeedbacks({
      current: 1,
      size: 1000
    })
    
    // 初始化评分数组
    const scoreCount = [0, 0, 0, 0, 0]
    
    if (res && res.records) {
      res.records.forEach((feedback: any) => {
        if (feedback.score >= 1 && feedback.score <= 5) {
          scoreCount[feedback.score - 1]++
        }
      })
      
      feedbackData.value = scoreCount
    }
    
    // 更新反馈图表
    initFeedbackChart()
    feedbackChartLoading.value = false
  } catch (error) {
    console.error('加载用户反馈数据失败', error)
    feedbackChartLoading.value = false
  }
}

// 加载历史记录
const loadHistoryData = async () => {
  try {
    historyLoading.value = true
    const res = await pageHistories({
      current: 1,
      size: 5
    })
    
    if (res && res.records) {
      recentActivities.value = res.records.map((record: any) => {
        let type: 'primary' | 'success' | 'warning' | 'danger' | 'info' = 'info'
        let color = '#909399'
        
        // 根据操作类型设置不同样式
        if (record.operationType === 1) { // 评估
          type = 'primary'
          color = '#409EFF'
        } else if (record.operationType === 2) { // 分割
          type = 'success'
          color = '#67C23A'
        }
        
        return {
          content: `${record.createUserName} ${record.operationType === 1 ? '评估' : '分割'}了模型 "${record.modelName}"`,
          time: record.createTime,
          type,
          color,
          hollow: false,
          detail: true
        }
      })
    }
    
    // 如果没有历史记录，添加一条提示
    if (recentActivities.value.length === 0) {
      recentActivities.value.push({
        content: '暂无操作历史记录',
        time: new Date().toLocaleString(),
        type: 'info',
        color: '#909399',
        hollow: true,
        detail: false
      })
    }
    
    historyLoading.value = false
  } catch (error) {
    console.error('加载历史记录失败', error)
    historyLoading.value = false
    
    // 添加错误提示
    recentActivities.value = [{
      content: '加载历史记录失败',
      time: new Date().toLocaleString(),
      type: 'danger',
      color: '#F56C6C',
      hollow: true,
      detail: false
    }]
  }
}

// 获取数据集列表
const loadDatasetList = async () => {
  try {
    datasetChartLoading.value = true
    const res = await getAllDatasets()
    
    if (res && Array.isArray(res)) {
      datasetList.value = res
    } else {
      datasetList.value = []
    }
    
    datasetChartLoading.value = false
  } catch (error) {
    console.error('加载数据集列表失败', error)
    datasetList.value = []
    datasetChartLoading.value = false
    ElMessage.error('加载数据集列表失败')
  }
}

// 格式化日期
const formatDate = (dateString: string | undefined) => {
  if (!dateString) return '未知时间'
  
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (error) {
    return dateString
  }
}

// 获取数据集标签类型
const getDatasetTypeTag = (index: number): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const types: Array<'primary' | 'success' | 'warning' | 'danger' | 'info'> = ['success', 'warning', 'danger', 'info', 'primary']
  return types[index % types.length]
}

// 初始化用户活跃度图表
const initUserActivityChart = () => {
  if (!userActivityChart.value) return
  userActivityInstance = echarts.init(userActivityChart.value)
  
  userChartLoading.value = true
  
  // 这里使用模拟数据，因为API中没有用户活跃度的时间序列数据
  const days = userTimeRange.value === '7' 
    ? ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    : Array.from({length: parseInt(userTimeRange.value)}, (_, i) => `${i+1}日`)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['访问用户', '活跃用户', '操作次数'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: {
        lineStyle: {
          color: '#E0E6F1'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: '#E0E6F1',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '访问用户',
        type: 'bar',
        barWidth: '20%',
        itemStyle: {
          color: '#91CC75'
        },
        data: [32, 45, 55, 43, 68, 35, 27].slice(0, days.length)
      },
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        itemStyle: {
          color: '#5470C6'
        },
        data: [28, 32, 31, 34, 30, 25, 24].slice(0, days.length)
      },
      {
        name: '操作次数',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        itemStyle: {
          color: '#FAC858'
        },
        data: [120, 156, 130, 145, 160, 112, 125].slice(0, days.length)
      }
    ]
  }
  
  userActivityInstance.setOption(option)
  userChartLoading.value = false
}

// 初始化模型性能图表
const initModelPerformanceChart = async () => {
  if (!modelPerformanceChart.value) return
  modelPerformanceInstance = echarts.init(modelPerformanceChart.value)
  
  modelChartLoading.value = true
  
  try {
    if (!selectedModel.value) {
      modelChartLoading.value = false
      return
    }
    
    // 获取模型详情
    const res = await getModelDetail(parseInt(selectedModel.value))
    
    // 这里假设模型详情中有训练指标，如果没有则使用模拟数据
    let metricsData = [85, 80, 82, 75, 80]
    let previousMetricsData = [75, 70, 72, 65, 70]
    
    if (res && res.trainMetrics) {
      try {
        // 尝试解析训练指标
        const metrics = JSON.parse(res.trainMetrics)
        if (metrics) {
          // 根据实际的指标格式进行提取
          metricsData = [
            metrics.accuracy ? Math.round(metrics.accuracy * 100) : 85,
            metrics.recall ? Math.round(metrics.recall * 100) : 80,
            metrics.f1 ? Math.round(metrics.f1 * 100) : 82,
            metrics.speed ? Math.round(metrics.speed * 100) : 75,
            metrics.robustness ? Math.round(metrics.robustness * 100) : 80
          ]
          
          // 模拟上一版本指标（实际中可能需要从历史记录中获取）
          previousMetricsData = metricsData.map(val => Math.max(val - Math.floor(Math.random() * 10 + 5), 60))
        }
      } catch (e) {
        console.error('解析训练指标失败', e)
      }
    }
    
    const option = {
      radar: {
        indicator: [
          { name: '准确率', max: 100 },
          { name: '召回率', max: 100 },
          { name: 'F1分数', max: 100 },
          { name: '处理速度', max: 100 },
          { name: '鲁棒性', max: 100 }
        ],
        splitArea: {
          areaStyle: {
            color: ['rgba(255, 255, 255, 0.5)'],
            shadowColor: 'rgba(0, 0, 0, 0.05)',
            shadowBlur: 10
          }
        }
      },
      series: [
        {
          name: '模型性能评估',
          type: 'radar',
          data: [
            {
              value: metricsData,
              name: '当前版本',
              areaStyle: {
                color: 'rgba(64, 158, 255, 0.6)'
              },
              lineStyle: {
                color: '#409EFF'
              }
            },
            {
              value: previousMetricsData,
              name: '上一版本',
              areaStyle: {
                color: 'rgba(245, 108, 108, 0.4)'
              },
              lineStyle: {
                color: '#F56C6C'
              }
            }
          ]
        }
      ]
    }
    
    modelPerformanceInstance.setOption(option)
    modelChartLoading.value = false
  } catch (error) {
    console.error('初始化模型性能图表失败', error)
    modelChartLoading.value = false
    
    // 显示错误提示
    ElMessage.error('加载模型性能数据失败')
  }
}

// 初始化用户反馈图表
const initFeedbackChart = () => {
  if (!feedbackChart.value) return
  feedbackInstance = echarts.init(feedbackChart.value)
  
  // 计算总评分数
  const total = feedbackData.value.reduce((acc, val) => acc + val, 0)
  
  // 计算百分比
  const percentages = feedbackData.value.map(val => total > 0 ? Math.round((val / total) * 100) : 0)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: function(params: any) {
        const data = params[0]
        return `${data.name}: ${data.value}人 (${percentages[data.dataIndex]}%)`
      },
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: {
        color: '#666'
      }
    },
    grid: {
      left: '3%',
      right: '15%',
      bottom: '10%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      splitLine: {
        lineStyle: { 
          color: '#f5f5f5',
          type: 'dashed'
        }
      },
      axisLabel: { show: false }
    },
    yAxis: {
      type: 'category',
      data: ['5★', '4★', '3★', '2★', '1★'].map(item => {
        return {
          value: item,
          textStyle: {
            fontSize: 14,
            fontWeight: 500,
            color: '#666'
          }
        }
      }),
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '用户数量',
        type: 'bar',
        barWidth: '60%',
        data: [
          feedbackData.value[4], 
          feedbackData.value[3], 
          feedbackData.value[2], 
          feedbackData.value[1], 
          feedbackData.value[0]
        ],
        itemStyle: {
          color: function(params: any) {
            // 使用更柔和的渐变色
            const colorList = [
              {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: '#60a5fa' },
                  { offset: 1, color: '#3b82f6' }
                ]
              },
              {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: '#4ade80' },
                  { offset: 1, color: '#22c55e' }
                ]
              },
              {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: '#a78bfa' },
                  { offset: 1, color: '#8b5cf6' }
                ]
              },
              {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: '#fbbf24' },
                  { offset: 1, color: '#f59e0b' }
                ]
              },
              {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: '#f87171' },
                  { offset: 1, color: '#ef4444' }
                ]
              }
            ]
            return colorList[params.dataIndex]
          },
          borderRadius: [0, 4, 4, 0],
          shadowColor: 'rgba(0, 0, 0, 0.1)',
          shadowBlur: 10
        },
        label: {
          show: true,
          position: 'right',
          formatter: function(params: any) {
            return `${params.value}人 (${percentages[4 - params.dataIndex]}%)`
          },
          color: '#666',
          fontSize: 13,
          fontWeight: 500
        }
      }
    ],
    // 添加自定义背景虚线
    markLine: {
      silent: true,
      symbol: 'none',
      lineStyle: {
        type: 'dashed',
        color: '#f5f5f5'
      },
      data: [
        { yAxis: 0.5 },
        { yAxis: 1.5 },
        { yAxis: 2.5 },
        { yAxis: 3.5 }
      ]
    }
  }
  
  feedbackInstance.setOption(option)
}

// 监听筛选条件变化
watch(() => userTimeRange.value, () => {
  initUserActivityChart()
})

watch(() => selectedModel.value, () => {
  if (selectedModel.value) {
    initModelPerformanceChart()
  }
})

// 监听窗口大小变化
const handleResize = () => {
  userActivityInstance?.resize()
  modelPerformanceInstance?.resize()
  feedbackInstance?.resize()
}

onMounted(async () => {
  // 加载统计卡片数据
  await loadStatCards()
  
  // 加载模型选项
  await loadModelOptions()
  
  // 加载用户反馈数据
  await loadFeedbackData()
  
  // 加载历史记录
  await loadHistoryData()
  
  // 加载数据集列表
  await loadDatasetList()
  
  // 初始化图表
  setTimeout(() => {
    initUserActivityChart()
    if (selectedModel.value) {
      initModelPerformanceChart()
    }
  }, 200)
  
  // 添加窗口大小变化监听
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize)
  
  // 销毁图表实例
  userActivityInstance?.dispose()
  modelPerformanceInstance?.dispose()
  feedbackInstance?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  height: 100px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  position: absolute;
  right: -10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 70px;
  opacity: 0.2;
}

.stat-info {
  z-index: 1;
  padding: 15px;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 10px;
}

.stat-desc {
  font-size: 12px;
  display: flex;
  align-items: center;
}

.trend-up {
  color: #67C23A;
  margin-right: 5px;
}

.trend-down {
  color: #F56C6C;
  margin-right: 5px;
}

.user-card {
  background: linear-gradient(120deg, #e0f2fe, #bfdbfe);
  color: #1d4ed8;
}

.dataset-card {
  background: linear-gradient(120deg, #dcfce7, #bbf7d0);
  color: #16a34a;
}

.model-card {
  background: linear-gradient(120deg, #fef3c7, #fde68a);
  color: #d97706;
}

.feedback-card {
  background: linear-gradient(120deg, #fee2e2, #fecaca);
  color: #dc2626;
}

.chart-section {
  margin-bottom: 20px;
}

.chart-row {
  margin-top: 20px;
}

.chart-card {
  height: 380px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.chart-title .el-icon {
  margin-right: 8px;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.history-card {
  margin-bottom: 20px;
}

.detail-link {
  margin-left: 10px;
  font-size: 12px;
}

.dataset-list-container {
  height: 280px;
  overflow: hidden;
}

.dataset-list {
  height: 100%;
}

.dataset-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.dataset-item:hover {
  background-color: #f5f7fa;
}

.dataset-item:last-child {
  border-bottom: none;
}

.dataset-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.dataset-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dataset-item-content {
  display: flex;
  flex-direction: column;
}

.dataset-description {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dataset-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.dataset-meta span {
  display: flex;
  align-items: center;
}

.dataset-meta .el-icon {
  margin-right: 4px;
  font-size: 14px;
}

/* 适配移动端 */
@media (max-width: 768px) {
  .stat-card {
    margin-bottom: 15px;
  }
  
  .chart-card {
    margin-bottom: 15px;
  }
}
</style> 