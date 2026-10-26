import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ChevronRight,
  FileText,
  HelpCircle,
  LogOut,
  ShieldCheck,
  ScrollText,
  UserCog,
} from 'lucide-react';
import { useAuth } from '../state/AuthContext';
import { API_BASE_URL, API_MODE } from '../lib/api';
import { Alert, Button, Card, Dialog, InfoRow, SectionTitle } from '../components/ui';
import { formatDate, initials } from '../lib/format';

const MENU = [
  { to: '/profil/riwayat-login', label: 'Riwayat login', icon: ScrollText },
  { to: '/bantuan', label: 'Pusat bantuan', icon: HelpCircle },
  { to: '/syarat', label: 'Syarat penggunaan', icon: FileText },
  { to: '/privasi', label: 'Kebijakan privasi', icon: ShieldCheck },
];

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [leaving, setLeaving] = useState(false);

  async function handleLogout() {
    setLeaving(true);
    await logout();
    setLeaving(false);
    setConfirmOpen(false);
    navigate('/masuk', { replace: true });
  }

  return (
    <div className="space-y-5">
      <header className="space-y-1">
        <h1 className="text-lg font-extrabold tracking-tight text-ink">Profil</h1>
        <p className="text-xs text-muted">Data akun dan pengaturan aplikasi.</p>
      </header>

      <Card className="space-y-4">
        <div className="flex items-center gap-3">
          <span
            className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-gradient-to-br from-brand-800 to-brand-500 text-lg font-extrabold text-white"
            aria-hidden
          >
            {initials(user?.name ?? 'Pengguna')}
          </span>
          <div className="min-w-0">
            <p className="truncate text-base font-bold text-ink" data-testid="profile-name">
              {user?.name ?? '—'}
            </p>
            <p className="truncate text-xs text-muted">{user?.email ?? '—'}</p>
            <span className="mt-1 inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-brand-700">
              {user?.role === 'admin' ? 'Administrator' : 'Pengguna'}
            </span>
          </div>
        </div>

        <div className="divide-y divide-line">
          <InfoRow label="Nomor HP" value={user?.phone ?? 'Belum diisi'} />
          <InfoRow label="Terdaftar sejak" value={formatDate(user?.created_at)} />
          <InfoRow label="Sumber data" value={API_MODE === 'demo' ? 'DEMO (contoh)' : 'Backend Laravel'} />
        </div>
      </Card>

      <Card className="space-y-3">
        <SectionTitle title="Pengaturan akun" className="mb-0" />
        <Alert tone="info">
          Perubahan nama, nomor HP, dan password memerlukan endpoint pengelolaan akun di backend Laravel
          (belum tersedia). Saat ini data akun hanya dapat dilihat.
        </Alert>
        <Button variant="outline" block disabled icon={<UserCog className="h-4 w-4" aria-hidden />}>
          Ubah data akun (Tahap 2)
        </Button>
      </Card>

      <nav className="overflow-hidden rounded-2xl border border-line bg-white">
        {MENU.map((item, index) => (
          <Link
            key={item.to}
            to={item.to}
            className={
              index === 0
                ? 'flex items-center gap-3 px-4 py-3 text-sm font-semibold text-ink transition hover:bg-canvas'
                : 'flex items-center gap-3 border-t border-line px-4 py-3 text-sm font-semibold text-ink transition hover:bg-canvas'
            }
          >
            <item.icon className="h-4 w-4 text-brand-600" aria-hidden />
            {item.label}
            <ChevronRight className="ml-auto h-4 w-4 text-muted" aria-hidden />
          </Link>
        ))}
      </nav>

      {API_MODE === 'live' ? (
        <p className="break-all text-[11px] text-muted">Terhubung ke backend: {API_BASE_URL}</p>
      ) : null}

      <Button
        variant="danger"
        block
        icon={<LogOut className="h-4 w-4" aria-hidden />}
        onClick={() => setConfirmOpen(true)}
      >
        Keluar dari akun
      </Button>

      <Dialog
        open={confirmOpen}
        title="Keluar dari akun?"
        description="Anda perlu masuk kembali untuk melihat saldo dan melakukan transaksi."
        onClose={() => setConfirmOpen(false)}
      >
        <div className="flex gap-2">
          <Button variant="outline" className="flex-1" onClick={() => setConfirmOpen(false)}>
            Batal
          </Button>
          <Button variant="danger" className="flex-1" loading={leaving} onClick={handleLogout}>
            Keluar
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
