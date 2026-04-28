import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxies /api to the Spring Boot backend so the browser talks to a
// single origin (cookies + no CORS in dev). Port and proxy target can be
// overridden via env (VITE_PORT / VITE_PROXY_TARGET) when the defaults collide
// with another local app.
export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.VITE_PORT) || 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
