import axios from 'axios'

const http = axios.create({
  baseURL: 'https://144.31.193.121:8443/first-service/',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')

  // список путей, где токен не нужен
  const publicPaths = ['auth/login', 'auth/register']

  // если не публичный путь — добавить Bearer
  const isPublic = publicPaths.some((path) => config.url?.includes(path))
  if (!isPublic && token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('access_token')
      if (location.pathname !== '/auth') location.href = '/auth'
    }
    return Promise.reject(error)
  },
)

export default http
