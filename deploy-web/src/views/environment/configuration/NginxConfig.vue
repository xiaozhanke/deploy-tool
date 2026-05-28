<script setup lang="ts">
import { View, Plus, Edit, EditPen, Delete, Refresh, Odometer, Loading, SwitchButton, MagicStick } from '@element-plus/icons-vue'
import { sshExecCommand, sshWriteFile } from '@/api/api'
import CodeEditor from '@/components/code-editor/index.vue'
import type { File, NginxConfigParams } from '@/types/environment'
import type { NginxLayout } from '@/utils/nginxLayout'
import { detectNginxLayout, emptyNginxLayout, nginxCommand } from '@/utils/nginxLayout'
import NginxConfigAdd from './NginxConfigAdd.vue'
import NginxConfigEdit from './NginxConfigEdit.vue'

const props = defineProps<{
  homeDir: string
}>()

const sessionId = inject('sessionId') as Ref<string>
// 远程探测出的 Nginx 布局；configDir 默认从中取，用户可手动覆盖
const nginxLayout = ref<NginxLayout>(emptyNginxLayout())
const configDir = ref<string>('')
const detecting = ref<boolean>(false)
// 文件列表
const fileList = ref<File[]>([])
// 编辑器引用
const codeEditorRef = ref<InstanceType<typeof CodeEditor>>()
const currentNginxConfig = ref<NginxConfigParams>({
  configName: '',
  frontEndHost: 'localhost',
  frontEndPort: 0,
  frontEndStaticDir: '',
  backEndHost: 'localhost',
  backEndPort: 0,
})
const addVisible = ref<boolean>(false)
const editVisible = ref<boolean>(false)

// 检查 Nginx 是否已探测到
const checkNginxInstalled = () => {
  if (!nginxLayout.value.installed) {
    ElMessage.error('未检测到 Nginx，请先点击"重新探测"或确认远程主机已安装 Nginx')
    return false
  }
  return true
}

// 探测远程 Nginx 布局
const handleDetectLayout = async () => {
  if (!sessionId.value) {
    return ElMessage.warning('请先连接服务器')
  }
  detecting.value = true
  try {
    const layout = await detectNginxLayout(sessionId.value)
    nginxLayout.value = layout
    if (layout.installed) {
      configDir.value = layout.preferredIncludeDir
      ElNotification.success(`检测到 Nginx (${layout.binary})，配置目录 ${configDir.value}`)
    } else {
      // fallback 到 deploy-tool 自编译安装的默认路径
      configDir.value = `${props.homeDir}/environment/nginx/conf/conf.d`
      ElMessage.warning(`未检测到 Nginx，回退到默认目录 ${configDir.value}`)
    }
  } catch (error) {
    ElNotification.error('Nginx 布局探测失败：' + extractErrorMessage(error))
  } finally {
    detecting.value = false
  }
}

// 手动覆盖 configDir（仅当前会话生效，不持久化）
const handleEditConfigDir = () => {
  ElMessageBox.prompt('请输入 Nginx 配置文件目录（绝对路径）', '手动设置配置目录', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: configDir.value,
    inputPattern: /^\/.+/,
    inputErrorMessage: '请输入以 / 开头的绝对路径',
  })
    .then(async ({ value }) => {
      configDir.value = value.replace(/\/+$/, '')
      await fetchFileList()
    })
    .catch(() => {})
}

// 获取文件列表
const fetchFileList = async () => {
  if (!sessionId.value) {
    return ElMessage.warning('请先连接服务器')
  }
  if (!configDir.value) {
    fileList.value = []
    return
  }
  try {
    const data = await sshExecCommand(
      sessionId.value,
      // 用 find -exec 而不是 stat ${configDir}/* —— 后者在空目录下 shell 不展开 glob，stat 把 "*" 当字面文件名报错
      `find ${configDir.value} -maxdepth 1 -type f -exec stat --printf='{"path":"%n","size":%s,"updateTime":"%y"}\n' {} +`,
    )
    const { exitCode, result } = data
    if (exitCode !== 0) {
      fileList.value = []
      return ElNotification.error('获取文件列表失败:' + result)
    }
    // 将 JSON 字符串解析为对象列表
    fileList.value = result
      .split('\n')
      // 过滤空行
      .filter((line) => line.trim() !== '')
      // 解析 JSON
      .map((line) => {
        const file = JSON.parse(line)
        // 提取文件名
        const name = file.path.split('/').pop() || ''
        return { ...file, name }
      })
      // 过滤掉 .default 结尾的文件
      .filter((file) => !file.name.endsWith('.default'))
  } catch (error) {
    ElNotification.error('获取文件列表失败:' + extractErrorMessage(error))
  }
}

