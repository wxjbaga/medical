import type { DatasetAddDTO, DatasetQueryDTO, DatasetVO } from '@/types/dataset'
import type { PageVO, Result } from '@/types/common'
import { request } from './request'
import { fileRequest } from './file_request'

/**
 * 添加数据集
 * @param data 数据集信息
 * @param config 请求配置
 * @returns Promise<Result<number>>
 */
export const addDataset = (data: DatasetAddDTO, config = {}) => {
  return request.post<number>('/dataset/add', data, config)
}

/**
 * 上传数据集文件
 * @param id 数据集ID
 * @param formData 表单数据
 * @param config 请求配置
 * @returns Promise<Result<boolean>>
 */
export const uploadDataset = (id: number, formData: FormData, config = {}) => {
  return request.post<boolean>(`/dataset/upload/${id}`, formData, {
    ...config,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除数据集
 * @param id 数据集ID
 * @param config 请求配置
 * @returns Promise<Result<boolean>>
 */
export const deleteDataset = (id: number, config = {}) => {
  return request.delete<boolean>(`/dataset/delete/${id}`, config)
}

/**
 * 获取数据集详情
 * @param id 数据集ID
 * @param config 请求配置
 * @returns Promise<Result<DatasetVO>>
 */
export const getDatasetDetail = (id: number, config = {}) => {
  return request.get<DatasetVO>(`/dataset/detail/${id}`, config)
}

/**
 * 分页查询数据集
 * @param params 查询参数
 * @param config 请求配置
 * @returns Promise<Result<PageVO<DatasetVO>>>
 */
export const getDatasetPage = (params: DatasetQueryDTO, config = {}) => {
  return request.get<PageVO<DatasetVO>>('/dataset/page', { ...config, params })
}

/**
 * 获取所有数据集
 * @param config 请求配置
 * @returns Promise<Result<DatasetVO[]>>
 */
export const getAllDatasets = (config = {}) => {
  return request.get<DatasetVO[]>('/dataset/list', config)
}

/**
 * 验证数据集
 * @param id 数据集ID
 * @param config 请求配置
 * @returns Promise<Result<boolean>>
 */
export const validateDataset = (id: number, config = {}) => {
  return request.post<boolean>(`/dataset/validate/${id}`, {}, config)
}

/**
 * 下载数据集文件
 * @param bucket 数据集存储桶
 * @param objectKey 数据集文件对象键
 * @param fileName 下载后的文件名
 * @returns Promise<void>
 */
export const downloadDataset = async (bucket: string, objectKey: string, fileName: string) => {
  try {
    // 使用fileRequest.get获取文件内容
    const blob = await fileRequest.get(bucket, objectKey)
    
    // 创建下载链接
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName ? `${fileName}.zip` : 'dataset.zip'
    
    // 触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    URL.revokeObjectURL(downloadUrl)
  } catch (error) {
    console.error('下载数据集文件失败', error)
    throw error
  }
}

/**
 * 编辑数据集
 * @param id 数据集ID
 * @param data 数据集信息
 * @param config 请求配置
 * @returns Promise<Result<boolean>>
 */
export const updateDataset = (id: number, data: Partial<DatasetAddDTO>, config = {}) => {
  return request.put<boolean>(`/dataset/update/${id}`, data, config)
} 