import { request } from './request'

/**
 * 操作历史接口类型定义
 */

// 操作历史创建DTO
export interface OperationHistoryCreateDTO {
  modelId: number; // 模型ID
  operationType: number; // 操作类型：1-评估，2-分割
  originalImageBucket?: string; // 原始图片桶
  originalImageKey?: string; // 原始图片键
  resultImageBucket?: string; // 结果图片桶
  resultImageKey?: string; // 结果图片键
  overlayImageBucket?: string; // 叠加图片桶
  overlayImageKey?: string; // 叠加图片键
  metrics?: string; // 评估指标JSON字符串
}

// 操作历史VO
export interface OperationHistoryVO {
  id: number; // ID
  modelId: number; // 模型ID
  modelName: string; // 模型名称
  operationType: number; // 操作类型：1-评估，2-分割
  originalImageUrl?: string; // 原始图片URL
  resultImageUrl?: string; // 结果图片URL
  overlayImageUrl?: string; // 叠加图片URL
  metrics?: string; // 评估指标JSON字符串
  createUserId: number; // 创建用户ID
  createUserName: string; // 创建用户名
  createTime: string; // 创建时间
  updateTime: string; // 更新时间
}

// 分页查询参数
export interface HistoryQueryParams {
  current: number; // 当前页
  size: number; // 每页大小
  modelId?: number; // 模型ID
  createUsername?: string; // 创建用户名
}

/**
 * 创建操作历史
 * @param data 创建参数
 * @returns 操作历史ID
 */
export function createHistory(data: OperationHistoryCreateDTO) {
  return request.post('/operation-history/add', data)
}

/**
 * 根据ID获取操作历史
 * @param id 操作历史ID
 * @returns 操作历史详情
 */
export function getHistoryById(id: number) {
  return request.get(`/operation-history/${id}`)
}

/**
 * 分页查询操作历史
 * @param params 查询参数
 * @returns 分页结果
 */
export function pageHistories(params: HistoryQueryParams) {
  return request.get('/operation-history/page', { params })
}

/**
 * 删除操作历史
 * @param id 操作历史ID
 * @returns 操作结果
 */
export function deleteHistory(id: number) {
  return request.delete(`/operation-history/delete/${id}`)
} 