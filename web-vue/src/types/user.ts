// 用户信息
export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string | null
  email: string | null
  role: number
  status: number
  avatarBucket?: string
  avatarObjectKey?: string
  avatarUrl?: string
  createTime?: string
  updateTime?: string
}

// 登录响应
export interface LoginResponse {
  userInfo: UserInfo
  token: string
}

// 用户角色
export enum UserRole {
  USER = 0,
  ADMIN = 1,
}

// 用户状态
export enum UserStatus {
  DISABLED = 0,
  ENABLED = 1,
}

// 用户列表查询参数
export interface UserListParams {
  page: number
  size: number
  username?: string
  realName?: string
  phone?: string
  email?: string
  role?: number
  status?: number
}

// 用户列表响应
export interface UserListResult {
  total: number
  list: UserInfo[]
}

// 添加用户参数
export interface AddUserParams {
  username: string
  password: string
  realName: string
  phone: string
  email: string
  role: number
  status: number
}

// 更新用户参数
export interface UpdateUserParams {
  id: number
  username?: string
  realName?: string
  phone?: string
  email?: string
  role?: number
  status?: number
  avatarBucket?: string
  avatarObjectKey?: string
}

// 修改密码参数
export interface UpdatePasswordParams {
  id: number
  oldPassword: string
  newPassword: string
}

// 登录参数
export interface LoginParams {
  username: string
  password: string
}

// 注册参数
export interface RegisterParams {
  username: string
  password: string
  realName: string
  phone: string
  email: string
}

// 用户查询参数
export interface UserQueryParams {
  current: number
  size: number
  username?: string
  realName?: string
  phone?: string
  email?: string
  role?: number
  status?: number
}

// 用户表单数据
export interface UserForm {
  id?: number
  username?: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  role?: number
  status?: number
  avatarBucket?: string
  avatarObjectKey?: string
}

/**
 * 用户VO
 */
export interface UserVO {
  /**
   * 用户ID
   */
  id: number
  
  /**
   * 用户名
   */
  username: string
  
  /**
   * 昵称
   */
  nickname: string
  
  /**
   * 头像
   */
  avatar?: string
  
  /**
   * 角色(0:普通用户 1:管理员)
   */
  role: number
  
  /**
   * 角色名称
   */
  roleName: string
} 