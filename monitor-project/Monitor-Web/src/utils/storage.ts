/**
 * 本地存储封装
 */

class Storage {
  private prefix = 'monitor_'

  private getKey(key: string): string {
    return this.prefix + key
  }

  // 设置数据
  set(key: string, value: any): void {
    try {
      const serializedValue = JSON.stringify(value)
      localStorage.setItem(this.getKey(key), serializedValue)
    } catch (error) {
      console.error('Storage set error:', error)
    }
  }

  // 获取数据
  get<T = any>(key: string): T | null {
    try {
      const item = localStorage.getItem(this.getKey(key))
      if (item === null) return null
      return JSON.parse(item)
    } catch (error) {
      console.error('Storage get error:', error)
      return null
    }
  }

  // 删除数据
  remove(key: string): void {
    localStorage.removeItem(this.getKey(key))
  }

  // 清空所有数据
  clear(): void {
    const keys = Object.keys(localStorage)
    keys.forEach((key) => {
      if (key.startsWith(this.prefix)) {
        localStorage.removeItem(key)
      }
    })
  }

  // 获取token
  getToken(): string | null {
    return this.get<string>('token')
  }

  // 设置token
  setToken(token: string): void {
    this.set('token', token)
  }

  // 删除token
  removeToken(): void {
    this.remove('token')
  }

  // 获取用户信息
  getUserInfo(): any | null {
    return this.get('userInfo')
  }

  // 设置用户信息
  setUserInfo(userInfo: any): void {
    this.set('userInfo', userInfo)
  }

  // 删除用户信息
  removeUserInfo(): void {
    this.remove('userInfo')
  }
}

export default new Storage()
