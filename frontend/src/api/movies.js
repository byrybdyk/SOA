import http from './http'
export function searchMovies(body) {
  return http.post('/movies/search', body).then((r) => r.data)
}
