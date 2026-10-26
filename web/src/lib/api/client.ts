// Klien HTTP terpusat untuk backend Laravel SE SMART PULSA.
//
// Semua panggilan API melewati `request()` supaya alamat backend, autentikasi,
// timeout, dan penanganan error hanya dikelola di satu tempat.
//
// ATURAN KEAMANAN YANG DIPATUHI DI SINI:
// - Server Key Midtrans / secret backend TIDAK pernah ada di frontend. Frontend
//   hanya memanggil endpoint backend; endpoint notifikasi Midtrans
//   (POST /api/v1/payments/midtrans/notification) tidak pernah dipanggil dari sini.
// - Nominal, harga, dan status pembayaran yang dikirim ke backend hanya berupa
//   permintaan; keputusan akhir tetap milik backend.

import { getToken, notifyUnauthorized } from '../session';

const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').trim();

/** Base URL backend, tanpa garis miring di akhir. Kosong = mode DEMO. */
export const API_BASE_URL = rawBaseUrl.replace(/\/+$/, '');

/** true bila aplikasi sudah diarahkan ke backend Laravel (Tahap 2 dan seterusnya). */
export const IS_LIVE_MODE = API_BASE_URL !== '';

const DEFAULT_TIMEOUT_MS = 15000;

/**
 * Cookie sesi hanya berguna bila backend berada di origin yang sama dengan halaman
 * ini. Untuk backend di origin lain (mis. https://api.sesmartpulsa.id atau tunnel
 * HTTPS), kredensial tidak dikirim — browser menolak respons CORS bila
 * `Access-Control-Allow-Origin` bernilai `*` sementara permintaan memakai
 * kredensial. Autentikasi tetap memakai header Authorization: Bearer <token>.
 *
 * Bila backend ingin memakai cookie lintas origin, backend harus mengirim
 * `Access-Control-Allow-Credentials: true` dengan origin eksplisit (bukan `*`).
 */
function useBrowserCredentials(baseUrl: string): boolean {
  if (!baseUrl) return true;
  try {
    return new URL(baseUrl, window.location.href).origin === window.location.origin;
  } catch {
    return false;
  }
}

export type ApiErrorKind =
  | 'network'
  | 'timeout'
  | 'unauthorized'
  | 'validation'
  | 'not_found'
  | 'conflict'
  | 'server'
  | 'not_implemented';

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number;
  /** Error validasi Laravel: { email: ["Email sudah dipakai."] } */
  readonly errors: Record<string, string[]>;

  constructor(
    kind: ApiErrorKind,
    message: string,
    status = 0,
    errors: Record<string, string[]> = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.kind = kind;
    this.status = status;
    this.errors = errors;
  }
}

function asErrorMap(payload: unknown): Record<string, string[]> {
  if (!payload || typeof payload !== 'object') return {};
  const raw = (payload as { errors?: unknown }).errors;
  if (!raw || typeof raw !== 'object') return {};
  const result: Record<string, string[]> = {};
  for (const [field, value] of Object.entries(raw as Record<string, unknown>)) {
    if (Array.isArray(value)) result[field] = value.map(String);
    else if (typeof value === 'string') result[field] = [value];
  }
  return result;
}

function messageFromPayload(payload: unknown, fallback: string): string {
  if (typeof payload === 'string' && payload.trim()) return payload.trim();
  if (payload && typeof payload === 'object') {
    const message = (payload as { message?: unknown }).message;
    if (typeof message === 'string' && message.trim()) return message.trim();
  }
  return fallback;
}

function toApiError(status: number, payload: unknown): ApiError {
  const errors = asErrorMap(payload);
  const firstFieldError = Object.values(errors)[0]?.[0];

  if (status === 401) {
    return new ApiError(
      'unauthorized',
      messageFromPayload(payload, 'Sesi Anda sudah berakhir. Silakan masuk kembali.'),
      status,
      errors,
    );
  }
  if (status === 404) {
    return new ApiError('not_found', messageFromPayload(payload, 'Data tidak ditemukan.'), status, errors);
  }
  if (status === 409) {
    return new ApiError(
      'conflict',
      messageFromPayload(payload, 'Permintaan ini sudah diproses sebelumnya.'),
      status,
      errors,
    );
  }
  if (status === 422 || status === 400) {
    return new ApiError(
      'validation',
      messageFromPayload(payload, firstFieldError ?? 'Data yang dikirim tidak valid.'),
      status,
      errors,
    );
  }
  if (status >= 500) {
    return new ApiError(
      'server',
      messageFromPayload(payload, 'Server sedang bermasalah. Silakan coba beberapa saat lagi.'),
      status,
      errors,
    );
  }
  return new ApiError('server', messageFromPayload(payload, `Permintaan gagal (${status}).`), status, errors);
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  /** false untuk endpoint publik (login/register). */
  auth?: boolean;
  timeoutMs?: number;
  /** Header tambahan, mis. `Idempotency-Key` untuk pembelian. */
  headers?: Record<string, string>;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const url = `${API_BASE_URL}${path}`;
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), options.timeoutMs ?? DEFAULT_TIMEOUT_MS);

  const headers: Record<string, string> = { Accept: 'application/json', ...options.headers };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';

  const token = getToken();
  if (options.auth !== false && token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      // Token Bearer selalu dikirim; cookie sesi (Sanctum stateful) hanya dipakai
      // bila backend se-origin dengan halaman ini.
      credentials: useBrowserCredentials(API_BASE_URL) ? 'include' : 'omit',
      signal: controller.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError('timeout', 'Permintaan melebihi batas waktu. Silakan coba lagi.');
    }
    throw new ApiError(
      'network',
      'Tidak dapat terhubung ke server. Periksa koneksi internet Anda lalu coba lagi.',
    );
  } finally {
    window.clearTimeout(timeout);
  }

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  let payload: unknown;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!response.ok) {
    const error = toApiError(response.status, payload);
    if (error.kind === 'unauthorized') notifyUnauthorized();
    throw error;
  }

  return payload as T;
}

/**
 * Laravel sering membungkus respons dengan `{ "data": ... }`.
 * Helper ini menerima dua bentuk agar integrasi Tahap 2 tidak rapuh.
 */
export function unwrap<T>(payload: unknown): T {
  if (payload && typeof payload === 'object' && 'data' in (payload as Record<string, unknown>)) {
    return (payload as { data: T }).data;
  }
  return payload as T;
}

/** Pesan siap tampil untuk sembarang error. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return 'Terjadi kesalahan yang tidak diketahui. Silakan coba lagi.';
}

/** Ambil pesan validasi untuk satu field (bila ada). */
export function fieldError(error: unknown, field: string): string | null {
  if (error instanceof ApiError) return error.errors[field]?.[0] ?? null;
  return null;
}

export function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.kind === 'unauthorized';
}
