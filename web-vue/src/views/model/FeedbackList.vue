<template>
  <div class="feedback-container">
    <el-card class="feedback-card">
      <template #header>
        <div class="card-header">
          <h2>评估反馈</h2>
          <p>模型评估反馈列表</p>
        </div>
      </template>

      <!-- 过滤表单 -->
      <el-form :model="queryParams" inline class="filter-form">
        <el-form-item label="模型">
          <el-select v-model="queryParams.modelId" placeholder="请选择模型" clearable style="width: 200px;">
            <el-option
              v-for="item in modelList"
              :key="item.id"
              :label="`${item.name} (创建人: ${item.createUsername})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 反馈列表 -->
      <el-table
        v-loading="loading"
        :data="feedbackList"
        style="width: 100%"
        border
      >
        <el-table-column prop="modelName" label="模型名称" max-width="100" />
        <el-table-column label="评分" min-width="60">
          <template #default="{ row }">
            <el-rate
              v-model="row.score"
              disabled
              text-color="#ff9900"
              score-template="{value}"
            />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="100" :show-overflow-tooltip="true" />
        <el-table-column prop="createUserName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">
              查看详情
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="反馈详情"
      width="800px"
      destroy-on-close
    >
      <el-descriptions
        v-if="currentFeedback"
        :column="2"
        border
      >
        <el-descriptions-item label="模型名称">{{ currentFeedback.modelName }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentFeedback.createUserName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentFeedback.createTime }}</el-descriptions-item>
        <el-descriptions-item label="评分">
          <el-rate
            v-model="currentFeedback!.score"
            disabled
            text-color="#ff9900"
            score-template="{value}"
          />
        </el-descriptions-item>
        <el-descriptions-item label="反馈内容" :span="2">
          {{ currentFeedback!.content || '无反馈内容' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 评估指标 -->
      <template v-if="currentFeedback && currentFeedback.metrics">
        <h3 class="metrics-title">评估指标</h3>
        <el-card shadow="never" class="metrics-card">
          <div class="metrics-grid">
            <template v-for="(value, key) in parsedMetrics" :key="key">
              <div class="metric-item" v-if="typeof value === 'number'">
                <div class="metric-label">{{ formatMetricName(String(key)) }}</div>
                <div class="metric-value">{{ (value * 100).toFixed(2) }}%</div>
              </div>
            </template>
          </div>
        </el-card>
      </template>

      <!-- 图片展示 -->
      <template v-if="hasImages(currentFeedback)">
        <h3 class="images-title">相关图片</h3>
        <div class="feedback-images">
          <div v-if="currentFeedback?.originalImageUrl" class="image-item">
            <h4>原始图片</h4>
            <el-image
              :src="currentFeedback.originalImageUrl"
              fit="contain"
              :preview-src-list="[currentFeedback.originalImageUrl]"
              :initial-index="0"
              preview-teleported
            />
          </div>
          <div v-if="currentFeedback?.labelImageUrl" class="image-item">
            <h4>标签图片</h4>
            <el-image
              :src="currentFeedback.labelImageUrl"
              fit="contain"
              :preview-src-list="[currentFeedback.labelImageUrl]"
              :initial-index="0"
              preview-teleported
            />
          </div>
          <div v-if="currentFeedback?.overlayImageUrl" class="image-item">
            <h4>叠加结果图</h4>
            <el-image
              :src="currentFeedback.overlayImageUrl"
              fit="contain"
              :preview-src-list="[currentFeedback.overlayImageUrl]"
              :initial-index="0"
              preview-teleported
            />
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Picture } from '@element-plus/icons-vue'
import { pageFeedbacks, deleteFeedback, type FeedbackVO, type FeedbackQueryParams } from '@/api/feedback'
import { getModelList } from '@/api/model'

// 查询参数
const queryParams = reactive<FeedbackQueryParams>({
  current: 1,
  size: 10,
  modelId: undefined
})

// 反馈列表数据
const feedbackList = ref<FeedbackVO[]>([])
const total = ref(0)
const loading = ref(false)
const modelList = ref<any[]>([])

// 详情弹窗
const detailDialogVisible = ref(false)
const currentFeedback = ref<FeedbackVO | null>(null)

// 获取反馈列表
const fetchFeedbackList = async () => {
  loading.value = true
  try {
    const response = await pageFeedbacks(queryParams)
    feedbackList.value = response.records
    total.value = response.total
  } catch (error) {
    console.error('获取反馈列表失败', error)
    ElMessage.error('获取反馈列表失败')
  } finally {
    loading.value = false
  }
}

// 获取模型列表
const fetchModelList = async () => {
  try {
    const response = await getModelList({
      current: 1,
      size: 100,
      status: 2 // 只获取训练成功的模型
    })
    modelList.value = response.records || []
  } catch (error) {
    console.error('获取模型列表失败', error)
    ElMessage.error('获取模型列表失败')
  }
}

// 查询
const handleQuery = () => {
  queryParams.current = 1
  fetchFeedbackList()
}

// 重置
const handleReset = () => {
  queryParams.modelId = undefined
  handleQuery()
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  queryParams.size = size
  fetchFeedbackList()
}

// 页码变化
const handleCurrentChange = (current: number) => {
  queryParams.current = current
  fetchFeedbackList()
}

// 查看详情
const handleViewDetail = (row: FeedbackVO) => {
  currentFeedback.value = row
  detailDialogVisible.value = true
}

// 删除反馈
const handleDelete = (row: FeedbackVO) => {
  ElMessageBox.confirm('确定要删除该反馈吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteFeedback(row.id)
      ElMessage.success('删除成功')
      fetchFeedbackList()
    } catch (error) {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 判断是否有图片
const hasImages = (row: FeedbackVO | null) => {
  if (!row) return false
  return !!(row.originalImageUrl || row.labelImageUrl || row.overlayImageUrl)
}

// 解析指标JSON
const parsedMetrics = computed(() => {
  if (!currentFeedback.value?.metrics) return {}
  try {
    return JSON.parse(currentFeedback.value.metrics)
  } catch (e) {
    console.error('解析指标数据失败', e)
    return {}
  }
})

// 格式化指标名称
const formatMetricName = (key: string) => {
  const nameMap: Record<string, string> = {
    dice: 'Dice系数',
    iou: 'IoU系数',
    precision: '精确率',
    recall: '召回率',
    f1: 'F1分数',
    accuracy: '准确率'
  }
  return nameMap[key] || key
}

// 组件挂载时获取数据
onMounted(() => {
  fetchModelList()
  fetchFeedbackList()
})
</script>

<style scoped>
.feedback-container {
  padding: 20px;
}

.feedback-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h2 {
  margin-bottom: 10px;
  color: #409EFF;
  font-size: 24px;
}

.card-header p {
  color: #606266;
  font-size: 14px;
}

.filter-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 详情弹窗样式 */
.metrics-title,
.images-title {
  margin-top: 20px;
  margin-bottom: 10px;
  font-size: 16px;
  color: #303133;
  font-weight: bold;
}

.metrics-card {
  margin-bottom: 20px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-gap: 15px;
}

.metric-item {
  text-align: center;
  background-color: #f8f9fa;
  border-radius: 6px;
  padding: 10px;
}

.metric-label {
  color: #606266;
  font-size: 14px;
  margin-bottom: 6px;
}

.metric-value {
  font-size: 16px;
  font-weight: bold;
  color: #409EFF;
}

.feedback-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-gap: 15px;
  margin-top: 15px;
}

.image-item {
  display: flex;
  flex-direction: column;
}

.image-item h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

:deep(.el-image) {
  width: 100%;
  height: 200px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style> 