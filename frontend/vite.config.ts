import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { appendFileSync, mkdirSync } from 'node:fs'
import { resolve } from 'node:path'

const logBaseDir = resolve(__dirname, '../.logs')

function writeClientLog(rawBody: string) {
  const today = new Date().toISOString().slice(0, 10)
  const logDir = resolve(logBaseDir, today)
  mkdirSync(logDir, { recursive: true })
  const logFile = resolve(logDir, 'frontend-client.log')
  appendFileSync(logFile, `${new Date().toISOString()} ${rawBody}\n`, 'utf-8')
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'pipeline-client-log-writer',
      configureServer(server) {
        server.middlewares.use('/__client-log', (request, response) => {
          if (request.method !== 'POST') {
            response.statusCode = 405
            response.end()
            return
          }

          let body = ''
          request.setEncoding('utf-8')
          request.on('data', (chunk) => {
            body += chunk
          })
          request.on('end', () => {
            writeClientLog(body)
            response.statusCode = 204
            response.end()
          })
        })
      },
    },
  ],
  server: {
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
