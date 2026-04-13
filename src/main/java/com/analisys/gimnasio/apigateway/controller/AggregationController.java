package com.analisys.gimnasio.apigateway.controller;

import com.analisys.gimnasio.apigateway.dto.MiembroResumenDTO;
import com.analisys.gimnasio.apigateway.service.AggregationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de agregación de respuestas del API Gateway.
 *
 * Implementa el patrón API Composition: en lugar de que el cliente
 * haga múltiples llamadas a distintos microservicios, el gateway
 * orquesta esas llamadas y devuelve una respuesta unificada.
 *
 * Ejemplo de uso real:
 *   Un panel/dashboard del miembro que necesita mostrar su perfil,
 *   su entrenador asignado, clases disponibles y equipos libres,
 *   todo en UNA sola petición HTTP.
 */
@RestController
@RequestMapping("/api/resumen")
@Tag(name = "Aggregation", description = "Endpoints de agregación que combinan datos de múltiples microservicios")
@SecurityRequirement(name = "bearer-jwt")
public class AggregationController {

    private final AggregationService aggregationService;

    public AggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Operation(
        summary = "Resumen completo de un miembro",
        description = """
            Agrega información de 4 microservicios en una sola respuesta:
            - **miembros-service**: datos personales del miembro
            - **trainer-service**: entrenador personal asignado (si tiene)
            - **clases-service**: listado de clases disponibles
            - **equipment-service**: equipos disponibles para usar

            Las llamadas a los servicios secundarios se ejecutan en paralelo.
            Si algún servicio no responde, la respuesta se degrada parcialmente
            en lugar de fallar completamente (resilience pattern).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumen agregado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Miembro no encontrado"),
        @ApiResponse(responseCode = "401", description = "Token JWT inválido o ausente")
    })
    @GetMapping("/miembro/{miembroId}")
    public ResponseEntity<MiembroResumenDTO> getResumenMiembro(
            @Parameter(description = "ID del miembro del gimnasio") @PathVariable Long miembroId,
            @RequestHeader("Authorization") String authHeader) {

        MiembroResumenDTO resumen = aggregationService.obtenerResumenMiembro(miembroId, authHeader);
        return ResponseEntity.ok(resumen);
    }
}
