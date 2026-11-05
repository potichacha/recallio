/**
 * L'installation de la PWA, proposée au bon moment (ADR 0050). Chrome et Edge annoncent qu'elle est installable
 * (`beforeinstallprompt`) : on garde l'annonce pour la proposer après une session, jamais à la première visite. Safari
 * n'annonce rien : sur iPhone, on explique la marche à suivre.
 */
type InstallPromptEvent = Event & {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

let deferred: InstallPromptEvent | null = null
const listeners = new Set<() => void>()
const notify = () => listeners.forEach((listener) => listener())

/** À appeler une fois au démarrage : le navigateur n'annonce l'installation qu'une fois par page. */
export function listenForInstallPrompt() {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault()
    deferred = event as InstallPromptEvent
    notify()
  })
  window.addEventListener('appinstalled', () => {
    deferred = null
    notify()
  })
}

export function subscribeToInstall(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/** Vrai si le navigateur a annoncé que l'application est installable. */
export const installAvailable = () => deferred !== null

/** Ouvre la fenêtre d'installation du navigateur ; vrai si l'utilisateur a accepté. */
export async function promptInstall(): Promise<boolean> {
  const event = deferred
  if (!event) return false
  deferred = null
  notify()
  await event.prompt()
  return (await event.userChoice).outcome === 'accepted'
}

/** Sur iPhone et iPad, Safari installe depuis le menu Partager ; rien à expliquer une fois l'application installée. */
export function iosInstallable(): boolean {
  const standalone =
    globalThis.matchMedia?.('(display-mode: standalone)').matches === true ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  return !standalone && /iphone|ipad|ipod/i.test(navigator.userAgent)
}

const VISITS_KEY = 'recallio.visits'

/** Compte une visite de plus, au démarrage de l'application. */
export function countVisit() {
  try {
    localStorage.setItem(VISITS_KEY, String(Number(localStorage.getItem(VISITS_KEY) ?? 0) + 1))
  } catch {
    // Stockage indisponible : chaque visite passe pour la première, et l'installation n'est pas proposée.
  }
}

/** Vrai à partir de la deuxième visite : l'installation ne se propose jamais à la première (ADR 0050). */
export function isReturningVisitor(): boolean {
  try {
    return Number(localStorage.getItem(VISITS_KEY) ?? 0) >= 2
  } catch {
    return false
  }
}
