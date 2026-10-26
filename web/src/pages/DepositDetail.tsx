import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { ExternalLink, RefreshCw } from 'lucide-react';
import { api } from '../lib/api';
import { useResource } from '../hooks/useResource';
import { StatusBadge } from '../components/StatusBadge';
import {
  Alert,
  Button,
  Card,
  CopyButton,
  ErrorState,
  InfoRow,
  Loading,
} from '../components/ui';
import { formatDateTime, formatRupiah } from '../lib/format';

export default function DepositDetailPage() {
  const { orderNumber } = useParams<{ orderNumber: string }>();
  const navigate = useNavigate();

  const deposit = useResource(() => api.deposits.get(orderNumber ?? ''), [orderNumber]);

  if (!orderNumber) return <Navigate to="/saldo" replace />;

  if (deposit.loading) return <Loading label="Memuat pesanan deposit…" />;
  if (deposit.error) return <ErrorState message={deposit.error} onRetry={deposit.reload} />;
  if (!deposit.data) return <ErrorState message="Pesanan deposit tidak ditemukan." />;

  const data = deposit.data;

  return (
    <div className="space-y-4">
      <header className="space-y-1">
        <h1 className="text-lg font-extrabold tracking-tight text-ink">Detail Deposit</h1>
        <p className="text-xs text-muted">Status pesanan diperbarui dari backend, bukan dari perangkat.</p>
      </header>

      <Card className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs text-muted">Nominal deposit</p>
            <p className="text-2xl font-extrabold tracking-tight text-ink" data-testid="deposit-amount">
              {formatRupiah(data.amount)}
            </p>
          </div>
          <StatusBadge status={data.status} kind="deposit" className="mt-1" />
        </div>

        <div className="rounded-xl bg-canvas px-3 py-2">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[11px] text-muted">Nomor pesanan</p>
              <p className="truncate text-sm font-bold text-ink" data-testid="deposit-order">
                {data.order_number}
              </p>
            </div>
            <CopyButton value={data.order_number} label="Salin" />
          </div>
        </div>

        <div className="divide-y divide-line">
          <InfoRow label="Dibuat" value={formatDateTime(data.created_at)} />
          <InfoRow label="Metode" value={data.payment_method ?? 'Midtrans'} />
          {data.expires_at ? <InfoRow label="Kedaluwarsa" value={formatDateTime(data.expires_at)} /> : null}
          {data.paid_at ? <InfoRow label="Dibayar" value={formatDateTime(data.paid_at)} /> : null}
          {data.va_number ? <InfoRow label="Nomor VA" value={data.va_number} /> : null}
        </div>
      </Card>

      {data.status === 'pending' ? (
        <Card className="space-y-3">
          <p className="text-sm font-bold text-ink">Instruksi pembayaran</p>

          {data.payment_url ? (
            <a
              href={data.payment_url}
              target="_blank"
              rel="noreferrer noopener"
              className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-brand-700 to-brand-500 text-sm font-bold text-white"
            >
              Buka halaman pembayaran Midtrans
              <ExternalLink className="h-4 w-4" aria-hidden />
            </a>
          ) : (
            <Alert tone="warning" title="Instruksi pembayaran belum tersedia">
              Halaman ini akan menampilkan nomor VA / tombol pembayaran Midtrans setelah backend mengirim data
              pembayaran (Tahap 3). Tidak ada instruksi pembayaran yang dibuat di sisi aplikasi.
            </Alert>
          )}

          <p className="text-[11px] leading-relaxed text-muted">
            Setelah pembayaran selesai, status pesanan berubah otomatis ketika backend menerima notifikasi
            Midtrans yang tervalidasi. Tidak ada tombol untuk menandai pesanan sebagai sudah dibayar.
          </p>

          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={deposit.reload}
              icon={<RefreshCw className="h-4 w-4" aria-hidden />}
            >
              Perbarui status
            </Button>
            <Button variant="ghost" onClick={() => navigate('/saldo')}>
              Ke halaman saldo
            </Button>
          </div>
        </Card>
      ) : null}

      {data.status === 'paid' ? (
        <Alert tone="success" title="Deposit berhasil">
          Saldo Anda sudah ditambahkan oleh backend sebesar {formatRupiah(data.amount)}. Perubahan tercatat di
          ledger saldo.
        </Alert>
      ) : null}

      {data.status === 'failed' ? (
        <Alert tone="danger" title="Pembayaran gagal">
          Pembayaran tidak berhasil diselesaikan. Tidak ada saldo yang ditambahkan dan Anda dapat membuat
          pesanan deposit baru.
        </Alert>
      ) : null}

      {data.status === 'expired' ? (
        <Alert tone="warning" title="Pesanan kedaluwarsa">
          Batas waktu pembayaran terlewat, sehingga pesanan ditutup tanpa menambah saldo.
        </Alert>
      ) : null}

      {data.status === 'canceled' ? (
        <Alert tone="warning" title="Pesanan dibatalkan">
          Pesanan ini dibatalkan dan tidak menambah saldo.
        </Alert>
      ) : null}

      {data.status !== 'pending' ? (
        <div className="flex gap-2">
          <Button onClick={() => navigate('/deposit')} className="flex-1">
            Buat deposit baru
          </Button>
          <Button variant="outline" onClick={() => navigate('/saldo')} className="flex-1">
            Riwayat saldo
          </Button>
        </div>
      ) : null}

      <Link to="/deposit" className="block text-center text-xs font-semibold text-brand-600 hover:underline">
        ← Kembali ke halaman isi saldo
      </Link>
    </div>
  );
}
