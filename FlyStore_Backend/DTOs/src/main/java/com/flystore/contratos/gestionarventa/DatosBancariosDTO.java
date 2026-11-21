package com.flystore.contratos.gestionarventa;

/** Cuenta del administrador para transferir. El folio llega aparte (FolioDTO). */
public record DatosBancariosDTO(String banco, String titular, String numeroCuenta, String clabe) {}
