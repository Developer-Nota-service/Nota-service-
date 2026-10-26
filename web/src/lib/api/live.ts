// Adapter LIVE: memanggil backend Laravel yang sudah ada.
//
// Endpoint yang SUDAH ADA di backend (dipakai langsung di sini):
//   POST /api/v1/register, POST /api/v1/login, GET /api/v1/me, POST /api/v1/logout,
//   POST /api/v1/deposits, GET /api/v1/deposits
//
// Bentuk respons yang SUDAH DIPASTIKAN dari backend (hasil pemeriksaan langsung,
// lihat docs/se-smart-pulsa-integrasi-laravel.md bagian 2):
//   - POST /api/v1/login    -> { "message", "user": {...}, "token": "..." }
//   - GET  /api/v1/me       -> { "user": {...}, "wallet": { "balance_idr": 11000 } }
//   - GET  /api/v1/deposits -> { "deposits": [{ "order_id", "amount_idr", "status", "created_at", "paid_at" }] }
//   - POST /api/v1/deposits menerima { "amount_idr": <integer> }
//   - Tidak ada pembungkus { "data": ... }.
//
// Endpoint yang MASIH PERLU DITAMBAHKAN di backend Laravel (ditandai
// `// PERLU BACKEND` di bawah, daftar lengkap ada di docs/se-smart-pulsa-integrasi-laravel.md).
// Selama belum ada, halaman yang membutuhkannya akan menampilkan pesan error
// yang jelas — bukan data karangan.
//
// Endpoint notifikasi Midtrans (POST /api/v1/payments/midtrans/notification)
// SENGAJA TIDAK pernah dipanggil dari frontend: itu endpoint server-ke-server.

import type {
  Category,
  Deposit,
  LedgerEntry,
  LoginHistoryItem,
  Product,
  PulsaApi,
  SupportConfig,
  Transaction,
  TransactionFilter,
  User,
} from './types';
import { ApiError, isUnauthorized, request, unwrap } from './client';
import { SUPPORT } from '../constants';
import { clearToken, getToken, setToken } from '../session';

const V1 = '/api/v1';

type Raw = Record<string, unknown>;

function asRecord(value: unknown): Raw {
  return (value && typeof value === 'object' ? value : {}) as Raw;
}

function toNumber(value: unknown, fallback = 0): number {
  const parsed = typeof value === 'string' ? Number.parseFloat(value) : value;
  return typeof parsed === 'number' && Number.isFinite(parsed) ? parsed : fallback;
}

function toText(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : value === null || value === undefined ? fallback : String(value);
}

function notImplemented(endpoint: string): never {
  throw new ApiError(
    'not_implemented',
    `Backend Laravel belum menyediakan ${endpoint}. Tambahkan endpoint tersebut (lihat docs/se-smart-pulsa-integrasi-laravel.md) untuk mengaktifkan bagian ini.`,
  );
}

/**
 * Endpoint belum ada di backend: 404 Laravel ("The route api/v1/... could not be
 * found") diterjemahkan menjadi pesan yang jelas untuk pengguna. Error lain
 * (jaringan, 401, 5xx) diteruskan apa adanya.
 */
function missingEndpoint(error: unknown, endpoint: string): never {
  if (error instanceof ApiError && (error.kind === 'not_found' || error.kind === 'not_implemented')) {
    return notImplemented(endpoint);
  }
  throw error;
}

/**
 * Dua bentuk pengguna diterima di sini:
 * - `{ "user": {...}, "wallet": { "balance_idr": ... } }` (GET /api/v1/me)
 * - `{ "user": {...} }` (respons login/register)
 */
function toUser(payload: unknown): User {
  const outer = asRecord(unwrap<unknown>(payload));
  const raw = outer.user ? asRecord(outer.user) : outer;
  const wallet = asRecord(outer.wallet);
  return {
    id: (raw.id as number | string) ?? 0,
    name: toText(raw.name),
    email: toText(raw.email),
    phone: (raw.phone as string | null) ?? null,
    role: raw.role === 'admin' ? 'admin' : 'user',
    // Saldo hanya berasal dari backend; tidak ada perhitungan di sisi frontend.
    balance: toNumber(wallet.balance_idr ?? wallet.balance ?? raw.balance_idr ?? raw.balance ?? raw.saldo),
    created_at: (raw.created_at as string | undefined) ?? undefined,
  };
}

