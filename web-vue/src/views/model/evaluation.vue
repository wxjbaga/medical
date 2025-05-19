<template>
  <div class="model-evaluation-container">
    <el-card class="evaluation-card">
      <template #header>
        <div class="card-header">
          <h2>模型评估</h2>
          <p>上传待分割图片和标签掩码图，选择模型进行评估</p>
        </div>
      </template>
      
      <!-- 选择模型与开始评估放到同一行 -->
      <div class="model-selection-row">
        <el-select 
          v-model="form.modelId" 
          placeholder="请选择模型" 
          class="model-select">
          <el-option 
            v-for="item in modelList" 
            :key="item.id" 
            :label="`${item.name} (创建人: ${item.createUsername})`" 
            :value="item.id" 
          />
        </el-select>
        <el-button 
          type="primary" 
          @click="handleEvaluate" 
          :disabled="!isFormValid || loading"
          :loading="loading"
          class="evaluate-button">
          开始评估
        </el-button>
      </div>
      
      <!-- 四块布局的评估区域 -->
      <div class="evaluation-layout">
        <!-- 左上：待分割图片 -->
        <div class="quadrant original-image">
          <h3>待分割图片</h3>
          <div class="upload-controls">
            <el-upload
              class="upload-demo"
              action="#"
              :http-request="uploadImage"
              :show-file-list="false"
              :before-upload="beforeUploadImage"
            >
              <el-button type="primary">选择图片</el-button>
            </el-upload>
            <div class="el-upload__tip">
              请上传JPG/PNG格式图片
            </div>
          </div>
          <el-skeleton v-if="imageLoading" animated>
            <template #template>
              <div class="image-skeleton"></div>
            </template>
          </el-skeleton>
          <div v-else class="image-wrapper">
            <el-image 
              v-if="form.imageUrl" 
              :src="form.imageUrl" 
              class="display-image" 
              :preview-src-list="[form.imageUrl]"
              :initial-index="0"
              fit="contain"
              :preview-teleported="true"
            />
            <el-empty v-else description="尚未上传图片"></el-empty>
          </div>
        </div>
        
        <!-- 右上：标签掩码图 -->
        <div class="quadrant mask-image">
          <h3>标签掩码图</h3>
          <div class="upload-controls">
            <el-upload
              class="upload-demo"
              action="#"
              :http-request="uploadMask"
              :show-file-list="false"
              :before-upload="beforeUploadImage"
            >
              <el-button type="primary">选择掩码图</el-button>
            </el-upload>
            <div class="el-upload__tip">
              请上传JPG/PNG格式掩码图
            </div>
          </div>
          <el-skeleton v-if="maskLoading" animated>
            <template #template>
              <div class="image-skeleton"></div>
            </template>
          </el-skeleton>
          <div v-else class="image-wrapper">
            <el-image 
              v-if="form.maskUrl" 
              :src="form.maskUrl" 
              class="display-image" 
              :preview-src-list="[form.maskUrl]"
              :initial-index="0"
              fit="contain"
              :preview-teleported="true"
            />
            <el-empty v-else description="尚未上传掩码图"></el-empty>
          </div>
        </div>
        
        <!-- 左下：评估结果叠加图 -->
        <div class="quadrant overlay-image">
          <h3>叠加结果图</h3>
          <el-skeleton v-if="loading" animated>
            <template #template>
              <div class="image-skeleton"></div>
            </template>
          </el-skeleton>
          <div v-else class="image-wrapper">
            <template v-if="evaluationResult && evaluationResult.overlayImage">
              <el-image 
                :src="evaluationResult.overlayImage.url" 
                class="display-image" 
                :preview-src-list="[evaluationResult.overlayImage.url]"
                :initial-index="0"
                fit="contain"
                :preview-teleported="true"
              />
              <div class="legend">
                <div class="legend-item">
                  <div class="color-box green"></div>
                  <span>真实标签</span>
                </div>
                <div class="legend-item">
                  <div class="color-box blue"></div>
                  <span>预测结果</span>
                </div>
              </div>
            </template>
            <el-empty v-else description="尚未评估"></el-empty>
          </div>
        </div>
        
        <!-- 右下：评估指标结果 -->
        <div class="quadrant metrics-results">
          <h3>评估指标</h3>
          <el-skeleton v-if="loading" animated :rows="6">
            <template #template>
              <div class="image-skeleton"></div>
            </template>
          </el-skeleton>
          <div v-else class="result-content">
            <div v-if="evaluationResult && evaluationResult.metrics" class="metrics-container">
              <el-card shadow="hover" class="metrics-card">
                <div class="metrics-grid">
                  <div class="metric-item">
                    <div class="metric-label">Dice系数</div>
                    <div class="metric-value success">{{ (evaluationResult.metrics.dice * 100).toFixed(2) }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">IoU系数</div>
                    <div class="metric-value success">{{ (evaluationResult.metrics.iou * 100).toFixed(2) }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">精确率</div>
                    <div class="metric-value primary">{{ (evaluationResult.metrics.precision * 100).toFixed(2) }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">召回率</div>
                    <div class="metric-value primary">{{ (evaluationResult.metrics.recall * 100).toFixed(2) }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">F1分数</div>
                    <div class="metric-value warning">{{ (evaluationResult.metrics.f1 * 100).toFixed(2) }}%</div>
                  </div>
                  <div class="metric-item">
                    <div class="metric-label">准确率</div>
                    <div class="metric-value warning">{{ (evaluationResult.metrics.accuracy * 100).toFixed(2) }}%</div>
                  </div>
                </div>
              </el-card>
            </div>
            <el-empty v-else description="尚未评估"></el-empty>
          </div>
        </div>
      </div>

      <!-- 评估结果反馈表单 -->
      <el-card v-if="evaluationResult" class="feedback-card">
        <template #header>
          <div class="card-header">
            <h3>评估反馈</h3>
            <p>请对本次评估结果进行反馈，帮助我们改进模型</p>
          </div>
        </template>
        <el-form :model="feedbackForm" label-width="120px" ref="feedbackFormRef">
          <el-form-item label="评分" prop="score" :rules="[{ required: true, message: '请选择评分', trigger: 'change' }]">
            <el-rate v-model="feedbackForm.score" :max="5" :texts="['很差', '较差', '一般', '较好', '很好']" show-text />
          </el-form-item>
          <el-form-item label="反馈内容" prop="content">
            <el-input v-model="feedbackForm.content" type="textarea" :rows="3" placeholder="请输入您对本次评估结果的反馈意见" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitFeedback" :loading="feedbackLoading">提交反馈</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import { fileRequest } from '@/api/file_request'
import { getModelList } from '@/api/model'
import { evaluateModel } from '@/api/model'
import { createFeedback } from '@/api/feedback'

// 定义评估结果的类型
interface ImageInfo {
  bucket: string
  objectKey: string
  url: string
}

interface EvaluationResult {
  metrics: {
    accuracy: number
    classMetrics: Record<string, any>
    dice: number
    f1: number
    iou: number
    precision: number
    recall: number
  }
  overlayImage: ImageInfo
  predictionMask: ImageInfo
  processedGtMask: ImageInfo
  processedOrigImg: ImageInfo
}

// 定义表单数据
const form = reactive({
  modelId: '',
  imageUrl: '',
  imageBucket: '',
  imageKey: '',
  maskUrl: '',
  maskBucket: '',
  maskKey: ''
})

// 模型列表
const modelList = ref<any[]>([])

// 加载状态
const loading = ref(false)
const imageLoading = ref(false)
const maskLoading = ref(false)

// 评估结果
const evaluationResult = ref<EvaluationResult | null>(null)

// 反馈表单
const feedbackForm = reactive({
  score: 3,
  content: ''
})
const feedbackFormRef = ref(null)
const feedbackLoading = ref(false)

// 表单是否有效
const isFormValid = computed(() => {
  return form.modelId && form.imageBucket && form.imageKey && form.maskBucket && form.maskKey
})

// 获取模型列表
const fetchModelList = async () => {
  try {
    // 获取训练成功的模型
    const res = await getModelList({ 
      current: 1, 
      size: 100,
      status: 2 // 只获取训练成功的模型
    })
    modelList.value = res.records || []
  } catch (error) {
    console.error('获取模型列表失败', error)
    ElMessage.error('获取模型列表失败')
  }
}

// 上传图片前的验证
const beforeUploadImage = (file: File) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传JPG/PNG格式的图片！')
    return false
  }
  
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB！')
    return false
  }
  
  return true
}

// 开始评估
const handleEvaluate = async () => {
  if (!isFormValid.value) {
    ElMessage.warning('请完善表单信息')
    return
  }
  
  try {
    loading.value = true
    evaluationResult.value = null // 清空之前的结果
    
    // 调用评估接口
    const params = {
      modelId: form.modelId,
      imageBucket: form.imageBucket,
      imageKey: form.imageKey,
      maskBucket: form.maskBucket,
      maskKey: form.maskKey
    }
    
    const result = await evaluateModel(params)
    
    // 更新为处理后的原始图片
    if (result.processedOrigImg) {
      form.imageUrl = result.processedOrigImg.url
      form.imageBucket = result.processedOrigImg.bucket
      form.imageKey = result.processedOrigImg.objectKey
      await preloadImage(form.imageUrl)
    }
    
    // 更新为处理后的掩码图
    if (result.processedGtMask) {
      form.maskUrl = result.processedGtMask.url
      form.maskBucket = result.processedGtMask.bucket
      form.maskKey = result.processedGtMask.objectKey
      await preloadImage(form.maskUrl)
    }
    
    // 预加载预测掩码图和叠加结果图
    if (result.predictionMask) {
      await preloadImage(result.predictionMask.url)
    }
    
    if (result.overlayImage) {
      await preloadImage(result.overlayImage.url)
    }
    
    evaluationResult.value = result
    ElMessage.success('模型评估完成')
  } catch (error) {
    console.error('模型评估失败', error)
    ElMessage.error('模型评估失败')
  } finally {
    loading.value = false
  }
}

// 预加载图片的工具函数
const preloadImage = (url: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve()
    img.onerror = () => {
      console.warn('图片加载失败:', url)
      resolve() // 即使加载失败也继续执行
    }
    img.src = url
  })
}

