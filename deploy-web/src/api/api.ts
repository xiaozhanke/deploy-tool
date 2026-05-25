import type { ExecResult, NginxConfigParams } from '@/types/environment'
import request from './request'
import type { ServerRecord, ServerParams } from '@/types/server'
import type { PageParams, PageResult } from '@/types/api'
import type { FileParams, FileRecord } from '@/types/file'
import type { LoginRequest, PasswordForm, UserProfile } from '@/types/auth'
import type { DeploymentRecord, DeploymentParams } from '@/types/deployment'

/**
 * 测试 API 可用性
 * @returns
 */
export const testPing = (): Promise<string> => {
  return request.get('/test/ping')
}

/**
 * 获取服务器列表
 */
export const serverQueryList = (): Promise<Array<ServerRecord>> => {
  return request.get('/servers')
}

/**
 * 添加服务器
 * @param server 服务器信息
 */
export const serverAdd = (server: ServerParams): Promise<ServerRecord> => {
  return request.post('/servers', server)
}

/**
 * 更新服务器信息
 * @param id 服务器 Id
 * @param server 服务器信息
 */
export const serverUpdate = (id: string, server: ServerParams): Promise<ServerRecord> => {
  return request.put(`/servers/${id}`, server)
}

/**
 * 删除服务器
 * @param id 服务器 Id
 */
export const serverDelete = (id: string): Promise<void> => {
  return request.delete(`/servers/${id}`)
}

/**
 * 获取服务器信息
 * @param id 服务器 Id
 */
export const serverQueryById = (id: string): Promise<ServerRecord> => {
  return request.get(`/servers/${id}`)
}

/**
 * 测试服务器连接
 * @param server 服务器信息
 */
export const serverTestConnection = (server: ServerParams): Promise<boolean> => {
  return request.post('/servers/test-connection', server)
}

/**
 * SSH 连接
 * @param serverId 服务器 Id
 */
export const sshConnect = (serverId: string): Promise<string> => {
  return request.post('/ssh/sessions', null, { params: { serverId } })
}

/**
 * SSH 断开连接
 * @param sessionId 会话 Id
 */
export const sshDisconnect = (sessionId: string): Promise<void> => {
  return request.delete(`/ssh/sessions/${sessionId}`)
}

/**
 * SSH 创建 Shell 通道
 * @param sessionId  会话 Id
 */
export const sshShellAdd = (sessionId: string): Promise<string> => {
  return request.post(`/ssh/sessions/${sessionId}/shell`)
}

/**
 * SSH 断开 Shell 通道
 * @param sessionId 会话 Id
 * @param channelId 通道 Id
 */
export const sshShellClose = (sessionId: string, channelId: string): Promise<void> => {
  return request.delete(`/ssh/sessions/${sessionId}/shell/${channelId}`)
}

/**
 * SSH 连接 Exec 通道并执行命令
 * @param sessionId 会话 Id
 * @param command 要执行的命令
 */
export const sshExecCommand = (sessionId: string, command: string): Promise<ExecResult> => {
  return request.post(`/ssh/sessions/${sessionId}/exec`, { command })
}

/**
 * 通过 SFTP 把文本内容覆盖写入远程文件。
 * 替代旧的「拼 cat <<EOF 走 Exec」方案，避免 path 与内容被 shell 解释。
 * @param sessionId 会话 Id
 * @param remotePath 远程文件绝对路径（POSIX 分隔符 /）
 * @param content 文件内容（UTF-8）
 */
export const sshWriteFile = (sessionId: string, remotePath: string, content: string): Promise<void> => {
  return request.post(`/ssh/sessions/${sessionId}/file`, { remotePath, content })
}

/**
 * 新建 Nginx 配置文件
 * @param params 配置文件参数
 */
export const configNginxAdd = (params: NginxConfigParams): Promise<string> => {
  return request.post('/config/nginx', params)
}

/**
 * 查询文件记录列表
 * @param params 查询参数
 * @param sort 排序参数，格式为 "字段名,排序方式"（如：updateTime,desc）
 */
export const fileQueryList = (params: FileParams, sort?: string): Promise<Array<FileRecord>> => {
  return request.get('/files/list', {
    params: { ...params, sort },
  })
}

/**
 * 分页查询文件记录列表
 * @param queryParams 查询参数
 * @param pageParams 分页参数
 * @returns 分页列表
 */
export const fileQueryPage = (
  queryParams: Partial<FileParams>,
  pageParams?: PageParams,
): Promise<PageResult<FileRecord>> => {
  return request.get('/files/page', { params: { ...queryParams, ...pageParams } })
}

/**
 * 上传文件并保存文件记录
 * @param file 文件对象
 * @param fileParams 文件参数
 */
export const fileUpload = (file: File, fileParams?: FileParams): Promise<FileRecord> => {
  const formData = new FormData()
  formData.append('file', file)
  if (fileParams) {
    Object.entries(fileParams).forEach(([key, value]) => {
      if (value !== undefined) {
        formData.append(key, value as string)
      }
    })
  }

  return request.post('/files', formData)
}

/**
 * 在目录上创建子文件夹
 * @param relativePath 相对路径
 * @param directoryName 文件夹名称
 */
export const fileCreateDirectory = (relativePath: string, directoryName: string): Promise<FileRecord> => {
  return request.post('/files/directories', { relativePath, directoryName })
}

