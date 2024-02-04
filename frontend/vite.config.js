import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'fs'

export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server:{
    //hmr:false,
    cors: "*",
    https: {
      key: fs.readFileSync('./key/rtctest2.pem'),
      cert: fs.readFileSync('./key/rtccert.pem'),
      handshakeTimeout: 300000,
    },
    proxy:{
      '/ws/room':{
        target:'https://localhost:8081',
        ws:true,
        changeOrigin:true,
      }
    }
  }
})
