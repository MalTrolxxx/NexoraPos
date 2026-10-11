package com.nexora.pos.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "Hoy" depende de la zona horaria de la tienda, no de la del servidor (que en la nube suele ser UTC):
 * sin esto, las ventas de las 7 pm en México caerían en el corte del día siguiente.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${nexora.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
