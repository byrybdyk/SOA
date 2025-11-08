import http from './http'

export function redistributeRewards(fromGenre, toGenre) {
  // было: https://144.31.193.121:8444/second-service/...
  const url = `/second-service/api/genres/redistribute-rewards/${encodeURIComponent(
    fromGenre,
  )}/${encodeURIComponent(toGenre)}`
  return http.post(url).then((r) => r.data) // { transferredCount: number }
}
