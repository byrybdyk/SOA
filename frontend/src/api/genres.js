import { httpSecond } from './http'

export function redistributeRewards(fromGenre, toGenre) {
  // ВАЖНО: путь БЕЗ начального слэша, так как baseURL уже '/second-service/'
  const url = `api/genres/redistribute-rewards/${encodeURIComponent(
    fromGenre,
  )}/${encodeURIComponent(toGenre)}`

  return httpSecond.post(url).then((r) => r.data)
}