function toDeposit(payload: unknown): Deposit {
  const raw = asRecord(unwrap<unknown>(payload));
  const status = toText(raw.status, 'pending');
  return {
    order_number: toText(raw.order_number ?? raw.order_id),
    amount: toNumber(raw.amount_idr ?? raw.amount ?? raw.gross_amount),
    status: (['pending', 'paid', 'failed', 'expired', 'canceled'].includes(status)
      ? status
      : 'pending') as Deposit['status'],
    payment_method: (raw.payment_method as string | null) ?? null,
    payment_url: (raw.payment_url ?? raw.redirect_url ?? null) as string | null,
    va_number: (raw.va_number as string | null) ?? null,
    expires_at: (raw.expires_at as string | null) ?? null,
    created_at: toText(raw.created_at, new Date().toISOString()),
    paid_at: (raw.paid_at as string | null) ?? null,
  };
}

function toCategory(payload: unknown): Category {
  const raw = asRecord(payload);
  const destination = asRecord(raw.destination);
  return {
    slug: toText(raw.slug),
    name: toText(raw.name),
    description: toText(raw.description),
    icon: toText(raw.icon, 'smartphone'),
    demo: raw.demo === true,
    destination: {
      label: toText(destination.label, 'Nomor tujuan'),
      placeholder: toText(destination.placeholder),
      hint: destination.hint ? toText(destination.hint) : undefined,
      pattern: destination.pattern ? toText(destination.pattern) : undefined,
      pattern_message: destination.pattern_message ? toText(destination.pattern_message) : undefined,
      optional: destination.optional === true,
    },
  };
}

function toProduct(payload: unknown): Product {
  const raw = asRecord(payload);
  return {
    id: toText(raw.id ?? raw.sku_code),
    category_slug: toText(raw.category_slug ?? asRecord(raw.category).slug),
    name: toText(raw.name),
    provider: toText(raw.provider),
    sku_code: toText(raw.sku_code),
    price: toNumber(raw.price),
    admin_fee: toNumber(raw.admin_fee ?? raw.fee),
    popular: raw.popular === true,
    description: raw.description ? toText(raw.description) : undefined,
    demo: raw.demo !== false,
  };
}

function toTransaction(payload: unknown): Transaction {
  const raw = asRecord(unwrap<unknown>(payload));
  const status = toText(raw.status, 'pending');
  return {
    order_number: toText(raw.order_number ?? raw.order_id),
    category_slug: toText(raw.category_slug),
    category_name: toText(raw.category_name, toText(raw.category_slug)),
    product_id: toText(raw.product_id),
    product_name: toText(raw.product_name ?? raw.name),
    provider: (raw.provider as string | null) ?? null,
    destination: toText(raw.destination ?? raw.customer_number),
    server_id: (raw.server_id as string | null) ?? null,
    amount: toNumber(raw.amount ?? raw.price),
    admin_fee: toNumber(raw.admin_fee),
    total: toNumber(raw.total ?? raw.amount ?? raw.price),
    status: (['pending', 'processing', 'success', 'failed'].includes(status)
      ? status
      : 'pending') as Transaction['status'],
    note: (raw.note ?? raw.message ?? null) as string | null,
    serial: (raw.serial ?? raw.sn ?? null) as string | null,
    created_at: toText(raw.created_at, new Date().toISOString()),
    updated_at: (raw.updated_at as string | null) ?? null,
    demo: raw.demo === true,
  };
}

function toList<T>(payload: unknown, mapper: (item: unknown) => T): T[] {
  const data = unwrap<unknown>(payload);
  if (Array.isArray(data)) return data.map(mapper);
  const record = asRecord(data);
  if (Array.isArray(record.data)) return record.data.map(mapper);
  // Laravel membungkus koleksi dengan nama resource, mis. { "deposits": [...] }
  // (bentuk yang dipakai GET /api/v1/deposits pada backend saat ini).
  const collection = Object.values(record).find((value) => Array.isArray(value));
  if (Array.isArray(collection)) return collection.map(mapper);
  return [];
}

