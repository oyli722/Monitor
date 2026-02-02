/**
 * 认证相关类型定义
 */

// 登录请求参数
export interface LoginParams {
  username: string
  password: string
}

// 登录响应数据
export interface LoginResponse {
  token: string
  userInfo: UserInfo
}

// 用户信息
export interface UserInfo {
  id: string
  username: string
  email: string
  avatar?: string
  role: string
  permissions: string[]
}

// 注册请求参数
export interface RegisterParams {
  username: string
  email: string
  password: string
  confirmPassword: string
  emailCode: string
  captcha: string
  agreed: boolean
}

// 请求验证码参数
export interface AskCodeParams {
  email: string
  type: CodeType
}

// 验证码类型
export enum CodeType {
  REGISTER = 1,    // 注册
  RESET_PASSWORD = 2,  // 修改密码
}

// 重置密码请求参数
export interface ResetPasswordParams {
  email: string
  password: string
  repeatPassword: string
  emailCode: string
}