// 修改上传图片的函数
const uploadImage = async (params: any) => {
  try {
    imageLoading.value = true
    const file = params.file
    const res = await fileRequest.upload('images', file)
    
    // 预加载图片
    await preloadImage(res.url)
    
    form.imageUrl = res.url
    form.imageBucket = res.bucket
    form.imageKey = res.objectKey
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('上传图片失败', error)
    ElMessage.error('上传图片失败')
  } finally {
    imageLoading.value = false
  }
}

// 修改上传掩码图的函数
const uploadMask = async (params: any) => {
  try {
    maskLoading.value = true
    const file = params.file
    const res = await fileRequest.upload('masks', file)
    
    // 预加载图片
    await preloadImage(res.url)
    
    form.maskUrl = res.url
    form.maskBucket = res.bucket
    form.maskKey = res.objectKey
    ElMessage.success('掩码图上传成功')
  } catch (error) {
    console.error('上传掩码图失败', error)
    ElMessage.error('上传掩码图失败')
  } finally {
    maskLoading.value = false
  }
}

// 清空页面内容
const resetPageContent = () => {
  // 清空表单数据
  form.modelId = ''
  form.imageUrl = ''
  form.imageBucket = ''
  form.imageKey = ''
  form.maskUrl = ''
  form.maskBucket = ''
  form.maskKey = ''
  
  // 清空评估结果
  evaluationResult.value = null
  
  // 清空反馈表单
  feedbackForm.score = 3
  feedbackForm.content = ''
}

