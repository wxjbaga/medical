<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { enhanceImage, analyzeImage, getHealthAdvice } from '@/api/heart_image'
import { fileRequest } from '@/api/file_request'
import type { UploadFile, UploadFiles } from 'element-plus'
import { 
  Picture, 
  Upload, 
  PictureFilled, 
  PictureRounded, 
  Brush, 
  Operation, 
  DataAnalysis, 
  InfoFilled,
  Guide,
  Loading
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'

// 创建markdown解析器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
})

// 图像相关状态
const originImage = ref<string>('')
const enhancedImage = ref<string>('')
const analysisResult = ref<string>('')
const healthAdvice = ref<string>('')
const formattedAdvice = ref<string>('')

// 当健康建议更新时，进行Markdown格式化
function updateFormattedAdvice() {
  if (healthAdvice.value) {
    formattedAdvice.value = md.render(healthAdvice.value)
  } else {
    formattedAdvice.value = ''
  }
}

// 监听healthAdvice变化
watch(() => healthAdvice.value, updateFormattedAdvice, { immediate: true })

// 存储图像信息
const imageInfo = reactive({
  bucket: '',
  objectKey: '',
  file: null as File | null
})

// 加载状态
const isEnhancing = ref(false)
const isAnalyzing = ref(false)
const isGeneratingAdvice = ref(false)
const showOriginSkeleton = ref(true)  // 默认显示骨架屏
const showEnhancedSkeleton = ref(true)  // 默认显示骨架屏
const showAnalysisSkeleton = ref(false)
const showAdviceSkeleton = ref(false)

// 显示状态
const showEnhancedCard = ref(true)  // 始终显示增强图像卡片

// 初始化
onMounted(() => {
  // 在2秒后隐藏初始骨架屏，显示空白状态
  setTimeout(() => {
    showOriginSkeleton.value = false
    showEnhancedSkeleton.value = false
  }, 2000)
})

// 选择图像
const handleImageSelect = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    const file = uploadFile.raw
    // 检查文件类型
    if (!file.type.startsWith('image/')) {
      ElMessage.error('请选择图像文件')
      return
    }
    
    // 存储文件
    imageInfo.file = file
    
    // 清空之前的结果
    enhancedImage.value = ''
    analysisResult.value = ''
    healthAdvice.value = ''
    formattedAdvice.value = ''
    imageInfo.bucket = ''
    imageInfo.objectKey = ''
    
    // 显示骨架屏
    showOriginSkeleton.value = true
    showEnhancedSkeleton.value = true
    
    // 显示原始图像预览
    const reader = new FileReader()
    reader.onload = (e) => {
      originImage.value = e.target?.result as string
      // 图片加载完成后隐藏骨架屏
      showOriginSkeleton.value = false
    }
    reader.readAsDataURL(file)
  }
}

// 增强图像
const handleEnhance = async () => {
  if (!imageInfo.file) {
    ElMessage.warning('请先选择图像文件')
    return
  }
  
  // 显示加载中和骨架屏
  isEnhancing.value = true
  showEnhancedSkeleton.value = true
  
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '图像增强中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // 调用图像增强接口
    const result = await enhanceImage(imageInfo.file)
    
    // 保存图像信息
    imageInfo.bucket = result.bucket
    imageInfo.objectKey = result.objectKey
    
    // 获取增强后的图像URL并显示
    const imageUrl = fileRequest.getFileUrl(result.bucket, result.objectKey)
    console.log('增强后图像URL:', imageUrl)
    enhancedImage.value = imageUrl
    
    // 创建Image对象验证图像是否可加载
    const img = new Image()
    img.onload = () => {
      console.log('增强图像加载成功')
      showEnhancedSkeleton.value = false
    }
    img.onerror = () => {
      console.error('增强图像加载失败')
      ElMessage.warning('图像加载失败，但处理已完成')
      showEnhancedSkeleton.value = false
    }
    img.src = imageUrl
    
    ElMessage.success('图像增强成功')
  } catch (error) {
    console.error('图像增强失败', error)
    ElMessage.error('图像增强失败，请重试')
    showEnhancedSkeleton.value = false
  } finally {
    // 关闭加载
    loadingInstance.close()
    isEnhancing.value = false
    
    // 确保5秒后一定会隐藏骨架屏，防止长时间加载
    setTimeout(() => {
      showEnhancedSkeleton.value = false
    }, 5000)
  }
}

