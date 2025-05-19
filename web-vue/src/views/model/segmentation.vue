<template>
  <div class="model-segmentation-container">
    <el-card class="segmentation-card">
      <template #header>
        <div class="card-header">
          <h2>影像分割</h2>
          <p>上传待分割图片，选择模型进行分割</p>
        </div>
      </template>
      
      <!-- 选择模型与开始分割放到同一行 -->
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
          @click="handleSegment" 
          :disabled="!isFormValid || loading"
          :loading="loading"
          class="segment-button">
          开始分割
        </el-button>
      </div>
      
      <!-- 三块布局的分割区域 -->
      <div class="segmentation-layout">
        <!-- 左：待分割图片 -->
        <div class="section original-image">
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
        
        <!-- 中：分割掩码图 -->
        <div class="section mask-image">
          <h3>分割掩码图</h3>
          <el-skeleton v-if="loading" animated>
            <template #template>
              <div class="image-skeleton"></div>
            </template>
          </el-skeleton>
          <div v-else class="image-wrapper">
            <el-image 
              v-if="segmentationResult && segmentationResult.predictionMask" 
              :src="segmentationResult.predictionMask.url" 
              class="display-image" 
              :preview-src-list="[segmentationResult.predictionMask.url]"
              :initial-index="0"
              fit="contain"
              :preview-teleported="true"
            />
            <el-empty v-else description="尚未分割"></el-empty>
          </div>
        </div>
        
        <!-- 右：叠加结果图 -->
        <div class="section overlay-image">
          <h3>叠加结果图</h3>
          <el-skeleton v-if="loading" animated>
            <template #template>
              <div class="image-skeleton"></div>
            </template>
          </el-skeleton>
          <div v-else class="image-wrapper">
            <el-image 
              v-if="segmentationResult && segmentationResult.overlayImage" 
              :src="segmentationResult.overlayImage.url" 
              class="display-image" 
              :preview-src-list="[segmentationResult.overlayImage.url]"
              :initial-index="0"
              fit="contain"
              :preview-teleported="true"
            />
            <el-empty v-else description="尚未分割"></el-empty>
          </div>
        </div>
      </div>
      
      <!-- 操作按钮区域 -->
      <div v-if="segmentationResult" class="action-buttons">
        <el-button type="primary" @click="saveHistory">
          <el-icon><Star /></el-icon>
          保存记录
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import { fileRequest } from '@/api/file_request'
import { getModelList, predictImage } from '@/api/model'
import { createHistory } from '@/api/history'

// 定义表单数据
const form = reactive({
  modelId: '',
  imageUrl: '',
  imageBucket: '',
  imageKey: ''
})

// 模型列表
const modelList = ref<any[]>([])

// 加载状态
const loading = ref(false)
const imageLoading = ref(false)

// 分割结果
const segmentationResult = ref<any>(null)

// 表单是否有效
const isFormValid = computed(() => {
  return form.modelId && form.imageBucket && form.imageKey
})

