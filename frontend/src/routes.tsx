import type { RouteObject } from 'react-router'
import { Layout } from './components/Layout'
import { HomePage } from './pages/HomePage'
import { ThemePage } from './pages/ThemePage'
import { ThemesPage } from './pages/ThemesPage'

/** Routes de l'application, partagées par le routeur du navigateur et celui des tests. */
export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'themes', element: <ThemesPage /> },
      { path: 'themes/:slug', element: <ThemePage /> },
    ],
  },
]
