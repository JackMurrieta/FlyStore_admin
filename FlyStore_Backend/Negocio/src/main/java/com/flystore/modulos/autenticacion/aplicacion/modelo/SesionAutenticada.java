package com.flystore.modulos.autenticacion.aplicacion.modelo;
public record SesionAutenticada(String accessToken, String tokenType, Integer expiresIn,
                               String refreshToken, IdentidadAutenticada user) {}
