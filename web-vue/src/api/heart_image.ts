import { algoRequest } from './algo_request'
import type { AxiosRequestConfig } from 'axios'

/**
 * 心脏图像增强
 * @param file 图像文件
 * @param config 请求配置
 * @returns Promise<{url: string, bucket: string, objectKey: string}>
 */
export function enhanceImage(file: File, config?: AxiosRequestConfig) {
  const formData = new FormData()
  formData.append('image', file)
  
  return algoRequest.post<{url: string, bucket: string, objectKey: string}>('/heart/enhance', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    ...config
  })
}

/**
 * 心脏图像分析
 * @param bucket 存储桶
 * @param objectKey 对象键
 * @param config 请求配置
 * @returns Promise<{analysis: string}>
 */
export function analyzeImage(bucket: string, objectKey: string, config?: AxiosRequestConfig) {
  return algoRequest.post<{analysis: string}>('/heart/analyze', {
    imageUrl: true,  // 添加标志位表示使用imageUrl方式
    bucket,
    objectKey
  }, config)
}

/**
 * 获取健康建议
 * @param analysis 图像分析结果
 * @param config 请求配置
 * @returns Promise<{advice: string}>
 */
export function getHealthAdvice(analysis: string, config?: AxiosRequestConfig) {
  return algoRequest.post<{advice: string}>('/heart/health-advice', {
    analysis
  }, config)
}

/**
 * 直接上传并分析图像
 * @param file 图像文件
 * @param config 请求配置
 * @returns Promise<{analysis: string}>
 */
export function uploadAndAnalyzeImage(file: File, config?: AxiosRequestConfig) {
  const formData = new FormData()
  formData.append('image', file)
  
  return algoRequest.post<{analysis: string}>('/heart/analyze', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    ...config
  })
} 