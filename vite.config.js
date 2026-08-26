import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import compression from 'vite-plugin-compression2'

export default defineConfig({
  // 部署到子路径（如 /diabetes/）时改为具体路径；默认 './' 相对路径可避免资源 404
  base: './',
  plugins: [
    vue(),
    // 构建时预生成 gzip 压缩包（体积 > 10KB 才压缩，避免小文件开销）
    // 部署时配合 Nginx `gzip_static on;` 直接返回 .gz，无需服务端实时压缩
    compression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240,
      deleteOriginFile: false
    }),
    // 构建时预生成 brotli 压缩包（体积 > 10KB），Node 内置 brotliCompress，无需额外依赖
    // 部署时配合 Nginx `brotli_static on;` 直接返回 .br
    compression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
      deleteOriginFile: false
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: true,
    open: false,
    // 开发环境代理：将 /api 请求转发到 SpringBoot 后端
    // 后端启动后（默认 http://localhost:8080），VITE_USE_MOCK=false 即可走真实接口
    // 允许任意 Host 访问（cpolar 内网穿透域名），Vite 6 需用 true，字符串 'all' 不生效
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
