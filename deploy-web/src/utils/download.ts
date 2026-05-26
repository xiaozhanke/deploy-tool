import { fileDownload } from '@/api/api'

/**
 * 下载文件
 * @param id 文件ID
 * @param filename 文件名（可选，如果不提供则使用服务器返回的文件名）
 */
export const downloadFile = async (id: string, filename?: string) => {
  try {
    const response = await fileDownload(id)
    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition']
    const contentType = response.headers['content-type']
    let defaultFilename = 'download'

    if (contentDisposition) {
      // 尝试从 Content-Disposition 中提取文件名
      const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition)
      if (matches != null && matches[1]) {
        defaultFilename = matches[1].replace(/['"]/g, '')
        // 处理 MIME 编码
        defaultFilename = decodeMimeEncodedString(defaultFilename)
      }
    }

    // 创建下载链接
    const blob = new Blob([response.data], { type: contentType || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename || defaultFilename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElNotification.error('下载文件失败: ' + extractErrorMessage(error))
    throw error
  }
}

/**
 * 解析 MIME 编码的文件名
 * @param str 编码后的字符串
 * @returns 解码后的文件名
 */
function decodeMimeEncodedString(str: string): string {
  // 处理 RFC 2047 编码
  const mimeRegex = /=\?([^?]+)\?([^?]+)\?([^?]+)\?=/g
  return str.replace(mimeRegex, (match, charset, encoding, text) => {
    if (encoding.toUpperCase() === 'Q') {
      // Quoted-printable 编码
      return text
        .replace(/=([0-9A-F]{2})/g, (_match: string, hex: string) => String.fromCharCode(parseInt(hex, 16)))
        .replace(/_/g, ' ')
    } else if (encoding.toUpperCase() === 'B') {
      // Base64 编码
      return atob(text)
    }
    return match
  })
}
