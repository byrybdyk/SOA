import { httpSecond } from './http'

export function redistributeRewards(fromGenre, toGenre) {
  const url = `api/genres/redistribute-rewards/${encodeURIComponent(
    fromGenre,
  )}/${encodeURIComponent(toGenre)}`

  return httpSecond.post(url).then((r) => r.data)
}
