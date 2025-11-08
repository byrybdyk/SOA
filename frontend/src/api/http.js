import axios from 'axios'

// общие настройки
const common = {
  headers: { 'Content-Type': 'application/json' },
  // withCredentials: true, // если нужны куки — раскомментируй
}

// вспомогательный интерсептор c токеном
function attachAuth(instance) {
  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('access_token')
    const publicPaths = ['auth/login', 'auth/register']

    const isPublic = publicPaths.some((p) => (config.url || '').includes(p))
    if (!isPublic && token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  instance.interceptors.response.use(
    (r) => r,
    (error) => {
      if (error?.response?.status === 401) {
        localStorage.removeItem('access_token')
        if (location.pathname !== '/auth') location.href = '/auth'
      }
      return Promise.reject(error)
    },
  )
  return instance
}

// Клиент для ПЕРВОГО сервиса
export const httpFirst = attachAuth(axios.create({ baseURL: '/first-service/', ...common }))

// Клиент для ВТОРОГО сервиса
export const httpSecond = attachAuth(axios.create({ baseURL: '/second-service/', ...common }))

export default httpFirst
