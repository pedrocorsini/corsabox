import { Navigate, Route, Routes } from 'react-router'
import { GuestOnly, RequireAuth } from './auth/guards'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import GoogleLoginPage from './pages/GoogleLoginPage'
import RegisterPage from './pages/RegisterPage'
import CatalogPage from './pages/CatalogPage'
import ProfilePage from './pages/ProfilePage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import LibraryPage from './pages/LibraryPage'
import TitlePage from './pages/TitlePage'


export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/auth/google" element={<GoogleLoginPage />} />
      <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
      <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<RequireAuth />}>
        <Route path="/catalogo" element={<CatalogPage />} />
          <Route path="/titulo/:format/:id" element={<TitlePage />} />
          <Route path="/biblioteca" element={<LibraryPage />} />
        <Route path="/perfil" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
