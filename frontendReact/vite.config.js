import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// REST API (Spring Boot) runs on :8080 ; Socket.IO (netty-socketio) runs on :5000
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/socket.io': { target: 'http://localhost:5000', ws: true }
    }
  }
})
