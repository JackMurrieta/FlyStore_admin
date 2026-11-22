package com.flystore.modulos.autenticacion.aplicacion.modelo;

/**
 * Resultado de confirmar un enlace de correo (token_hash).
 *
 * @param tipo     tipo de enlace: email (registro), recovery, email_change…
 * @param sesion   sesión abierta por el enlace; null si el proveedor aún espera otra confirmación
 *                 (cambio de correo seguro: hay que confirmar en el correo anterior y en el nuevo).
 * @param mensaje  texto para el cliente.
 */
public record ConfirmacionCuenta(String tipo, AuthResponse sesion, String mensaje) {}
