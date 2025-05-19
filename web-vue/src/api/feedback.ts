import { request } from '@/api/request'
import type { PageVO } from '@/types/common'

/**
 * 评估反馈接口类型定义
 */

// 评估反馈创建DTO
export interface FeedbackCreateDTO {
  modelId: number; // 模型ID
  content: string; // 反馈内容
  score: number; // 评分(1-5)
  metrics: string; // 评估指标(JSON格式)
  originalImageBucket?: string; // 原始图片桶
  originalImageKey?: string; // 原始图片键
  labelImageBucket?: string; // 标签图片桶
  labelImageKey?: string; // 标签图片键
  overlayImageBucket?: string; // 叠加图片桶
  overlayImageKey?: string; // 叠加图片键
}

// 评估反馈VO
export interface FeedbackVO {
  id: number; // ID
  modelId: number; // 模型ID
  modelName: string; // 模型名称
  content: string; // 反馈内容
  score: number; // 评分(1-5)
  metrics: string; // 评估指标(JSON格式)
  originalImageUrl?: string; // 原始图片URL
  labelImageUrl?: string; // 标签图片URL
  overlayImageUrl?: string; // 叠加图片URL
  createUserId: number; // 创建用户ID
  createUserName: string; // 创建用户名
  createTime: string; // 创建时间
}

// 分页查询参数
export interface FeedbackQueryParams {
  current: number; // 当前页
  size: number; // 每页大小
  modelId?: number; // 模型ID
}

/**
 * 创建评估反馈
 * @param data 创建参数
 * @returns 评估反馈ID
 */
export function createFeedback(data: FeedbackCreateDTO) {
  return request.post<number>('/feedback/add', data)
}

/**
 * 根据ID获取评估反馈
 * @param id 评估反馈ID
 * @returns 评估反馈详情
 */
export function getFeedbackById(id: number) {
  return request.get<FeedbackVO>(`/feedback/detail/${id}`)
}

/**
 * 分页获取反馈列表
 * @param params 查询参数
 * @returns 分页结果
 */
export function pageFeedbacks(params: FeedbackQueryParams) {
  return request.get<PageVO<FeedbackVO>>('/feedback/page', { params })
}

/**
 * 删除评估反馈
 * @param id 评估反馈ID
 * @returns 操作结果
 */
export function deleteFeedback(id: number) {
  return request.delete<boolean>(`/feedback/delete/${id}`)
} 