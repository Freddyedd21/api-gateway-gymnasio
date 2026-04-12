package com.analisys.gimnasio.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
	* Punto de entrada del API Gateway.
	*
	* Este microservicio actúa como puerta de entrada (API Gateway) hacia el resto de
	* microservicios del sistema de gimnasio, aplicando reglas transversales como:
	* - Enrutamiento (routes)
	* - Seguridad (autenticación/autorización)
	* - Observabilidad (actuator)
	*/
@SpringBootApplication
public class ApiGatewayGymnasioApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayGymnasioApplication.class, args);
	}

}
