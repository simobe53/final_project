import { RouterProvider } from 'react-router-dom';
import { createRoot } from 'react-dom/client'
import router from './router.jsx';
import App from './App.jsx'
import './index.css'
import './config/axios.js' // axios 전역 설정 로드

createRoot(document.getElementById('root')).render(
  <RouterProvider router={router}>
    <App />
  </RouterProvider>,
)