// 监听增强后图像加载完成
const enhancedImageLoaded = () => {
  console.log('增强图像DOM元素加载完成')
  showEnhancedSkeleton.value = false
}

// 分析图像
const handleAnalyze = async () => {
  if (!imageInfo.bucket || !imageInfo.objectKey) {
    ElMessage.warning('请先进行图像增强')
    return
  }
  
  // 显示加载中和骨架屏
  isAnalyzing.value = true
  showAnalysisSkeleton.value = true
  
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '多模态大语言模型分析中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // 调用图像分析接口
    console.log('正在分析图像，参数:', {
      bucket: imageInfo.bucket,
      objectKey: imageInfo.objectKey
    })
    const result = await analyzeImage(imageInfo.bucket, imageInfo.objectKey)
    
    // 显示分析结果
    analysisResult.value = result.analysis
    
    ElMessage.success('图像分析成功')
  } catch (error: any) {
    console.error('图像分析失败', error)
    // 显示更详细的错误信息
    const errorMessage = error.response?.data?.msg || error.message || '图像分析失败，请重试'
    ElMessage.error(`图像分析失败: ${errorMessage}`)
  } finally {
    // 关闭加载
    loadingInstance.close()
    isAnalyzing.value = false
    showAnalysisSkeleton.value = false
  }
}

