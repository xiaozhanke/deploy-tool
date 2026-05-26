import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 源码里 ElNotification 等依赖 AutoImport 隐式注入，vitest 也必须经过同一条 transform 链
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: [
        'vue',
        {
          '@/utils/errorMessage': ['extractErrorMessage'],
        },
      ],
      // importStyle 必须为 false：测试 css: false，开 sass 注入会让 Node 端 require .scss 抛 "Unknown file extension"
      resolvers: [ElementPlusResolver({ importStyle: false })],
      // dts 由 vite.config.ts 单独维护，避免与 dev server 并行运行时竞争写入 types/auto-imports.d.ts
      dts: false,
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{spec,test}.ts', 'src/__tests__/**/*.{spec,test}.ts'],
    setupFiles: ['./src/__tests__/setup.ts'],
    css: false,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: ['src/**/*.spec.ts', 'src/**/*.test.ts', 'src/__tests__/**'],
    },
  },
})
