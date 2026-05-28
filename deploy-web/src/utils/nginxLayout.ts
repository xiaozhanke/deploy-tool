import { sshExecCommand } from '@/api/api'

/**
 * Nginx 安装布局 —— 由远程探测脚本返回，供配置管理页决定 configDir / nginx 二进制路径
 */
export interface NginxLayout {
  /** 是否在远程主机上检测到 nginx */
  installed: boolean
  /** nginx 二进制绝对路径，例如 /usr/sbin/nginx */
  binary: string
  /** 主配置文件绝对路径，例如 /etc/nginx/nginx.conf */
  conf: string
  /** 主配置所在目录，conf 的 dirname */
  confDir: string
  /** 候选的子配置 include 目录(实际存在的) */
  includeDirs: string[]
  /** 优先使用的 include 目录 —— includeDirs[0]，否则 fallback 到 confDir/conf.d */
  preferredIncludeDir: string
}

/**
 * 远程探测脚本：用 `nginx -V` 编译期参数推断主配置位置，再 `ls -d` 探测候选子配置目录。
 * 不依赖 sudo / nginx -T(普通用户读不了被 include 的 sites-available 文件会失败)。
 * 已知覆盖：apt/yum (/etc/nginx/conf.d + sites-enabled)、apk (/etc/nginx/http.d)、源码自编 (--prefix 路径)。
 */
const DETECT_SCRIPT = `set +e
NGINX_BIN=$(command -v nginx 2>/dev/null)
[ -z "$NGINX_BIN" ] && [ -x /usr/sbin/nginx ] && NGINX_BIN=/usr/sbin/nginx
[ -z "$NGINX_BIN" ] && [ -x /usr/local/nginx/sbin/nginx ] && NGINX_BIN=/usr/local/nginx/sbin/nginx
if [ -z "$NGINX_BIN" ]; then echo "installed=false"; exit 0; fi
NGINX_CONF=$("$NGINX_BIN" -V 2>&1 | tr ' ' '\\n' | grep -oE -- '--conf-path=[^ ]+' | head -1 | cut -d= -f2)
CONF_DIR=$(dirname "$NGINX_CONF")
INCLUDES=$(ls -d "$CONF_DIR/conf.d" "$CONF_DIR/sites-enabled" "$CONF_DIR/http.d" 2>/dev/null | tr '\\n' ',')
echo "installed=true"
echo "binary=$NGINX_BIN"
echo "conf=$NGINX_CONF"
echo "confDir=$CONF_DIR"
echo "includes=$INCLUDES"`

export const emptyNginxLayout = (): NginxLayout => ({
  installed: false,
  binary: '',
  conf: '',
  confDir: '',
  includeDirs: [],
  preferredIncludeDir: '',
})

const parseOutput = (output: string): NginxLayout => {
  const map: Record<string, string> = {}
  for (const line of output.split('\n')) {
    const idx = line.indexOf('=')
    if (idx < 0) continue
    const key = line.slice(0, idx).trim()
    if (key) {
      map[key] = line.slice(idx + 1).trim()
    }
  }
  if (map.installed !== 'true') {
    return emptyNginxLayout()
  }
  const includeDirs = (map.includes || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return {
    installed: true,
    binary: map.binary || '',
    conf: map.conf || '',
    confDir: map.confDir || '',
    includeDirs,
    preferredIncludeDir: includeDirs[0] || (map.confDir ? `${map.confDir}/conf.d` : ''),
  }
}

/**
 * 探测远程主机 nginx 布局；脚本执行失败或 nginx 未安装时返回 installed=false 的空 layout
 */
export const detectNginxLayout = async (sessionId: string): Promise<NginxLayout> => {
  const result = await sshExecCommand(sessionId, DETECT_SCRIPT)
  if (result.exitCode !== 0) {
    return emptyNginxLayout()
  }
  return parseOutput(result.result || '')
}

/**
 * 包装 nginx 子命令：用绝对路径 + `sudo -n` 前缀。
 * apt/yum 装的 nginx master 为 root，普通用户 SSH 时必须 sudo；root 用户 sudo 是 no-op。
 * 自编译跑在普通用户家目录的 nginx，sudo -n 仍能跑(只是绕了一层)，沿用同一前缀简化逻辑。
 */
export const nginxCommand = (binary: string, args: string): string => {
  return `sudo -n ${binary} ${args}`.trim()
}