// 获取模型列表
const fetchModelList = async () => {
  try {
    // 获取已发布的模型
    const res = await getModelList({ 
      current: 1, 
      size: 100,
      status: 4 // 只获取已发布的模型
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

// 上传待分割图片
const uploadImage = async (params: any) => {
  try {
    imageLoading.value = true
    const file = params.file
    const res = await fileRequest.upload('images', file)
    form.imageUrl = res.url
    form.imageBucket = res.bucket
    form.imageKey = res.objectKey
    ElMessage.success('图片上传成功')
    
    // 创建一个新的Image对象来预加载图片
    const img = new Image()
    img.onload = () => {
      imageLoading.value = false
    }
    img.onerror = () => {
      imageLoading.value = false
      ElMessage.warning('图片预览加载失败')
    }
    img.src = form.imageUrl
  } catch (error) {
    imageLoading.value = false
    console.error('上传图片失败', error)
    ElMessage.error('上传图片失败')
  }
}

// 开始分割
const handleSegment = async () => {
  if (!isFormValid.value) {
    ElMessage.warning('请完善表单信息')
    return
  }
  
  try {
    loading.value = true
    segmentationResult.value = null // 清空之前的结果
    
    // 调用分割接口
    const params = {
      modelId: form.modelId,
      imageBucket: form.imageBucket,
      imageKey: form.imageKey
    }
    
    const result = await predictImage(params)
    segmentationResult.value = result

    // 更新待分割图片为处理后的图片
    if (result.processedOrigImg) {
      form.imageUrl = result.processedOrigImg.url
      form.imageBucket = result.processedOrigImg.bucket
      form.imageKey = result.processedOrigImg.objectKey
    }
    
    ElMessage.success('影像分割完成')
  } catch (error) {
    console.error('影像分割失败', error)
    ElMessage.error('影像分割失败')
  } finally {
    loading.value = false
  }
}

// 重置页面内容
const resetPageContent = () => {
  // 重置表单数据
  form.modelId = ''
  form.imageUrl = ''
  form.imageBucket = ''
  form.imageKey = ''
  
  // 清空分割结果
  segmentationResult.value = null
}

// 保存操作历史记录
const saveHistory = async () => {
  if (!segmentationResult.value) {
    ElMessage.warning('请先进行分割')
    return
  }
  
  try {
    loading.value = true
    const historyData = {
      modelId: Number(form.modelId),
      operationType: 2, // 2: 分割操作
      originalImageBucket: form.imageBucket,
      originalImageKey: form.imageKey,
      resultImageBucket: segmentationResult.value.predictionMask?.bucket,
      resultImageKey: segmentationResult.value.predictionMask?.objectKey,
      overlayImageBucket: segmentationResult.value.overlayImage?.bucket,
      overlayImageKey: segmentationResult.value.overlayImage?.objectKey,
    }
    
    await createHistory(historyData)
    ElMessage.success('操作记录保存成功')
    
    // 保存成功后重置页面
    resetPageContent()
  } catch (error) {
    console.error('保存操作记录失败', error)
    ElMessage.error('保存操作记录失败')
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取模型列表
onMounted(() => {
  fetchModelList()
})
</script>

<style scoped>
.model-segmentation-container {
  padding: 20px;
}

.segmentation-card {
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

/* 模型选择与分割按钮的行布局 */
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

.segment-button {
  min-width: 120px;
}

/* 三块布局样式 */
.segmentation-layout {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: 360px;
  gap: 20px;
  margin-top: 30px;
}

.section {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  overflow: hidden;
}

.section:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.1);
}

.section h3 {
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
  min-height: 280px;
  border-radius: 4px;
  background-color: #f8f9fa;
  cursor: pointer;
}

.display-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
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

/* 响应式调整 */
@media (max-width: 1200px) {
  .segmentation-layout {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 300px 300px;
  }
  
  .section.original-image {
    grid-column: 1 / 2;
    grid-row: 1 / 2;
  }
  
  .section.mask-image {
    grid-column: 2 / 3;
    grid-row: 1 / 2;
  }
  
  .section.overlay-image {
    grid-column: 1 / 3;
    grid-row: 2 / 3;
  }
}

@media (max-width: 768px) {
  .segmentation-layout {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(3, 300px);
  }
  
  .section.original-image {
    grid-column: 1;
    grid-row: 1;
  }
  
  .section.mask-image {
    grid-column: 1;
    grid-row: 2;
  }
  
  .section.overlay-image {
    grid-column: 1;
    grid-row: 3;
  }
  
  .model-selection-row {
    flex-direction: column;
    gap: 10px;
  }
  
  .model-select {
    width: 100%;
    margin-right: 0;
  }
  
  .segment-button {
    width: 100%;
  }
}

/* 操作按钮区域 */
.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}
</style> 