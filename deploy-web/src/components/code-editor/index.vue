<script setup lang="ts">
import { Codemirror } from 'vue-codemirror'
import { oneDark } from '@codemirror/theme-one-dark'
import { yaml } from '@codemirror/lang-yaml'
import { EditorView } from 'codemirror'
import { highlightWhitespace } from '@codemirror/view'
import { Close } from '@element-plus/icons-vue'
import { sshExecCommand, sshWriteFile } from '@/api/api'

defineOptions({
  name: 'CodeEditor',
})

const emit = defineEmits<{
  (e: 'close', value: void): void
}>()

// 从父组件获取 sessionId；若未注入则给空字符串 Ref 兜底，避免 inject 返回 undefined 后续访问 .value 抛错
const sessionId = inject<Ref<string>>('sessionId', ref(''))

// 文件路径
const filePath = ref<string>('')
// 是否显示
const visible = ref<boolean>(false)
// 是否可编辑
const editable = ref<boolean>(false)
// 文件内容
const fileContent = ref<string>('')
// 是否高亮空格
const highlightWhiteSpace = ref(false)
// 主题明暗
const theme = ref<'light' | 'dark'>('light')

// 切换高亮空格
const toggleHighlightWhiteSpace = () => {
  highlightWhiteSpace.value = !highlightWhiteSpace.value
}
// 切换主题
const toggleTheme = () => {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}
// 切换可编辑状态
const toggleEditable = () => {
  editable.value = !editable.value
}

// 动态加载主题和扩展
const extensions = computed(() => [
  yaml(),
  theme.value === 'dark' ? oneDark : EditorView.theme({}),
  highlightWhiteSpace.value ? highlightWhitespace() : [],
  EditorView.editable.of(editable.value),
])

// 获取文件内容
const fetchFileContent = async () => {
  try {
    const data = await sshExecCommand(sessionId.value, `cat ${filePath.value}`)
    const { exitCode, result } = data
    if (exitCode !== 0) {
      ElNotification.error('获取文件内容失败:' + result)
      return false
    }
    fileContent.value = result
    return true
  } catch (error) {
    ElNotification.error('获取文件内容失败:' + extractErrorMessage(error))
    return false
  }
}

// 查看文件
const viewFile = async (path: string) => {
  editable.value = false
  filePath.value = path
  const success = await fetchFileContent()
  if (success) {
    visible.value = true
  }
}

// 编辑文件
const editFile = async (path: string) => {
  editable.value = true
  filePath.value = path
  const success = await fetchFileContent()
  if (success) {
    visible.value = true
  }
}

// 保存文件
const handleSave = () => {
  ElMessageBox.confirm('是否保存文件？', '提示', {
    type: 'warning',
    showCancelButton: true,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(async () => {
      // 通过 SFTP 直接把当前内容覆盖写入远端文件：路径与内容都走 SFTP 协议字段，
      // 不再拼 `cat <<EOF > ${path}` 命令，前端不需要做任何 shell 转义。
      try {
        await sshWriteFile(sessionId.value, filePath.value, fileContent.value)
        ElNotification.success('文件保存成功')
        emit('close')
        handleClose()
      } catch (error) {
        ElNotification.error('文件保存失败:' + extractErrorMessage(error))
      }
    })
    .catch(() => {
      ElMessage.info('已取消保存')
    })
}

// 关闭窗口
const handleClose = () => {
  visible.value = false
  fileContent.value = ''
}

defineExpose({
  viewFile,
  editFile,
})
</script>

<template>
  <el-dialog v-model="visible" width="1000px" top="5vh" draggable :show-close="false" :close-on-click-modal="false">
    <template #header="{ close, titleId, titleClass }">
      <div class="dialog-header">
        <h4 :id="titleId" :class="titleClass">{{ filePath }}</h4>
        <div class="action-wrapper">
          <div class="status-bar">
            <el-tooltip content="可编辑" placement="top">
              <div :class="['status-icon', editable ? 'active' : 'inactive']" @click="toggleEditable">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24">
                  <path
                    fill="currentColor"
                    d="M4 14v-2h7v2zm0-4V8h11v2zm0-4V4h11v2zm9 14v-3.075l5.525-5.5q.225-.225.5-.325t.55-.1q.3 0 .575.113t.5.337l.925.925q.2.225.313.5t.112.55t-.1.563t-.325.512l-5.5 5.5zm6.575-5.6l.925-.975l-.925-.925l-.95.95z"
                  />
                </svg>
              </div>
            </el-tooltip>
            <el-tooltip content="是否高亮空格" placement="top">
              <div
                :class="['status-icon', highlightWhiteSpace ? 'active' : 'inactive']"
                @click="toggleHighlightWhiteSpace"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24">
                  <path
                    fill="currentColor"
                    d="M11 4v10q0 .425-.288.713T10 15t-.712-.288T9 14v-4q-1.65 0-2.825-1.175T5 6t1.175-2.825T9 2h7q.425 0 .713.287T17 3t-.288.713T16 4h-1v10q0 .425-.288.713T14 15t-.712-.288T13 14V4zm6.2 15H4q-.425 0-.712-.288T3 18t.288-.712T4 17h13.2l-.9-.9q-.275-.275-.275-.7t.275-.7t.7-.275t.7.275l2.6 2.6q.3.3.3.7t-.3.7l-2.6 2.6q-.275.275-.7.275t-.7-.275t-.275-.7t.275-.7z"
                  />
                </svg>
              </div>
            </el-tooltip>
            <el-tooltip content="主题明暗" placement="top">
              <div :class="['status-icon', theme === 'dark' ? 'active' : 'inactive']" @click="toggleTheme">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24">
                  <path
                    fill="currentColor"
                    d="M12 21q-3.75 0-6.375-2.625T3 12t2.625-6.375T12 3q.35 0 .688.025t.662.075q-1.025.725-1.638 1.888T11.1 7.5q0 2.25 1.575 3.825T16.5 12.9q1.375 0 2.525-.613T20.9 10.65q.05.325.075.662T21 12q0 3.75-2.625 6.375T12 21"
                  />
                </svg>
              </div>
            </el-tooltip>
          </div>
          <el-button class="close-button" type="danger" :icon="Close" @click="close" />
        </div>
      </div>
    </template>

    <!-- 代码编辑器 -->
    <codemirror
      v-model="fileContent"
      :extensions="extensions"
      autofocus
      :disabled="!editable"
      :style="{ maxHeight: '75vh' }"
    />

    <template #footer>
      <el-button type="warning" @click="toggleEditable">
        {{ editable ? '取消编辑' : '编辑' }}
      </el-button>
      <el-button v-show="editable" type="primary" @click="handleSave">保存</el-button>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.dialog-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  .action-wrapper {
    display: flex;
    gap: 20px;
    align-items: center;
    .status-bar {
      display: flex;
      gap: 4px;
      align-items: center;
      .status-icon {
        cursor: pointer;
        transition: color 0.3s ease;
        display: inline-block;
        height: 32px;
        width: 32px;
        line-height: 0;
        &.active {
          color: #000000;
        }
        &.inactive {
          color: #a8abb2;
        }
      }
    }
    .close-button {
      font-size: 18px;
      padding: 8px;
    }
  }
}
</style>
