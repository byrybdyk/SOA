import http from './http';

export async function apiLogin(payload) {
  // ожидаем, что backend вернёт { accessToken: "..." }
  const { data } = await http.post('auth/login', payload);
  return data;
}

export async function apiRegister(payload) {
  const { data } = await http.post('auth/register', payload);
  return data;
}