// 新建 Nginx 配置文件
const handleNginxConfigAdd = () => {
  addVisible.value = true
}

// Nginx 配置文件提交
const handleNginxConfigSubmit = async (fileName: string, fileContent: string, edit: boolean) => {
  // 走后端 SFTP+sudo 混合通道：内容通过 SFTP 协议字段送达临时文件后由 sudo mv 提权落盘，
  // 既能写 root 拥有的 /etc/nginx/conf.d，又不让用户内容进入任何 shell 解释。
  try {
    await sshWriteFile(sessionId.value, `${configDir.value}/${fileName}`, fileContent, true)
    ElNotification.success(`${edit ? '编辑' : '新建'} Nginx 配置文件成功`)
    await handleRefresh()
  } catch (error) {
    ElNotification.error(`${edit ? '编辑' : '新建'} Nginx 配置文件失败: ${extractErrorMessage(error)}`)
  }
}

// 查看文件
const handleFileView = (filePath: string) => {
  codeEditorRef.value?.viewFile(`${filePath}`)
}

// 手动编辑文件
const handleFileEditManual = (filePath: string) => {
  codeEditorRef.value?.editFile(`${filePath}`)
}

// 简化编辑文件
const handleFileEditSimple = async (filePath: string) => {
  currentNginxConfig.value = await parseNginxConfig(filePath)
  editVisible.value = true
}

