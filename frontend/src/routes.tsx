import type { RouteObject } from 'react-router'
import { ContentReportsPage } from './admin/ContentReportsPage'
import { RetentionPage } from './admin/RetentionPage'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { GroupPage } from './groups/GroupPage'
import { GroupsPage } from './groups/GroupsPage'
import { JoinPage } from './groups/JoinPage'
import { LivePage } from './live/LivePage'
import { ChallengePage } from './challenges/ChallengePage'
import { MockExamPage } from './exam/MockExamPage'
import { AuthCallbackPage } from './pages/AuthCallbackPage'
import { HomePage } from './pages/HomePage'
import { LearningPathPage } from './pages/LearningPathPage'
import { ProfilePage } from './pages/ProfilePage'
import { PublicProfilePage } from './pages/PublicProfilePage'
import { SubjectPage } from './pages/SubjectPage'
import { SubjectsPage } from './pages/SubjectsPage'
import { ThemePage } from './pages/ThemePage'
import { TrialPage } from './pages/TrialPage'
import { WelcomePage } from './pages/WelcomePage'
import { PlayPage } from './play/PlayPage'
import { PracticePage } from './practice/PracticePage'
import { DuelPage } from './ranked/DuelPage'
import { RankedPage } from './ranked/RankedPage'
import { ReviewPage } from './review/ReviewPage'
import { CoursesPage } from './subjects/CoursesPage'
import { LessonPage } from './subjects/LessonPage'
import { PathToCourse } from './subjects/PathToCourse'
import { ToursPage } from './subjects/ToursPage'
import { TourNotionPage } from './tour/TourNotionPage'
import { TourOutlinePage } from './tour/TourOutlinePage'

/** Routes de l'application, partagées par le routeur du navigateur et celui des tests. */
export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'learn', element: <CoursesPage /> },
      { path: 'tours', element: <ToursPage /> },
      { path: 'play', element: <PlayPage /> },
      { path: 'subjects', element: <SubjectsPage /> },
      { path: 'subjects/:slug', element: <SubjectPage /> },
      {
        path: 'subjects/:slug/course',
        element: (
          <RequireAuth>
            <LearningPathPage />
          </RequireAuth>
        ),
      },
      { path: 'subjects/:slug/course/:notion', element: <LessonPage /> },
      { path: 'subjects/:slug/path', element: <PathToCourse /> },
      { path: 'themes/:slug', element: <ThemePage /> },
      // Le Tour se lit sans compte, comme le catalogue ; seul « Exécuter » demande de se connecter (ADR 0037).
      { path: 'subjects/:slug/tour', element: <TourOutlinePage /> },
      { path: 'subjects/:slug/tour/:notion', element: <TourNotionPage /> },
      {
        path: 'subjects/:slug/practice/:notion',
        element: (
          <RequireAuth>
            <PracticePage />
          </RequireAuth>
        ),
      },
      {
        path: 'subjects/:slug/exam',
        element: (
          <RequireAuth>
            <MockExamPage />
          </RequireAuth>
        ),
      },
      {
        path: 'review',
        element: (
          <RequireAuth>
            <ReviewPage />
          </RequireAuth>
        ),
      },
      {
        path: 'ranked',
        element: (
          <RequireAuth>
            <RankedPage />
          </RequireAuth>
        ),
      },
      {
        path: 'ranked/duels/:id',
        element: (
          <RequireAuth>
            <DuelPage />
          </RequireAuth>
        ),
      },
      {
        path: 'groups',
        element: (
          <RequireAuth>
            <GroupsPage />
          </RequireAuth>
        ),
      },
      {
        path: 'groups/:groupId',
        element: (
          <RequireAuth>
            <GroupPage />
          </RequireAuth>
        ),
      },
      {
        path: 'challenges/:challengeId',
        element: (
          <RequireAuth>
            <ChallengePage />
          </RequireAuth>
        ),
      },
      {
        path: 'live/:gameId',
        element: (
          <RequireAuth>
            <LivePage />
          </RequireAuth>
        ),
      },
      {
        path: 'join/:token',
        element: (
          <RequireAuth>
            <JoinPage />
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
      {
        path: 'admin/reports',
        element: (
          <RequireAuth>
            <ContentReportsPage />
          </RequireAuth>
        ),
      },
      {
        path: 'admin/retention',
        element: (
          <RequireAuth>
            <RetentionPage />
          </RequireAuth>
        ),
      },
      // Un profil public se lit sans compte (ADR 0043), et l'essai se fait sans compte (ADR 0050).
      { path: 'u/:handle', element: <PublicProfilePage /> },
      { path: 'try/:slug', element: <TrialPage /> },
      { path: 'auth/callback', element: <AuthCallbackPage /> },
    ],
  },
]