// 获取健康建议
const handleGetAdvice = async () => {
  if (!analysisResult.value) {
    ElMessage.warning('请先进行图像分析')
    return
  }
  
  // 显示加载中和骨架屏
  isGeneratingAdvice.value = true
  showAdviceSkeleton.value = true
  
  const loadingInstance = ElLoading.service({
    lock: true,
    text: 'AI健康顾问生成建议中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // 调用健康建议接口
    console.log('正在生成健康建议')
    const result = await getHealthAdvice(analysisResult.value)
    
    // 显示健康建议
    healthAdvice.value = result.advice
    
    ElMessage.success('健康建议生成成功')
  } catch (error: any) {
    console.error('健康建议生成失败', error)
    // 显示更详细的错误信息
    const errorMessage = error.response?.data?.msg || error.message || '健康建议生成失败，请重试'
    ElMessage.error(`健康建议生成失败: ${errorMessage}`)
  } finally {
    // 关闭加载
    loadingInstance.close()
    isGeneratingAdvice.value = false
    showAdviceSkeleton.value = false
  }
}
</script>

<template>
  <div class="heart-image-analysis">
    <div class="page-header">
      <div class="tech-badge">
        <el-icon><Picture /></el-icon>
        <span>多模态大语言模型驱动</span>
      </div>
      <h1>医学影像智能分析</h1>
      <p>通过多模态大语言模型技术，智能分析医学CT/MRI影像，辅助医生诊断</p>
    </div>
    
    <el-card class="main-card">
      <div class="upload-section">
        <div class="upload-instruction">
          <el-icon class="instruction-icon"><Upload /></el-icon>
          <div class="instruction-text">
            <h3>上传医学医学影像</h3>
            <p>上传您的医学CT或MRI扫描图像，系统会自动进行增强和分析</p>
          </div>
        </div>
        <el-upload
          class="image-uploader"
          action="#"
          :auto-upload="false"
          accept="image/*"
          :show-file-list="false"
          @change="handleImageSelect"
        >
          <el-button type="primary" size="large">
            <el-icon><Upload /></el-icon>
            选择医学影像
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持jpg、png等常见图像格式，建议选择医学医学影像
            </div>
          </template>
        </el-upload>
      </div>
    </el-card>
    
    <div class="image-processing-section">
      <div class="image-container">
        <!-- 原始图像卡片 -->
        <el-card class="image-card">
          <template #header>
            <div class="card-header">
              <el-icon><PictureFilled /></el-icon>
              <span>原始图像</span>
            </div>
          </template>
          
          <el-skeleton :loading="showOriginSkeleton" animated class="image-skeleton">
            <template #template>
              <div class="skeleton-content">
                <el-skeleton-item variant="image" style="width: 100%; height: 300px" />
                <el-skeleton-item variant="button" style="width: 30%; height: 40px; margin-top: 16px" />
              </div>
            </template>
            
            <template #default>
              <div class="image-wrapper" :class="{ 'empty-wrapper': !originImage }">
                <img v-if="originImage" :src="originImage" alt="原始图像" />
                <div v-else class="empty-placeholder">
                  <el-icon><PictureFilled /></el-icon>
                  <span>请选择图像</span>
                </div>
              </div>
              <div class="button-wrapper">
                <el-button 
                  type="primary" 
                  @click="handleEnhance" 
                  :loading="isEnhancing"
                  :disabled="!originImage"
                  size="large"
                >
                  <el-icon><Brush /></el-icon>
                  图像增强
                </el-button>
              </div>
            </template>
          </el-skeleton>
        </el-card>
        
        <!-- 增强后图像卡片 -->
        <el-card class="image-card">
          <template #header>
            <div class="card-header">
              <el-icon><PictureRounded /></el-icon>
              <span>增强后图像</span>
            </div>
          </template>
          
          <el-skeleton :loading="showEnhancedSkeleton" animated class="image-skeleton">
            <template #template>
              <div class="skeleton-content">
                <el-skeleton-item variant="image" style="width: 100%; height: 300px" />
                <el-skeleton-item variant="button" style="width: 30%; height: 40px; margin-top: 16px" />
              </div>
            </template>
            
            <template #default>
              <div class="image-wrapper" :class="{ 'empty-wrapper': !enhancedImage }">
                <img v-if="enhancedImage" :src="enhancedImage" alt="增强后图像" @load="enhancedImageLoaded" />
                <div v-else class="empty-placeholder">
                  <el-icon><PictureRounded /></el-icon>
                  <span>等待增强</span>
                </div>
              </div>
              <div class="button-wrapper">
                <el-button 
                  type="success" 
                  @click="handleAnalyze" 
                  :loading="isAnalyzing"
                  :disabled="!enhancedImage"
                  size="large"
                >
                  <el-icon><Operation /></el-icon>
                  图像分析
                </el-button>
              </div>
            </template>
          </el-skeleton>
        </el-card>
      </div>
      
      <!-- 分析结果卡片 -->
      <el-card class="analysis-result" v-if="analysisResult || showAnalysisSkeleton">
        <template #header>
          <div class="card-header">
            <el-icon><DataAnalysis /></el-icon>
            <span>AI分析结果</span>
            <el-tag type="success" size="small" effect="dark" class="ai-tag">多模态大语言模型</el-tag>
          </div>
        </template>
        
        <el-skeleton :loading="showAnalysisSkeleton" animated class="analysis-skeleton">
          <template #template>
            <div class="skeleton-content">
              <el-skeleton-item variant="text" style="width: 100%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 95%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 90%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 98%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 93%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 100%; height: 16px; margin-bottom: 10px" />
            </div>
          </template>
          
          <template #default>
            <div class="result-content">
              <el-alert
                title="AI辅助诊断"
                type="info"
                description="以下分析结果仅供参考，请结合专业医生的诊断意见"
                :closable="false"
                show-icon
              />
              <pre>{{ analysisResult }}</pre>
              
              <div class="advice-button-wrapper">
                <el-button 
                  type="warning" 
                  @click="handleGetAdvice" 
                  :loading="isGeneratingAdvice"
                  size="large"
                >
                  <el-icon><Guide /></el-icon>
                  获取健康建议
                </el-button>
              </div>
            </div>
          </template>
        </el-skeleton>
      </el-card>
      
      <!-- 健康建议卡片 - 修改为使用markdown渲染 -->
      <el-card class="health-advice" v-if="healthAdvice || showAdviceSkeleton">
        <template #header>
          <div class="card-header">
            <el-icon><Guide /></el-icon>
            <span>AI健康建议</span>
            <el-tag type="danger" size="small" effect="dark" class="ai-tag">DeepSeek</el-tag>
          </div>
        </template>
        
        <el-skeleton :loading="showAdviceSkeleton" animated class="advice-skeleton">
          <template #template>
            <div class="skeleton-content">
              <el-skeleton-item variant="text" style="width: 100%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 95%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 90%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 98%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 93%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 100%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 91%; height: 16px; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 94%; height: 16px; margin-bottom: 10px" />
            </div>
          </template>
          
          <template #default>
            <div class="advice-content">
              <el-alert
                title="AI健康顾问"
                type="warning"
                description="以下健康建议由DeepSeek大语言模型生成，仅供参考，请遵循医生的专业建议"
                :closable="false"
                show-icon
              />
              <!-- 使用v-html渲染markdown内容 -->
              <div class="markdown-content" v-html="formattedAdvice"></div>
            </div>
          </template>
        </el-skeleton>
      </el-card>
      
      <!-- 技术说明部分 -->
      <el-card class="tech-info" v-if="analysisResult">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>技术说明</span>
          </div>
        </template>
        <div class="tech-content">
          <p>本系统采用了先进的多模态大语言模型(VLM)技术，能够同时理解图像内容和文本知识。系统集成了以下技术：</p>
          <ul>
            <li><strong>图像增强技术：</strong> 改善图像清晰度、对比度和噪声，突出医学结构细节</li>
            <li><strong>多模态理解：</strong> 融合视觉和语言模态，分析影像结构和潜在异常</li>
            <li><strong>AI健康顾问：</strong> 基于DeepSeek大语言模型，根据分析结果生成个性化健康建议</li>
          </ul>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.heart-image-analysis {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  background-color: #f5f7fa;
  min-height: calc(100vh - 130px);
}

.page-header {
  margin-bottom: 30px;
  text-align: center;
  position: relative;
  padding: 20px 0;
}

.page-header h1 {
  font-size: 32px;
  margin-bottom: 15px;
  color: #303133;
  font-weight: 600;
}

.page-header p {
  font-size: 16px;
  color: #606266;
  max-width: 700px;
  margin: 0 auto;
}

.tech-badge {
  position: relative;
  display: inline-flex;
  align-items: center;
  background-color: #ecf5ff;
  color: #409EFF;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.tech-badge .el-icon {
  margin-right: 6px;
  font-size: 16px;
}

.main-card {
  margin-bottom: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.upload-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
}

.upload-instruction {
  display: flex;
  align-items: center;
  max-width: 60%;
}

.instruction-icon {
  font-size: 36px;
  color: #409EFF;
  margin-right: 20px;
}

.instruction-text h3 {
  font-size: 18px;
  margin-bottom: 8px;
  color: #303133;
}

.instruction-text p {
  color: #606266;
  font-size: 14px;
}

.image-uploader {
  text-align: center;
}

.el-upload__tip {
  color: #909399;
  margin-top: 8px;
}

.image-processing-section {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.image-container {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.image-card {
  flex: 1;
  min-width: 300px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
}

.card-header .el-icon {
  margin-right: 8px;
  font-size: 18px;
}

.ai-tag {
  margin-left: auto;
}

.image-wrapper {
  width: 100%;
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.empty-wrapper {
  background-color: #f5f7fa;
  border: 2px dashed #dcdfe6;
}

.empty-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #909399;
}

.empty-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.image-wrapper img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.button-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.advice-button-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  margin-bottom: 10px;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.image-skeleton, .analysis-skeleton, .advice-skeleton {
  width: 100%;
}

.analysis-result, .health-advice {
  margin-top: 10px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.result-content, .advice-content {
  padding: 10px;
}

.result-content pre {
  white-space: pre-wrap;
  font-family: inherit;
  line-height: 1.8;
  margin-top: 16px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
  color: #303133;
}

.advice-text {
  margin-top: 20px;
}

.health-advice .card-header .el-icon {
  color: #e6a23c;
}

/* Markdown格式化内容的样式 */
.markdown-content {
  padding: 16px;
  margin-top: 16px;
  background-color: #f8f9fa;
  border-radius: 4px;
  color: #303133;
}

/* 覆盖markdown的样式 */
:deep(.markdown-content h3) {
  margin-top: 24px;
  margin-bottom: 16px;
  color: #e6a23c;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

:deep(.markdown-content ul) {
  padding-left: 20px;
  margin-bottom: 16px;
}

:deep(.markdown-content li) {
  margin-bottom: 8px;
  line-height: 1.6;
}

:deep(.markdown-content p) {
  margin-bottom: 16px;
  line-height: 1.6;
}

:deep(.markdown-content strong) {
  font-weight: 600;
  color: #409EFF;
}

.tech-info {
  margin-top: 10px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.tech-content {
  color: #606266;
  line-height: 1.8;
}

.tech-content ul {
  padding-left: 20px;
  margin-top: 10px;
}

.tech-content li {
  margin-bottom: 10px;
}
</style> 