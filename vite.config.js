import { defineConfig } from 'vite';
import { fileURLToPath } from 'url';
import path from 'path';
import react from '@vitejs/plugin-react';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],     // React 플러그인 적용
  base:'/',       //배포하려는 서버 상황에 맞게 절대 경로로 설정(기본값:/)
  server: {
    port: 5173,   // 개발 서버 포트 설정 (기본값: 5173)
    open: true,   // 개발 서버 실행 시 자동으로 브라우저 열기 (디폴트: false)
    proxy: {
      '/api': {   /* api call(REST API)는 로컬에서도 무조건 8080 포트를 탄다 */
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        ws: true
      }
    }
  },
  build: {
    outDir: 'springboot/src/main/resources/static',   // 빌드 결과물 폴더 (CI/CD용)
  },
  resolve: {
    alias: {
      '/pages': path.resolve(__dirname, 'src/pages'),
      '/components': path.resolve(__dirname, 'src/components'),
      '/config': path.resolve(__dirname, 'src/config'),
      '/services': path.resolve(__dirname, 'src/services'),
      '/context': path.resolve(__dirname, 'src/context'),
      '/assets': path.resolve(__dirname, 'public/assets'),
      // '/': path.resolve(__dirname, 'src/'),
    }
  }   
});