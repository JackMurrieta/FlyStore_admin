import type {
  LoginDto,
  RegisterDto,
  LoginResponse,
  SessionResponse,
  RefreshResponse,
  RegisterResult,
} from '../types'

// URL de tu backend (configúrala en .env.local)
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8787'

// Re-exportar para retrocompatibilidad
export type { RegisterResult }

/**
 * Opciones de configuración para las peticiones HTTP
 */
interface RequestOptions extends RequestInit {
  requiresAuth?: boolean // Si requiere autenticación (por defecto: true)
}

/**
 * Cliente HTTP para comunicarse con el backend
 * Usa HTTP-only cookies para autenticación automática
 */
export async function apiRequest<T = any>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { requiresAuth = true, ...fetchOptions } = options

  // Preparar headers
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  }

  // Hacer la petición
  const response = await fetch(`${API_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
    credentials: 'include', // IMPORTANTE: Envía cookies automáticamente
  })

  // Manejo de errores
  if (!response.ok) {
    // Token expirado o inválido
    if (response.status === 401) {
      // Intentar refrescar la sesión
      const refreshResponse = await fetch(`${API_URL}/api/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      })

      if (refreshResponse.ok) {
        // Reintentar la petición original
        return apiRequest<T>(endpoint, options)
      } else {
        throw new Error('Sesión expirada. Por favor inicia sesión nuevamente.')
      }
    }

    // Sin permisos (probablemente no es admin)
    if (response.status === 403) {
      throw new Error('No tienes permisos para acceder a este recurso.')
    }

    // Otros errores
    const errorData = await response.json().catch(() => ({}))
    throw new Error(errorData.error || `Error ${response.status}: ${response.statusText}`)
  }

  // Respuesta exitosa
  return response.json()
}

/**
 * API Endpoints organizados por dominio
 */
export const api = {
  // ============================================
  // AUTENTICACIÓN
  // ============================================
  auth: {
    /**
     * Iniciar sesión con email y contraseña
     */
    login: async (dto: LoginDto): Promise<LoginResponse> => {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(dto)
      })

      if (!response.ok) {
        const data = await response.json()
        throw new Error(data.error || 'Error al iniciar sesión')
      }

      return response.json()
    },

    /**
     * Obtener sesión actual
     */
    getSession: async (): Promise<SessionResponse> => {
      const response = await fetch(`${API_URL}/api/auth/session`, {
        credentials: 'include'
      })

      if (!response.ok) {
        return { user: null }
      }

      return response.json()
    },

    /**
     * Refrescar token de sesión
     */
    refresh: async (): Promise<RefreshResponse> => {
      const response = await fetch(`${API_URL}/api/auth/refresh`, {
        method: 'POST',
        credentials: 'include'
      })

      if (!response.ok) {
        const data = await response.json()
        throw new Error(data.error || 'Error al refrescar sesión')
      }

      return response.json()
    },

    /**
     * Cerrar sesión
     */
    logout: async (): Promise<void> => {
      await fetch(`${API_URL}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      })
    },
  },

  // ============================================
  // ADMIN - PRODUCTOS
  // ============================================
  admin: {
    productos: {
      /**
       * Listar todos los productos (incluye inactivos)
       * GET /api/admin/productos
       */
      getAll: () => apiRequest('/api/admin/productos'),

      /**
       * Obtener producto por ID
       * GET /api/admin/productos/:id
       */
      getById: (id: string) => apiRequest(`/api/admin/productos/${id}`),

      /**
       * Crear producto
       * POST /api/admin/productos
       */
      create: (producto: any) =>
        apiRequest('/api/admin/productos', {
          method: 'POST',
          body: JSON.stringify(producto),
        }),

      /**
       * Actualizar producto
       * PUT /api/admin/productos/:id
       */
      update: (id: string, cambios: any) =>
        apiRequest(`/api/admin/productos/${id}`, {
          method: 'PUT',
          body: JSON.stringify(cambios),
        }),

      /**
       * Eliminar producto
       * DELETE /api/admin/productos/:id
       */
      delete: (id: string) =>
        apiRequest(`/api/admin/productos/${id}`, {
          method: 'DELETE',
        }),
    },

    /**
     * Sincronización de flycaps
     */
    flycaps: {
      /**
       * Obtener metadata (marcas y categorías)
       * GET /api/admin/catalogo/flycaps/metadata
       */
      getMetadata: () => apiRequest('/api/admin/catalogo/flycaps/metadata'),

      /**
       * Obtener drops disponibles
       * GET /api/admin/catalogo/flycaps/drops
       */
      getDrops: () => apiRequest('/api/admin/catalogo/flycaps/drops'),

      /**
       * Sincronizar productos desde storage
       * POST /api/admin/catalogo/flycaps/sync
       */
      sync: (params: any) =>
        apiRequest('/api/admin/catalogo/flycaps/sync', {
          method: 'POST',
          body: JSON.stringify(params),
        }),
    },
  },

  // ============================================
  // PÚBLICO - PRODUCTOS (para preview)
  // ============================================
  public: {
    productos: {
      /**
       * Listar productos activos del catálogo
       * GET /api/public/productos
       */
      getAll: (params?: {
        categoria?: string
        subcategoria?: string
        marca?: string
        destacados?: boolean
        limit?: number
        offset?: number
      }) => {
        const query = new URLSearchParams()
        if (params?.categoria) query.set('categoria', params.categoria)
        if (params?.subcategoria) query.set('subcategoria', params.subcategoria)
        if (params?.marca) query.set('marca', params.marca)
        if (params?.destacados) query.set('destacados', 'true')
        if (params?.limit) query.set('limit', params.limit.toString())
        if (params?.offset) query.set('offset', params.offset.toString())

        const queryString = query.toString()
        return apiRequest(`/api/public/productos${queryString ? `?${queryString}` : ''}`, {
          requiresAuth: false,
        })
      },

      /**
       * Obtener producto por slug
       * GET /api/public/productos/:slug
       */
      getBySlug: (slug: string) =>
        apiRequest(`/api/public/productos/${slug}`, {
          requiresAuth: false,
        }),

      /**
       * Obtener productos destacados
       * GET /api/public/productos/destacados
       */
      getDestacados: (limit?: number) =>
        apiRequest(`/api/public/productos/destacados${limit ? `?limit=${limit}` : ''}`, {
          requiresAuth: false,
        }),
    },
  },
}

/**
 * Helper para manejar errores de API de forma consistente
 */
export function handleApiError(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  return 'Ocurrió un error inesperado. Por favor intenta de nuevo.'
}
