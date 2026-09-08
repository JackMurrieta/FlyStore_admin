/**
 * DTOs de Autenticación
 * Corresponde a los endpoints /api/auth/*
 */

/**
 * DTO para login
 * POST /api/auth/login
 */
export interface LoginDto {
  email: string
  password: string
}

/**
 * DTO para registro
 * POST /api/auth/register
 */
export interface RegisterDto {
  nombre: string
  telefono?: string
  direccion?: string
  email: string
  password: string
}

/**
 * Usuario devuelto por el backend en sesiones y respuestas de auth
 * Usado en: login, register, session, refresh
 */
export interface User {
  id: string
  email: string
  nombre: string
  telefono: string
  direccion: string
  rol?: 'admin' | 'cliente' // ROL del usuario
}

/**
 * Respuesta de login exitoso
 * POST /api/auth/login
 */
export interface LoginResponse {
  ok: true
  user: User
}

/**
 * Respuesta de registro
 * POST /api/auth/register
 */
export interface RegisterResponse {
  ok: true
  requiresEmailConfirmation: boolean
  user?: User
  message?: string
}

/**
 * Respuesta de sesión actual
 * GET /api/auth/session
 */
export interface SessionResponse {
  user: User | null
}

/**
 * Respuesta de refresh token
 * POST /api/auth/refresh
 */
export interface RefreshResponse {
  ok: true
  user: User
}

/**
 * Resultado del proceso de registro para el frontend
 */
export type RegisterResult = 'ok' | 'confirm_email'
