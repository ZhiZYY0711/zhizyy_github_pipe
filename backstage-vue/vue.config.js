const { defineConfig } = require('@vue/cli-service')
const path = require('path')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false, // 关闭eslint语法检查

  configureWebpack: {
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        '@modules': path.resolve(__dirname, 'src/modules'),
        '@shared': path.resolve(__dirname, 'src/modules/shared'),
        '@auth': path.resolve(__dirname, 'src/modules/auth'),
        '@dashboard': path.resolve(__dirname, 'src/modules/dashboard'),
        '@monitoring': path.resolve(__dirname, 'src/modules/monitoring'),
        '@management': path.resolve(__dirname, 'src/modules/management'),
        '@emergency': path.resolve(__dirname, 'src/modules/emergency'),
        '@logs': path.resolve(__dirname, 'src/modules/logs')
      }
    }
  },

  devServer: {
    port: 8081,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''
        }
      },
      '/sse': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/sse': ''
        }
      }
    }
  }
})
