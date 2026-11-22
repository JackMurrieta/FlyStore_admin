package com.flystore.modulos.gestionarventa.aplicacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj de la venta. Las fechas de vigencia de los códigos y el plazo de 42 h se calculan en la
 * zona horaria de la tienda, no en la del servidor.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionVenta {

    @Bean
    public Clock relojVenta(@Value("${app.ventas.zona-horaria:America/Hermosillo}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
