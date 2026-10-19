package br.com.confeitasys.config;

import br.com.confeitasys.seguranca.AutenticacaoInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração web:
 *  - CORS: libera o FRONTEND (que roda em outro endereço/porta, ex.: Live Server
 *    em http://127.0.0.1:5500) para chamar esta API em http://localhost:8080;
 *  - registra o interceptor de login nas rotas /api/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacao;
    private final List<String> origens;

    public WebConfig(AutenticacaoInterceptor autenticacao,
                     @Value("${confeitasys.cors.origens:http://localhost:*,http://127.0.0.1:*}") String origens) {
        this.autenticacao = autenticacao;
        this.origens = Arrays.stream(origens.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacao).addPathPatterns("/api/**");
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(origens);
        cfg.setAllowedOrigins(List.of("null")); // telas abertas direto do arquivo (file://)
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cfg);
        return new CorsFilter(fonte);
    }
}
