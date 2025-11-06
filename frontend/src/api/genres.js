import http from './http'

export function redistributeRewards(fromGenre, toGenre) {
  const url = `http://localhost:8081/second-service/api/genres/redistribute-rewards/${encodeURIComponent(fromGenre)}/${encodeURIComponent(toGenre)}`
  return http.post(url).then((r) => r.data) // { transferredCount: number }
}
