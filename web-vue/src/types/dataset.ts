import type { UserVO } from './user'

/**
 * 数据集VO
 */
export interface DatasetVO {
  /**
   * 数据集ID
   */
  id: number
  
  /**
   * 数据集名称
   */
  name: string
  
  /**
   * 数据集描述
   */
  description?: string
  
  /**
   * 数据集桶
   */
  bucket?: string

  /**
   * 数据集对象键
   */
  objectKey?: string
  
  /**
   * 训练样例数量
   */
  trainCount: number
  
  /**
   * 验证样例数量
   */
  valCount: number
  
  /**
   * 状态(0:未验证 1:验证中 2:验证成功 3:验证失败)
   */
  status: number
  
  /**
   * 状态名称
   */
  statusName: string
  
  /**
   * 错误消息
   */
  errorMsg?: string
  
  /**
   * 创建用户ID
   */
  createUserId: number
  
  /**
   * 创建用户
   */
  createUser: UserVO
  
  /**
   * 创建时间
   */
  createTime: string
  
  /**
   * 更新时间
   */
  updateTime: string
}

/**
 * 数据集添加DTO
 */
export interface DatasetAddDTO {
  /**
   * 数据集名称
   */
  name: string
  
  /**
   * 数据集描述
   */
  description?: string
}

/**
 * 数据集查询DTO
 */
export interface DatasetQueryDTO {
  /**
   * 当前页码
   */
  current?: number
  
  /**
   * 每页记录数
   */
  size?: number
  
  /**
   * 数据集名称
   */
  name?: string
  
  /**
   * 状态(0:未验证 1:验证中 2:验证成功 3:验证失败)
   */
  status?: number
  
  /**
   * 创建用户ID
   */
  createUserId?: number
} 