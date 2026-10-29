import { NavLink, Outlet } from 'react-router'

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'font-semibold text-indigo-600' : 'text-slate-600 hover:text-indigo-600'
}

export function Layout() {
  return (
    <div className="min-h-dvh bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-3xl items-center gap-6 px-4 py-3">
          <NavLink to="/" className="text-lg font-bold text-indigo-600">
            Recallio
          </NavLink>
          <NavLink to="/themes" className={navClass}>
            Thèmes
          </NavLink>
        </nav>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
