import axios from 'axios';
import type { Result } from '../types';

const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

export async function fetchJson<T>(path: string): Promise<T> {
  const response = await client.get<Result<T>>(path);
  if (response.data.code !== 200) {
    throw new Error(response.data.message);
  }
  return response.data.data;
}

export async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await client.post<Result<T>>(path, body);
  if (response.data.code !== 200) {
    throw new Error(response.data.message);
  }
  return response.data.data;
}

export async function putJson<T>(path: string, body: unknown): Promise<T> {
  const response = await client.put<Result<T>>(path, body);
  if (response.data.code !== 200) {
    throw new Error(response.data.message);
  }
  return response.data.data;
}

export async function deleteJson<T>(path: string): Promise<T> {
  const response = await client.delete<Result<T>>(path);
  if (response.data.code !== 200) {
    throw new Error(response.data.message);
  }
  return response.data.data;
}

export default client;
