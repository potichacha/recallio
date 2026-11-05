// Les rappels (ADR 0047), dans le service worker généré par vite-plugin-pwa, qui importe ce fichier.
// Le message arrive chiffré pour cet appareil ; le navigateur le déchiffre avant de le passer ici.
self.addEventListener('push', (event) => {
  const reminder = event.data ? event.data.json() : { title: 'Recallio', body: '', url: '/review' }
  event.waitUntil(
    self.registration.showNotification(reminder.title, {
      body: reminder.body,
      icon: '/favicon.svg',
      tag: 'recallio-reminder',
      data: { url: reminder.url },
    }),
  )
})

// Toucher la notification ouvre l'application sur la page du rappel, ou y ramène un onglet déjà ouvert.
self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = new URL(event.notification.data?.url ?? '/', self.location.origin).href
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
      const open = windows.find((client) => client.url.startsWith(self.location.origin))
      return open
        ? open.navigate(url).then((client) => client?.focus())
        : self.clients.openWindow(url)
    }),
  )
})
