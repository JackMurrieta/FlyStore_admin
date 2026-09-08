import { Navigate, Outlet } from 'react-router-dom'
import { useAuthContext } from './AuthProvider'

export function ProtectedRoute() {
  const { loading, isAuthenticated, isAdmin } = useAuthContext()

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh'
      }}>
        Cargando...
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!isAdmin) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh'
      }}>
        <h1>Acceso Denegado</h1>
        <p>No tienes permisos de administrador.</p>
      </div>
    )
  }

  return <Outlet />
}
