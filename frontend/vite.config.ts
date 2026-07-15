import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/user-service': {
        target: process.env.VITE_USER_SERVICE_URL ?? 'http://127.0.0.1:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/user-service/, ''),
      },
      '/learning-service': {
        target: process.env.VITE_LEARNING_SERVICE_URL ?? 'http://127.0.0.1:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/learning-service/, ''),
      },
      '/progress-service': {
        target: process.env.VITE_PROGRESS_SERVICE_URL ?? 'http://127.0.0.1:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/progress-service/, ''),
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
  },
})
