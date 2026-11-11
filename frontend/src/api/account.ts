import { useMutation } from '@tanstack/react-query'
import { api } from './client'
import { ApiError } from './errors'

const EXPORT_FILE_NAME = 'recallio-export.json'

/**
 * Télécharge toutes les données de l'utilisateur (ADR 0056). L'appel porte le jeton, ce qu'un simple lien ne ferait
 * pas : le fichier reçu est donc remis au navigateur par un lien temporaire.
 */
export function useAccountExport() {
  return useMutation({
    mutationFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/me/account/export', {
        parseAs: 'blob',
      })
      if (data === undefined) throw ApiError.from(response, error)
      const url = URL.createObjectURL(data)
      const link = document.createElement('a')
      link.href = url
      link.download = EXPORT_FILE_NAME
      link.click()
      URL.revokeObjectURL(url)
    },
  })
}

/** Supprime le compte et ses données, tout de suite et définitivement (ADR 0056). */
export function useDeleteAccount() {
  return useMutation({
    mutationFn: async () => {
      const { error, response } = await api.DELETE('/api/v1/me/account')
      if (!response.ok) throw ApiError.from(response, error)
    },
  })
}
