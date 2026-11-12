// Pose le thème sur la page avant son premier affichage (ADR 0058) : sans cela, qui a choisi le thème sombre verrait un
// éclair de thème clair à chaque ouverture. Un fichier, et non un script dans la page : la Content-Security-Policy
// n'admet aucun script en ligne (ADR 0054). La même règle est dans src/theme/theme.ts, pour le choix fait dans le profil.
;(function () {
  var choice = null
  try {
    choice = localStorage.getItem('recallio.theme')
  } catch (error) {
    // Stockage refusé (navigation privée stricte) : le thème du système.
  }
  var dark =
    choice === 'dark' ||
    (choice !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
})()
