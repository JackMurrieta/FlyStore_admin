// src/context/AuthProvider.tsx

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import type { ReactNode } from 'react'

import { api } from '../services/apiClient'

import type { LoginDto, User } from '../types'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isAdmin: boolean
  loading: boolean
  submitting: boolean
  error: string
  clearError(): void
  login(dto: LoginDto): Promise<void>
  logout(): Promise<void>
  refreshSession(): Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

interface Props {
  children: ReactNode
}

export function AuthProvider({ children }: Props) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const navigate = useNavigate()

  useEffect(() => {
    // Verificar sesión actual en el backend
    api.auth.getSession()
      .then(data => {
        setUser(data.user)
        setLoading(false)
      })
      .catch(() => {
        setUser(null)
        setLoading(false)
      })
  }, [])

  async function login(dto: LoginDto) {
    setError('')
    setSubmitting(true)

    try {
      console.log('[AuthProvider] Iniciando login...')
      const response = await api.auth.login(dto)
      console.log('[AuthProvider] Login exitoso, usuario:', response.user.email)

      // Verificar que el usuario sea admin
      if (response.user.rol !== 'admin') {
        throw new Error('No tienes permisos de administrador')
      }

      setUser(response.user)

      // Redirigir al dashboard admin
      setTimeout(() => {
        console.log('[AuthProvider] Navegando a /dashboard')
        navigate('/dashboard', { replace: true })
      }, 100)
    } catch (err) {
      console.error('[AuthProvider] Error en login:', err)
      setError(err instanceof Error ? err.message : 'Error al iniciar sesión.')
    } finally {
      setSubmitting(false)
    }
  }

  async function logout() {
    setError('')
    setSubmitting(true)

    try {
      await api.auth.logout()
      setUser(null)
      navigate('/login', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cerrar sesión.')
    } finally {
      setSubmitting(false)
    }
  }

  async function refreshSession() {
    setLoading(true)
    try {
      const data = await api.auth.getSession()
      setUser(data.user)
    } catch (err) {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isAdmin: user?.rol === 'admin',
      loading,
      submitting,
      error,
      clearError: () => setError(''),
      login,
      logout,
      refreshSession,
    }),
    [user, loading, submitting, error]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuthContext() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuthContext debe utilizarse dentro de AuthProvider.')
  }

  return context
}
