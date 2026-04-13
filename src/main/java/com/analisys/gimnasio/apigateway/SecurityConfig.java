package com.analisys.gimnasio.apigateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
/**
	* Configuración de seguridad del API Gateway.
	*
	* - Autenticación: valida JWT emitidos por Keycloak.
	* - Autorización: restringe el acceso por roles (ADMIN/MEMBER/TRAINER).
	* - Swagger/Actuator: expone endpoints de documentación y health sin autenticación.
	*/
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						// Actuator
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()

						// Swagger/OpenAPI del gateway + api-docs de microservicios (a través del gateway)
						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/api-docs/**",
								"/miembros/api-docs/**",
								"/equipment/api-docs/**",
								"/clases/api-docs/**",
								"/trainers/api-docs/**")
						.permitAll()
					// Ruta de agregación (resumen de miembro)
					.requestMatchers("/api/resumen/**").hasAnyRole("ADMIN", "MEMBER")
						// Reglas de autorización por “dominio” (prefijos del gateway)
						.requestMatchers("/miembros/**").hasAnyRole("ADMIN", "MEMBER")
						.requestMatchers("/trainers/**").hasAnyRole("ADMIN", "TRAINER")
						.requestMatchers("/clases/**").hasAnyRole("ADMIN", "TRAINER", "MEMBER")
						.requestMatchers("/equipment/**").hasAnyRole("ADMIN", "TRAINER")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.build();
	}

	@Bean
	/**
		* Convierte el JWT de Keycloak en authorities de Spring Security.
		*
		* - Lee roles del realm desde el claim: realm_access.roles
		* - Los mapea a: ROLE_<ROL> (ej. ROLE_ADMIN)
		* - También preserva scopes estándar si vienen en el token.
		*/
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

		Converter<Jwt, Collection<GrantedAuthority>> realmRoles = jwt -> {
			List<GrantedAuthority> authorities = new ArrayList<>();
			Object realmAccessObj = jwt.getClaims().get("realm_access");
			if (realmAccessObj instanceof Map<?, ?> realmAccess) {
				Object rolesObj = realmAccess.get("roles");
				if (rolesObj instanceof Collection<?> roles) {
					for (Object role : roles) {
						if (role != null) {
							authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
						}
					}
				}
			}
			return authorities;
		};

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			List<GrantedAuthority> combined = new ArrayList<>();
			combined.addAll(scopes.convert(jwt));
			combined.addAll(realmRoles.convert(jwt));
			return combined;
		});
		return converter;
	}
}