// 提交反馈
const submitFeedback = async () => {
  if (!evaluationResult.value) {
    ElMessage.warning('请先进行评估')
    return
  }

  try {
    feedbackLoading.value = true

    // 提交反馈
    const feedbackData = {
      modelId: Number(form.modelId),
      score: feedbackForm.score,
      content: feedbackForm.content,
      metrics: JSON.stringify(evaluationResult.value.metrics),
      originalImageBucket: form.imageBucket,
      originalImageKey: form.imageKey,
      labelImageBucket: form.maskBucket,
      labelImageKey: form.maskKey,
      overlayImageBucket: evaluationResult.value.overlayImage?.bucket,
      overlayImageKey: evaluationResult.value.overlayImage?.objectKey
    }

    await createFeedback(feedbackData)
    ElMessage.success('反馈提交成功')
    
    // 清空页面内容
    resetPageContent()
  } catch (error) {
    console.error('提交反馈失败', error)
    ElMessage.error('提交反馈失败')
  } finally {
    feedbackLoading.value = false
  }
}

// 组件挂载时获取模型列表
onMounted(() => {
  fetchModelList()
})
</script>

<style scoped>
.model-evaluation-container {
  padding: 20px;
}

.evaluation-card {
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

/* 模型选择与评估按钮的行布局 */
.model-selection-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 10px 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.model-select {
  flex: 1;
  margin-right: 20px;
}

.evaluate-button {
  min-width: 120px;
}

/* 四块布局样式 */
.evaluation-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 360px 270px;
  gap: 20px;
  margin-top: 30px;
}

