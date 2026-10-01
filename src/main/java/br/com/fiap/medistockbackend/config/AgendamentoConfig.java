package br.com.fiap.medistockbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("oracle")
@EnableScheduling
public class AgendamentoConfig {
}
