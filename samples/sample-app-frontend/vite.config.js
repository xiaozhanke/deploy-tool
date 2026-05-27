import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// deploy-tool 把 dist 解压到 nginx 站点目录,生产路径可能不是站点根,这里用相对路径
export default defineConfig({
    plugins: [vue()],
    base: './',
    build: {
        outDir: 'dist',
        emptyOutDir: true,
    },
})
