import { request } from '@/api/request'
import { fileRequest } from '@/api/file_request'
import { algoRequest } from './algo_request'

/**
 * 创建模型
 * @param data 模型数据
 * @returns 创建结果
 */
export function createModel(data: {
  name: string
  description: string
  datasetId: number
  trainHyperparams?: string
}) {
  return request.post('/model/add', data)
}

/**
 * 获取模型列表
 * @param params 查询参数
 * @returns 模型列表
 */
export function getModelList(params: {
  current: number
  size: number
  name?: string
  status?: number
  datasetId?: number
}) {
  return request.get('/model/page', { params })
}

/**
 * 获取模型详情
 * @param id 模型ID
 * @returns 模型详情
 */
export function getModelDetail(id: number) {
  return request.get(`/model/detail/${id}`)
}

/**
 * 训练模型
 * @param id 模型ID
 * @param trainHyperparams 训练超参数对象
 * @returns 训练结果
 */
export function trainModel(id: number, trainHyperparams?: any) {
  return request.post(`/model/train/${id}`, {
    trainHyperparams: trainHyperparams
  })
}

/**
 * 更新模型状态
 * @param data 模型数据
 * @returns 更新结果
 */
export function updateModelStatus(data: {
  id: number
  status: number
  errorMsg?: string
  trainMetrics?: string
  modelBucket?: string
  modelObjectKey?: string
}) {
  return request.post('/model/update-status', data)
}

/**
 * 更新模型训练超参数
 * @param data 模型数据
 * @returns 更新结果
 */
export function updateModelTrainParams(data: {
  id: number
  trainHyperparams: string
}) {
  return request.post('/model/update', {
    id: data.id,
    trainHyperparams: data.trainHyperparams
  })
}

/**
 * 更新模型基本信息
 * @param data 模型数据
 * @returns 更新结果
 */
export function updateModelInfo(data: {
  id: number
  name?: string
  description?: string
  datasetId?: number
}) {
  return request.post('/model/update', data)
}

/**
 * 删除模型
 * @param id 模型ID
 * @returns 删除结果
 */
export function deleteModel(id: number) {
  return request.delete(`/model/delete/${id}`)
}

/**
 * 获取模型训练状态
 * @param id 模型ID
 * @returns 训练状态
 */
export function getModelTrainingStatus(id: number) {
  return request.get(`/model/training-status/${id}`)
}

/**
 * 发布模型
 * @param id 模型ID
 * @returns 发布结果
 */
export function publishModel(id: number) {
  return request.post(`/model/publish/${id}`)
}

/**
 * 取消发布模型
 * @param id 模型ID
 * @returns 取消发布结果
 */
export function unpublishModel(id: number) {
  return request.post(`/model/unpublish/${id}`)
}

/**
 * 模型评估
 * @param params 评估参数
 * @returns 评估结果
 */
export function evaluateModel(params: {
  modelId: string | number
  imageBucket: string
  imageKey: string
  maskBucket: string
  maskKey: string
}) {
  return algoRequest.post<{
    metrics: {
      dice: number
      iou: number
      accuracy: number
      precision: number
      recall: number
      f1: number
      classMetrics: Record<string, any>
    },
    processedOrigImg: {
      bucket: string
      objectKey: string
      url: string
    },
    processedGtMask: {
      bucket: string
      objectKey: string
      url: string
    },
    predictionMask: {
      bucket: string
      objectKey: string
      url: string
    },
    overlayImage: {
      bucket: string
      objectKey: string
      url: string
    }
  }>('/prediction/evaluate', params)
}

/**
 * 图像分割预测
 * @param params 预测参数
 * @returns 预测结果
 */
export function predictImage(params: {
  modelId: string | number
  imageBucket: string
  imageKey: string
}) {
  return algoRequest.post<{
    processedOrigImg: {
      bucket: string
      objectKey: string
      url: string
    },
    predictionMask: {
      bucket: string
      objectKey: string
      url: string
    },
    overlayImage: {
      bucket: string
      objectKey: string
      url: string
    }
  }>('/prediction/predict', params)
}