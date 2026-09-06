import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// API target: 本机 Docker 后端默认 8081;本机开发后端为 8080
// 可通过环境变量覆盖: VITE_API_TARGET=http://localhost:8080 npm run dev
const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:8081'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: true },
      '/media': { target: apiTarget, changeOrigin: true },
    },
  },
})
