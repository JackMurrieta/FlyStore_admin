/**
 * Tipos para productos (Admin)
 */

export interface Marca {
  id: string
  nombre: string
  logo_url: string | null
}

export interface Subcategoria {
  id: string
  nombre: string
  id_categoria: string
}

export interface ProductoImagen {
  id: string
  storage_path: string
  alt_text: string
  orden: number
  es_principal: boolean
}

export interface ProductoVariante {
  id: string
  sku: string
  talla: string | null
  color: string | null
  color_hex: string | null
  precio_extra: number
  stock: number
  stock_minimo: number
  id_imagen: string | null
  activo: boolean
}

export interface Producto {
  id: string
  id_subcategoria: string | null
  id_marca: string | null
  nombre: string
  slug: string
  sku: string
  descripcion: string
  precio_base: number
  precio_mayoreo: number | null
  precio_distribuidor: number | null
  min_mayoreo: number | null
  min_distribuidor: number | null
  activo: boolean
  permitir_sin_stock: boolean
  destacado: boolean
  created_at: string
  updated_at: string
  marca?: Marca
  subcategoria?: Subcategoria
  imagenes?: ProductoImagen[]
  variantes?: ProductoVariante[]
}

export interface CreateProductoDto {
  id_subcategoria?: string | null
  id_marca?: string | null
  nombre: string
  slug: string
  sku: string
  descripcion: string
  precio_base: number
  precio_mayoreo?: number | null
  precio_distribuidor?: number | null
  min_mayoreo?: number | null
  min_distribuidor?: number | null
  activo: boolean
  permitir_sin_stock: boolean
  destacado: boolean
}

export interface UpdateProductoDto {
  id_subcategoria?: string | null
  id_marca?: string | null
  nombre?: string
  slug?: string
  sku?: string
  descripcion?: string
  precio_base?: number
  precio_mayoreo?: number | null
  precio_distribuidor?: number | null
  min_mayoreo?: number | null
  min_distribuidor?: number | null
  activo?: boolean
  permitir_sin_stock?: boolean
  destacado?: boolean
}