// 删除文件
const handleFileDelete = (filePath: string) => {
  ElMessageBox.confirm('是否删除文件？', '提示', {
    type: 'warning',
    showCancelButton: true,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        // /etc/nginx/conf.d 等系统目录归属 root，普通用户删不掉；和 nginx -t / -s reload 走同一个 sudo -n
        const data = await sshExecCommand(sessionId.value, `sudo -n rm -f ${filePath}`)
        if (data.exitCode !== 0) {
          return ElNotification.error('文件删除失败:' + (data.result || '远程命令执行失败'))
        }
        ElNotification.success('文件删除成功')
        await handleRefresh()
      } catch (error) {
        ElNotification.error('文件删除失败:' + extractErrorMessage(error))
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

// 重命名文件
const handleFileRename = (fileName: string) => {
  ElMessageBox.prompt('请输入新文件名', '重命名文件', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^[a-zA-Z0-9_.-]+$/,
    inputErrorMessage: '文件名只能包含字母、数字、下划线和点',
  })
    .then(async ({ value }) => {
      try {
        const data = await sshExecCommand(
          sessionId.value,
          // 同 delete：写 /etc/nginx/conf.d 需要 root，沿用 sudo -n
          `sudo -n mv ${configDir.value}/${fileName} ${configDir.value}/${value}`,
        )
        if (data.exitCode !== 0) {
          return ElNotification.error('文件重命名失败:' + (data.result || '远程命令执行失败'))
        }
        ElNotification.success('文件重命名成功')
        await handleRefresh()
      } catch (error) {
        ElNotification.error('文件重命名失败:' + extractErrorMessage(error))
      }
    })
    .catch(() => {
      ElMessage.info('已取消重命名')
    })
}

// 解析 Nginx 配置文件
const parseNginxConfig = async (filePath: string): Promise<NginxConfigParams> => {
  if (!sessionId.value) {
    ElMessage.warning('请先连接服务器')
    throw new Error('请先连接服务器')
  }

  try {
    const data = await sshExecCommand(sessionId.value, `cat ${filePath}`)
    const { exitCode, result } = data
    if (exitCode !== 0) {
      ElNotification.error('获取文件内容失败:' + result)
      throw new Error('获取文件内容失败:' + result)
    }
    const fileName = filePath.split('/').pop() || ''
    const configName = fileName.replace(/\.conf$/, '')
    const params: NginxConfigParams = {
      configName,
      frontEndHost: 'localhost',
      frontEndPort: 0,
      frontEndStaticDir: '',
      backEndHost: 'localhost',
      backEndPort: 0,
    }

    const lines = result.split('\n')
    for (const line of lines) {
      const trimmedLine = line.trim()

      // 匹配后端地址和端口
      if (trimmedLine.startsWith('server')) {
        const match = trimmedLine.match(/server\s+([\w.-]+):(\d+);/)
        if (match) {
          params.backEndHost = match[1]
          params.backEndPort = Number(match[2])
        }
      }

      // 匹配前端端口
      if (trimmedLine.startsWith('listen')) {
        const match = trimmedLine.match(/listen\s+(\d+);/)
        if (match) {
          params.frontEndPort = Number(match[1])
        }
      }

      // 匹配前端地址
      if (trimmedLine.startsWith('server_name')) {
        const match = trimmedLine.match(/server_name\s+([\w.-]+);/)
        if (match) {
          params.frontEndHost = match[1]
        }
      }

      // 匹配前端静态资源路径
      if (trimmedLine.startsWith('root')) {
        const match = trimmedLine.match(/root\s+(.+);/)
        if (match) {
          params.frontEndStaticDir = match[1]
        }
      }
    }
    return params
  } catch (error) {
    ElNotification.error('解析 Nginx 配置文件失败:' + extractErrorMessage(error))
    throw error
  }
}
// Nginx 测试配置
const handleNginxTest = async () => {
  if (!sessionId.value) {
    return ElMessage.warning('请先连接服务器')
  }
  if (!checkNginxInstalled()) return
  try {
    const data = await sshExecCommand(sessionId.value, nginxCommand(nginxLayout.value.binary, '-t'))
    const { exitCode, result } = data
    if (exitCode !== 0) {
      return ElNotification.error('测试配置失败:' + result)
    }
    ElNotification.success('测试配置成功')
  } catch (error) {
    ElNotification.error('测试配置失败:' + extractErrorMessage(error))
  }
}

// Nginx 重载配置
const handleNginxReload = async () => {
  if (!sessionId.value) {
    return ElMessage.warning('请先连接服务器')
  }
  if (!checkNginxInstalled()) return
  try {
    const data = await sshExecCommand(sessionId.value, nginxCommand(nginxLayout.value.binary, '-s reload'))
    const { exitCode, result } = data
    if (exitCode !== 0) {
      return ElNotification.error('重载配置失败:' + result)
    }
    ElNotification.success('重载配置成功')
  } catch (error) {
    ElNotification.error('重载配置失败:' + extractErrorMessage(error))
  }
}

// Nginx 启动服务
const handleNginxStart = async () => {
  if (!sessionId.value) {
    return ElMessage.warning('请先连接服务器')
  }
  if (!checkNginxInstalled()) return
  try {
    const data = await sshExecCommand(sessionId.value, nginxCommand(nginxLayout.value.binary, ''))
    const { exitCode, result } = data
    if (exitCode !== 0) {
      return ElNotification.error('启动服务失败:' + result)
    }
    ElNotification.success('启动服务成功')
  } catch (error) {
    ElNotification.error('启动服务失败:' + extractErrorMessage(error))
  }
}

// Nginx 停止服务
const handleNginxStop = async () => {
  if (!sessionId.value) {
    return ElMessage.warning('请先连接服务器')
  }
  if (!checkNginxInstalled()) return
  try {
    const data = await sshExecCommand(sessionId.value, nginxCommand(nginxLayout.value.binary, '-s stop'))
    const { exitCode, result } = data
    if (exitCode !== 0) {
      return ElNotification.error('停止服务失败:' + result)
    }
    ElNotification.success('停止服务成功')
  } catch (error) {
    ElNotification.error('停止服务失败:' + extractErrorMessage(error))
  }
}

const handleRefresh = async () => {
  if (!sessionId.value) return
  // 首次进入或尚未探测过：先探测布局
  if (!nginxLayout.value.installed && !configDir.value) {
    await handleDetectLayout()
  }
  await fetchFileList()
}

defineExpose({ handleRefresh })

onActivated(async () => {
  await handleRefresh()
})
</script>

<template>
  <section class="config-section">
    <div class="config-header">
      <span class="config-header-title">配置文件管理</span>

      <div class="action-wrapper">
        <el-tooltip content="新建配置文件">
          <el-button class="action-button" :icon="Plus" :disabled="!sessionId" @click="handleNginxConfigAdd" />
        </el-tooltip>
        <el-tooltip content="测试配置">
          <el-button
            class="action-button"
            type="warning"
            :icon="Odometer"
            :disabled="!sessionId"
            @click="handleNginxTest"
          />
        </el-tooltip>
        <el-tooltip content="重载配置">
          <el-button
            class="action-button"
            type="primary"
            :icon="Loading"
            :disabled="!sessionId"
            @click="handleNginxReload"
          />
        </el-tooltip>
        <el-tooltip content="启动服务">
          <el-button
            class="action-button"
            type="success"
            :icon="SwitchButton"
            :disabled="!sessionId"
            @click="handleNginxStart"
          />
        </el-tooltip>
        <el-tooltip content="停止服务">
          <el-button
            class="action-button"
            type="danger"
            :icon="SwitchButton"
            :disabled="!sessionId"
            @click="handleNginxStop"
          />
        </el-tooltip>
        <el-tooltip content="刷新文件列表">
          <el-button
            class="action-button"
            type="primary"
            :icon="Refresh"
            :disabled="!sessionId"
            @click="fetchFileList"
          />
        </el-tooltip>
      </div>
    </div>

    <div class="config-path">
      <span class="config-path-label">配置目录:&nbsp;</span>
      <code v-if="configDir">{{ configDir }}</code>
      <span v-else class="config-path-empty">（未探测）</span>
      <div class="config-path-actions">
        <el-tooltip content="重新探测">
          <el-button
            class="config-path-action"
            :icon="MagicStick"
            :loading="detecting"
            :disabled="!sessionId"
            @click="handleDetectLayout"
          />
        </el-tooltip>
        <el-tooltip content="手动设置">
          <el-button
            class="config-path-action"
            :icon="EditPen"
            :disabled="!sessionId"
            @click="handleEditConfigDir"
          />
        </el-tooltip>
      </div>
    </div>

    <div class="file-list-container">
      <el-empty v-if="fileList.length === 0" :description="sessionId ? '当前目录为空' : '未选择服务器'" />
      <el-table v-else :data="fileList" stripe highlight-current-row show-overflow-tooltip>
        <el-table-column prop="name" label="文件名" min-width="130px" />
        <el-table-column prop="size" label="文件大小" min-width="104px">
          <template #default="{ row }">
            <span>{{ $formatFileSize(row.size) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="172px">
          <template #default="scope">
            {{ $formatDateTime(scope.row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="366px" fixed="right" header-align="center" class-name="file-actions">
          <template #default="scope">
            <el-button type="primary" link :icon="View" @click="handleFileView(scope.row.path)">查看</el-button>
            <el-button type="primary" link :icon="Edit" @click="handleFileEditSimple(scope.row.path)"
              >简化编辑</el-button
            >
            <el-button type="primary" link :icon="Edit" @click="handleFileEditManual(scope.row.path)"
              >手动编辑</el-button
            >
            <el-button type="primary" link :icon="EditPen" @click="handleFileRename(scope.row.name)">重命名</el-button>
            <el-button type="primary" link :icon="Delete" @click="handleFileDelete(scope.row.path)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <code-editor ref="codeEditorRef" @close="handleRefresh" />

    <nginx-config-add v-if="addVisible" v-model="addVisible" @submit="handleNginxConfigSubmit" />

    <nginx-config-edit
      v-if="editVisible"
      v-model="editVisible"
      :params="currentNginxConfig"
      @submit="handleNginxConfigSubmit"
    />
  </section>
</template>

<style lang="scss" scoped>
.config-section {
  display: flex;
  flex-direction: column;
  gap: var(--layout-common-gap);
  padding: var(--layout-common-padding);
  background-color: var(--el-fill-color);
  border-radius: var(--layout-common-border-radius);

  .config-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--layout-common-gap);

    .config-header-title {
      font-size: var(--el-font-size-large);
      font-weight: bold;
    }
    .action-wrapper {
      .action-button {
        font-size: 18px;
        padding: 8px;
      }
    }
  }

  .config-path {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    background-color: white;
    padding: 4px 8px;
    border-radius: var(--el-border-radius-base);
    border: var(--el-border);
    .config-path-label {
      user-select: none;
    }
    .config-path-empty {
      color: var(--el-text-color-secondary);
    }
    .config-path-actions {
      margin-left: auto;
      display: flex;
      gap: 4px;
      .config-path-action {
        padding: 4px 8px;
      }
    }
  }

  .file-list-container {
    background-color: white;
    padding: var(--layout-common-padding);
    border-radius: var(--el-border-radius-base);
    border: var(--el-border);
    .file-actions {
      .el-button + .el-button {
        margin-left: 0;
      }
    }
  }
}
</style>
