import http from './http'
export function searchMovies(body) {
  return http.post('/movies/search', body).then((r) => r.data)
}

export function deleteMovie(id) {
  return http.delete(`/movies/${id}`).then((r) => r.data)
}

export function updateMovie(id, body) {
  return http.put(`/movies/${id}`, body).then((r) => r.data)
}
export function createMovie(body) {
  return http.post('/movies', body).then((r) => r.data)
}
