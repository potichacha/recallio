import type { RouteObject } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { AuthCallbackPage } from './pages/AuthCallbackPage'
import { HomePage } from './pages/HomePage'
import { ProfilePage } from './pages/ProfilePage'
import { SubjectPage } from './pages/SubjectPage'
import { SubjectsPage } from './pages/SubjectsPage'
import { ThemePage } from './pages/ThemePage'
import { WelcomePage } from './pages/WelcomePage'
import { ReviewPage } from './review/ReviewPage'

/** Routes de l'application, partagées par le routeur du navigateur et celui des tests. */
export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'subjects', element: <SubjectsPage /> },
      { path: 'subjects/:slug', element: <SubjectPage /> },
      { path: 'themes/:slug', element: <ThemePage /> },
      {
        path: 'review',
        element: (
          <RequireAuth>
            <ReviewPage />
          </RequireAuth>
        ),
      },
      {
        path: 'welcome',
        element: (
          <RequireAuth>
            <WelcomePage />
          </RequireAuth>
        ),
      },
      {
        path: 'profile',
        element: (
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        ),
      },
      { path: 'auth/callback', element: <AuthCallbackPage /> },
    ],
  },
]