.quadrant {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  overflow: hidden;
}

.quadrant:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
}

.quadrant h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #303133;
  font-size: 16px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
  font-weight: 600;
}

.upload-controls {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.el-upload__tip {
  margin-left: 5px;
  color: #909399;
  font-size: 12px;
}

.image-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  min-height: 180px;
  border-radius: 4px;
  background-color: #f8f9fa;
  cursor: pointer;
  padding: 10px;
}

.display-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  width: 100%;
  height: 100%;
  transition: transform 0.3s;
  border-radius: 4px;
}

/* 骨架屏样式 */
.image-skeleton {
  width: 100%;
  height: 200px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

/* 指标区域样式 */
.metrics-container {
  height: 100%;
  width: 100%;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metrics-card {
  width: 100%;
  height: auto;
  border: none;
  margin: 0;
}

.result-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  overflow: hidden;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-gap: 8px;
  padding: 6px;
}

.metric-item {
  text-align: center;
  background-color: #f8f9fa;
  border-radius: 6px;
  padding: 8px 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.metric-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
}

.metric-label {
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}

.metric-value {
  font-size: 18px;
  font-weight: bold;
}

.metric-value.success {
  color: #67c23a;
}

.metric-value.primary {
  color: #409eff;
}

.metric-value.warning {
  color: #e6a23c;
}

/* 图例样式 */
.legend {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background-color: rgba(255, 255, 255, 0.9);
  padding: 8px;
  border-radius: 6px;
  font-size: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.legend-item {
  display: flex;
  align-items: center;
  margin: 5px 0;
}

.color-box {
  width: 12px;
  height: 12px;
  margin-right: 8px;
  border-radius: 2px;
}

.green {
  background-color: #00ff00;
}

.red {
  background-color: #ff0000;
}

.blue {
  background-color: #0000ff;
}

.yellow {
  background-color: #ffff00;
}

/* 操作按钮区域 */
.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

/* 反馈表单样式 */
.feedback-card {
  margin-top: 30px;
  padding: 0;
}

.feedback-card .el-card__header {
  padding: 15px 20px;
  background-color: #f8f9fa;
}

.feedback-card .el-card__body {
  padding: 20px;
}

.feedback-card h3 {
  margin: 0;
  font-size: 18px;
  color: #409EFF;
}

.feedback-card p {
  margin: 5px 0 0;
  font-size: 14px;
  color: #606266;
}
</style> 