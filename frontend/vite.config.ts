import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
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
      '/genai-service': {
        target: process.env.VITE_GENAI_SERVICE_URL ?? 'http://127.0.0.1:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/genai-service/, ''),
      },
    },
  },
})
