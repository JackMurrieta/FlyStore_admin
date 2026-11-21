package com.flystore.contratos.autenticacion;

/**
 * Resultado de confirmar un enlace de correo.
 *
 * @param tipo    tipo de enlace confirmado (email, recovery, email_change…).
 * @param mensaje texto para mostrar.
 * @param usuario usuario con sesión abierta; null si falta otra confirmación (cambio de correo seguro).
 */
public record ConfirmacionCuentaResponse(String tipo, String mensaje, UserInfoResponse usuario) {}
