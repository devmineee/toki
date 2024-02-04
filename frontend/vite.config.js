import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

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
    cors:"*",
    // proxy:{
    //   '/ws/room':{
    //     target:'wss://api1:443',
    //     ws:true,
    //     secure:false,
    //     changeOrigin:true,
    //   }
    // }
  }
})
