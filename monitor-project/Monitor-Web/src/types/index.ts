/**
 * 通用类型定义
 */

// 分页参数
export interface PageParams {
  pageNum: number
  pageSize: number
}

// 分页响应
export interface PageResponse<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

// API响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 用户角色
export enum UserRole {
  ADMIN = 'admin',
  USER = 'user',
}

// 主机状态
export enum HostStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
}

// 主题模式
export type ThemeMode = 'light' | 'dark'
