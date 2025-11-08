import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import fs from 'fs'
import path from 'path'

export default defineConfig(({ command }) => {
  const isDev = command === 'serve'

  return {
    plugins: [vue(), vueDevTools()],
    server: isDev
      ? {
          https: {
            key: fs.readFileSync(path.resolve(__dirname, '../tls/frontend/frontend.key')),
            cert: fs.readFileSync(
              path.resolve(__dirname, '../tls/frontend/frontend-fullchain.crt'),
            ),
          },
          host: '0.0.0.0',
          port: 5173,
          strictPort: true,
        }
      : undefined,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  }
})