/** Token bisa bernama `token` atau `access_token` tergantung implementasi backend. */
function extractToken(payload: unknown): string | null {
  const raw = asRecord(unwrap<unknown>(payload));
  const token = raw.token ?? raw.access_token;
  return typeof token === 'string' && token ? token : null;
}

/**
 * Setelah login/register, data pengguna yang lengkap diambil dari GET /api/v1/me:
 * respons login backend saat ini hanya berisi `{user, token}` tanpa saldo, sedangkan
 * kartu saldo membutuhkan `wallet.balance_idr`.
 */
async function sessionUser(loginPayload: unknown): Promise<User> {
  const fallback = asRecord(unwrap<unknown>(loginPayload)).user;
  try {
    return toUser(await request<unknown>(`${V1}/me`));
  } catch (error) {
    // /me gagal: tetap masuk dengan data dari respons login, jangan gagalkan login.
    if (fallback) return toUser(fallback);
    throw error;
  }
}

export const liveApi: PulsaApi = {
  mode: 'live',

  auth: {
    async login(input): Promise<User> {
      const payload = await request<unknown>(`${V1}/login`, {
        method: 'POST',
        auth: false,
        body: { email: input.email, password: input.password },
      });
      const token = extractToken(payload);
      if (token) setToken(token);
      return sessionUser(payload);
    },

    async register(input): Promise<User> {
      const payload = await request<unknown>(`${V1}/register`, {
        method: 'POST',
        auth: false,
        body: {
          name: input.name,
          email: input.email,
          phone: input.phone,
          password: input.password,
          password_confirmation: input.password_confirmation,
        },
      });
      const token = extractToken(payload);
      if (token) setToken(token);
      return sessionUser(payload);
    },

    async current(): Promise<User | null> {
      // Tanpa token kita tetap mencoba /me karena backend bisa memakai cookie sesi.
      try {
        return toUser(await request<unknown>(`${V1}/me`));
      } catch (error) {
        if (isUnauthorized(error)) return null;
        throw error;
      }
    },

    async refresh(): Promise<User> {
      return toUser(await request<unknown>(`${V1}/me`));
    },

    async logout(): Promise<void> {
      try {
        await request<unknown>(`${V1}/logout`, { method: 'POST' });
      } finally {
        clearToken();
      }
    },

    // PERLU BACKEND: riwayat login (opsional — UI menampilkan pesan bila belum ada).
    async loginHistory(): Promise<LoginHistoryItem[]> {
      return notImplemented('endpoint riwayat login (mis. GET /api/v1/login-history)');
    },
  },

  wallet: {
    // Saldo diambil dari /me (endpoint yang sudah ada). Frontend tidak pernah
    // menghitung atau menetapkan saldo sendiri.
    async summary() {
      const user = toUser(await request<unknown>(`${V1}/me`));
      return { balance: user.balance };
    },

    // PERLU BACKEND: GET /api/v1/wallet/ledger
    async ledger(): Promise<LedgerEntry[]> {
      return notImplemented('endpoint ledger saldo (mis. GET /api/v1/wallet/ledger)');
    },
  },

  deposits: {
    // Backend menerima nominal pada field `amount_idr` (dikonfirmasi dari respons
    // validasi 422: "The amount idr field is required.").
    async create(amount: number): Promise<Deposit> {
      const payload = await request<unknown>(`${V1}/deposits`, {
        method: 'POST',
        body: { amount_idr: amount },
      });
      return toDeposit(payload);
    },

    async list(): Promise<Deposit[]> {
      return toList(await request<unknown>(`${V1}/deposits`), toDeposit);
    },

    // PERLU BACKEND: GET /api/v1/deposits/{order_number}
    // Selagi belum ada, detail diambil dari daftar deposit (data backend, bukan karangan).
    async get(orderNumber: string): Promise<Deposit> {
      try {
        const payload = await request<unknown>(`${V1}/deposits/${encodeURIComponent(orderNumber)}`);
        return toDeposit(payload);
      } catch (error) {
        if (error instanceof ApiError && error.kind === 'not_found') {
          const found = (await liveApi.deposits.list()).find(
            (item) => item.order_number === orderNumber,
          );
          if (found) return found;
          throw new ApiError('not_found', 'Pesanan deposit tidak ditemukan.', 404);
        }
        throw error;
      }
    },
  },

  catalog: {
    // PERLU BACKEND: GET /api/v1/categories
    async categories(): Promise<Category[]> {
      return notImplemented('endpoint katalog (mis. GET /api/v1/categories)');
    },

    // PERLU BACKEND: GET /api/v1/products?category={slug}
    async products(categorySlug: string): Promise<Product[]> {
      try {
        const payload = await request<unknown>(
          `${V1}/products?category=${encodeURIComponent(categorySlug)}`,
        );
        return toList(payload, toProduct);
      } catch (error) {
        return missingEndpoint(error, `endpoint katalog produk (GET ${V1}/products)`);
      }
    },

    // PERLU BACKEND: GET /api/v1/products?popular=1
    async popularProducts(): Promise<Product[]> {
      try {
        const payload = await request<unknown>(`${V1}/products?popular=1`);
        return toList(payload, toProduct);
      } catch (error) {
        return missingEndpoint(error, `endpoint katalog produk (GET ${V1}/products)`);
      }
    },

    // PERLU BACKEND: GET /api/v1/products/{id}
    async product(productId: string): Promise<Product> {
      try {
        return toProduct(await request<unknown>(`${V1}/products/${encodeURIComponent(productId)}`));
      } catch (error) {
        return missingEndpoint(error, `endpoint detail produk (GET ${V1}/products/{id})`);
      }
    },
  },

  transactions: {
    // PERLU BACKEND: POST /api/v1/transactions (atomik: validasi saldo + potong +
    // ledger + idempotency key di sisi backend).
    async purchase(input) {
      try {
        const payload = await request<unknown>(`${V1}/transactions`, {
          method: 'POST',
          headers: { 'Idempotency-Key': input.idempotency_key },
          body: {
            product_id: input.product_id,
            destination: input.destination,
            server_id: input.server_id,
            idempotency_key: input.idempotency_key,
          },
        });
        return toTransaction(payload);
      } catch (error) {
        return missingEndpoint(error, `endpoint pembelian (POST ${V1}/transactions)`);
      }
    },

    // PERLU BACKEND: GET /api/v1/transactions?status=&category=&from=&to=
    async list(filter?: TransactionFilter): Promise<Transaction[]> {
      const params = new URLSearchParams();
      if (filter?.status && filter.status !== 'all') params.set('status', filter.status);
      if (filter?.category && filter.category !== 'all') params.set('category', filter.category);
      if (filter?.from) params.set('from', filter.from);
      if (filter?.to) params.set('to', filter.to);
      const query = params.toString();
      try {
        const payload = await request<unknown>(`${V1}/transactions${query ? `?${query}` : ''}`);
        return toList(payload, toTransaction);
      } catch (error) {
        return missingEndpoint(error, `endpoint riwayat transaksi (GET ${V1}/transactions)`);
      }
    },

    // PERLU BACKEND: GET /api/v1/transactions/{order_number}
    async get(orderNumber: string): Promise<Transaction> {
      try {
        const payload = await request<unknown>(`${V1}/transactions/${encodeURIComponent(orderNumber)}`);
        return toTransaction(payload);
      } catch (error) {
        return missingEndpoint(error, `endpoint detail transaksi (GET ${V1}/transactions/{order})`);
      }
    },
  },

  support: {
    // PERLU BACKEND (opsional): GET /api/v1/settings/support — bila belum ada,
    // kontak dukungan memakai konfigurasi VITE_SUPPORT_*.
    async config(): Promise<SupportConfig> {
      try {
        const payload = await request<unknown>(`${V1}/settings/support`);
        const raw = asRecord(unwrap<unknown>(payload));
        return {
          whatsapp: toText(raw.whatsapp, SUPPORT.whatsapp),
          whatsapp_label: toText(raw.whatsapp_label, SUPPORT.whatsapp_label),
          email: toText(raw.email, SUPPORT.email),
          hours: toText(raw.hours, SUPPORT.hours),
          address: toText(raw.address, SUPPORT.address),
          note: SUPPORT.note,
        };
      } catch (error) {
        if (error instanceof ApiError && error.kind === 'not_implemented') return SUPPORT;
        if (error instanceof ApiError && (error.kind === 'not_found' || error.kind === 'server')) return SUPPORT;
        throw error;
      }
    },
  },
};
