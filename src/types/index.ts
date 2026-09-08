/**
 * Archivo central de exportación de tipos
 * Importa desde aquí todos los tipos necesarios en tu aplicación
 */

// Tipos de autenticación
export type {
  LoginDto,
  RegisterDto,
  User,
  LoginResponse,
  RegisterResponse,
  SessionResponse,
  RefreshResponse,
  RegisterResult,
} from './auth.types'

// Tipos de productos (admin)
export type {
  Producto,
  ProductoVariante,
  ProductoImagen,
  CreateProductoDto,
  UpdateProductoDto,
} from './producto.types'
