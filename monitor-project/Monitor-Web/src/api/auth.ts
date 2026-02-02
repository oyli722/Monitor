/**
 * 认证相关API
 */

import type { LoginParams, LoginResponse, RegisterParams, UserInfo } from '@/types/auth'
import type { AskCodeParams, ResetPasswordParams } from '@/types/auth'
import request from '@/utils/request'

/**
 * 用户登录
 */
export function login(params: LoginParams): Promise<LoginResponse> {
  return request.post<LoginResponse>('/auth/login', params)
}

/**
 * 用户注册
 */
export function register(params: RegisterParams): Promise<void> {
  return request.post<void>('/auth/register', params)
}

/**
 * 请求邮件验证码
 * @param params.email 邮箱
 * @param params.type 类型 1-注册 2-修改密码
 */
export function askCode(params: AskCodeParams): Promise<void> {
  return request.post<void>('/auth/ask-code', params)
}

/**
 * 重置密码
 * @param params.email 邮箱
 * @param params.password 密码（6-20个字符）
 * @param params.repeatPassword 重复密码（6-20个字符）
 * @param params.emailCode 邮箱验证码（6位数字）
 */
export function resetPassword(params: ResetPasswordParams): Promise<void> {
  return request.post<void>('/auth/reset-password', params)
}

/**
 * 获取当前用户信息
 */
export function getUserInfo(): Promise<UserInfo> {
  return request.get<UserInfo>('/auth/userinfo')
}

/**
 * 退出登录
 */
export function logout(): Promise<void> {
  return request.post<void>('/auth/logout')
}

/**
 * 刷新Token
 */
export function refreshToken(refreshToken: string): Promise<{ token: string }> {
  return request.post<{ token: string }>('/auth/refresh', { refreshToken })
}