/**
 * 根据文件 Id 删除文件和记录
 * @param id 文件 Id
 */
export const fileDelete = (id: string): Promise<void> => {
  return request.delete(`/files/${id}`)
}

/**
 * 根据文件 Id 下载文件资源
 * @param id 文件 Id
 */
export const fileDownload = (id: string) => {
  return request.get<Blob>(`/files/${id}`, {
    responseType: 'blob',
  })
}

/**
 * 更新文件记录元数据
 * @param id 文件 Id
 * @param params 文件参数
 */
export const fileUpdateMetadata = (id: string, params: FileParams): Promise<FileRecord> => {
  return request.put(`/files/${id}/metadata`, params)
}

/**
 * 更新文件记录原始文件
 * @param id 文件 Id
 * @param file 原始文件
 */
export const fileUpdateRaw = (id: string, file: File): Promise<FileRecord> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.put(`/files/${id}/raw`, formData)
}

/**
 * 查询文件绝对路径
 * @param params 文件参数
 * @returns 文件绝对路径
 */
export const fileQueryPath = (params: FileParams): Promise<string> => {
  return request.get('/files/path', { params })
}

/**
 * 根据文件 Id 查询文件绝对路径
 * @param id 文件 Id
 * @returns 文件绝对路径
 */
export const fileQueryPathById = (id: string): Promise<string> => {
  return request.get(`/files/${id}/path`)
}

/**
 * 用户登录
 * @param username 用户名
 * @param password 密码
 */
export const authLogin = (params: LoginRequest): Promise<void> => {
  return request.post('/auth/login', params, { noAuth: true })
}

/**
 * 用户登出
 */
export const authLogout = (): Promise<void> => {
  return request.post('/auth/logout')
}

/**
 * 获取当前用户
 */
export const authUserCurrent = (): Promise<UserProfile> => {
  return request.get('/auth/me')
}

/**
 * 更新当前用户信息
 * @param user 用户信息
 */
export const userProfileUpdate = (user: UserProfile) => {
  return request.put('/users/me/profiles', user)
}

/**
 * 更新当前用户密码
 * @param params 密码参数
 */
export const userPasswordUpdate = (params: PasswordForm) => {
  return request.put('/users/me/password', params)
}

/**
 * 查询部署记录列表
 * @param queryParams 查询参数
 * @param sort 排序参数
 * @returns 部署记录列表
 */
export const deploymentRecordQueryList = (
  queryParams: Partial<DeploymentParams>,
  sort?: string,
): Promise<Array<DeploymentRecord>> => {
  return request.get('/deployments/list', { params: { ...queryParams, sort } })
}

/**
 * 分页查询部署记录列表
 * @param queryParams 查询参数
 * @param pageParams 分页参数
 * @returns 分页列表
 */
export const deploymentRecordQueryPage = (
  queryParams: Partial<DeploymentParams>,
  pageParams?: PageParams,
): Promise<PageResult<DeploymentRecord>> => {
  return request.get('/deployments/page', { params: { ...queryParams, ...pageParams } })
}

/**
 * 获取部署记录
 * @param id 部署 Id
 * @returns 部署记录
 */
export const deploymentRecordQueryById = (id: string): Promise<DeploymentRecord> => {
  return request.get(`/deployments/${id}`)
}

/**
 * 创建部署记录
 * @param params 部署记录
 * @returns 部署记录
 */
export const deploymentRecordAdd = (params: DeploymentParams): Promise<DeploymentRecord> => {
  return request.post('/deployments', params)
}

/**
 * 更新部署记录
 * @param id 部署 Id
 * @param params 部署参数
 * @returns 部署记录
 */
export const deploymentRecordUpdate = (id: string, params: DeploymentParams): Promise<DeploymentRecord> => {
  return request.put(`/deployments/${id}`, params)
}

/**
 * 删除部署记录
 * @param id 部署记录
 * @returns
 */
export const deploymentRecordDelete = (id: string): Promise<void> => {
  return request.delete(`/deployments/${id}`)
}

/**
 * 启动后端应用
 * @param id 部署 Id
 * @returns
 */
export const deploymentRecordStart = (id: string): Promise<void> => {
  return request.post(`/deployments/${id}/actions/start`)
}

/**
 * 停止后端应用
 * @param id 部署 Id
 * @returns
 */
export const deploymentRecordStop = (id: string): Promise<void> => {
  return request.post(`/deployments/${id}/actions/stop`)
}

/**
 * 重启后端应用
 * @param id 部署 Id
 * @returns
 */
export const deploymentRecordRestart = (id: string): Promise<void> => {
  return request.post(`/deployments/${id}/actions/restart`)
}

/**
 * 获取后端应用运行状态
 * @param id 部署 Id
 * @returns
 */
export const deploymentRecordStatus = (id: string): Promise<DeploymentRecord> => {
  return request.get(`/deployments/${id}/status`)
}

/**
 * 更新应用包
 * @param id 部署 Id
 * @param fileRecordId 文件记录 Id
 * @returns
 */
export const deploymentRecordUpdatePackage = (id: string, fileRecordId: string): Promise<DeploymentRecord> => {
  return request.put(`/deployments/${id}/package`, null, { params: { fileRecordId } })
}
