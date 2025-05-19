// 验证手机号
export const validatePhone = (phone: string) => {
  const reg = /^1[3-9]\d{9}$/
  return reg.test(phone)
}

// 验证邮箱
export const validateEmail = (email: string) => {
  const reg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  return reg.test(email)
}

// 验证密码
export const validatePassword = (password: string) => {
  const reg = /^[a-zA-Z0-9]{6,20}$/
  return reg.test(password)
}

// 验证用户名
export const validateUsername = (username: string) => {
  const reg = /^[a-zA-Z0-9_-]{4,16}$/
  return reg.test(username)
}

// 验证IP地址
export const validateIP = (ip: string) => {
  return /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/.test(ip)
}

// 验证RTSP地址
export const validateRTSP = (rtsp: string) => {
  return /^rtsp:\/\/[^\s]*$/.test(rtsp)
}

// 验证图片类型
export const validateImageType = (type: string) => {
  return ['image/jpeg', 'image/png', 'image/gif'].includes(type)
}

// 验证视频类型
export const validateVideoType = (type: string) => {
  return ['video/mp4'].includes(type)
}

// 验证文件大小（单位：MB）
export const validateFileSize = (size: number, maxSize: number) => {
  return size <= maxSize * 1024 * 1024
} 