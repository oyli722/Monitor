/**
 * 表单验证规则
 */

// 邮箱验证正则
export const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

// IP地址验证正则
export const ipPattern = /^(\d{1,3}\.){3}\d{1,3}$/

// 用户名验证：4-20位字母、数字、下划线
export const usernamePattern = /^[a-zA-Z0-9_]{4,20}$/

// 密码验证：8-20位，至少包含字母和数字
export const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d\W_]{8,20}$/

// 手机号验证（国内）
export const phonePattern = /^1[3-9]\d{9}$/

/**
 * 验证邮箱格式
 */
export function isEmail(email: string): boolean {
  return emailPattern.test(email)
}

/**
 * 验证IP地址格式
 */
export function isIP(ip: string): boolean {
  if (!ipPattern.test(ip)) return false
  const parts = ip.split('.')
  return parts.every((part) => {
    const num = parseInt(part, 10)
    return num >= 0 && num <= 255
  })
}

/**
 * 验证用户名格式
 */
export function isUsername(username: string): boolean {
  return usernamePattern.test(username)
}

/**
 * 验证密码强度
 */
export function isPassword(password: string): boolean {
  return passwordPattern.test(password)
}

/**
 * 验证手机号格式
 */
export function isPhone(phone: string): boolean {
  return phonePattern.test(phone)
}

/**
 * 生成表单验证规则
 */
export const formRules = {
  // 用户名规则
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: usernamePattern,
      message: '用户名长度4-20位，只能包含字母、数字和下划线',
      trigger: 'blur',
    },
  ],

  // 邮箱规则
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: emailPattern,
      message: '请输入正确的邮箱格式',
      trigger: 'blur',
    },
  ],

  // 密码规则
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: passwordPattern,
      message: '密码长度8-20位，至少包含字母和数字',
      trigger: 'blur',
    },
  ],

  // 确认密码规则
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value !== rule.form?.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],

  // IP地址规则
  ip: [
    { required: true, message: '请输入IP地址', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (!isIP(value)) {
          callback(new Error('请输入正确的IP地址'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],

  // 验证码规则
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度不正确', trigger: 'blur' },
  ],

  // Token规则
  token: [
    { required: true, message: '请输入Token', trigger: 'blur' },
    { min: 1, message: 'Token不能为空', trigger: 'blur' },
  ],
}

export default formRules
