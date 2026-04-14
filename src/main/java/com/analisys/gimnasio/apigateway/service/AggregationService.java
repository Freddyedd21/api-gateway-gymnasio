package com.analisys.gimnasio.apigateway.service;

import com.analisys.gimnasio.apigateway.dto.*;
import com.analisys.gimnasio.apigateway.dto.MiembroResumenDTO.EstadisticasGimnasio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio de agregación que orquesta llamadas a múltiples microservicios
 * y combina sus respuestas en un único DTO enriquecido.
 *
 * Patrón: API Composition / Response Aggregation
 *
 * Llama en paralelo a:
 *   - miembros-service   (puerto 8080)
 *   - trainer-service     (puerto 8085)
 *   - clases-service      (puerto 8082)
 *   - equipment-service   (puerto 8081)
 *
 * Si algún servicio falla, la respuesta se degrada gracefully
 * devolviendo datos parciales en lugar de un error completo.
 */
@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final RestClient restClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final String miembrosUrl;
    private final String equipmentUrl;
    private final String clasesUrl;
    private final String trainersUrl;

    public AggregationService(
            RestClient.Builder restClientBuilder,
            @Value("${gateway.services.miembros:http://miembros-service}") String miembrosUrl,
            @Value("${gateway.services.equipment:http://gym-equipment-service}") String equipmentUrl,
            @Value("${gateway.services.clases:http://clases-service}") String clasesUrl,
            @Value("${gateway.services.trainers:http://trainer-service}") String trainersUrl) {
        this.restClient = restClientBuilder.build();
        this.miembrosUrl = miembrosUrl;
        this.equipmentUrl = equipmentUrl;
        this.clasesUrl = clasesUrl;
        this.trainersUrl = trainersUrl;
    }

    /**
     * Agrega información de múltiples servicios para crear un resumen
     * completo del miembro del gimnasio.
     *
     * @param miembroId ID del miembro a consultar
     * @param authHeader Token JWT para autenticación en los microservicios
     * @return Resumen agregado con información del miembro, entrenador, clases y equipos
     */
    public MiembroResumenDTO obtenerResumenMiembro(Long miembroId, String authHeader) {

        // 1. Obtener datos del miembro (obligatorio)
        MiembroDTO miembro = fetchMiembro(miembroId, authHeader);

        // 2. Lanzar llamadas en paralelo para clases, equipos y entrenador
        CompletableFuture<List<ClaseDTO>> clasesFuture = CompletableFuture
                .supplyAsync(() -> fetchClases(authHeader), executor);

        CompletableFuture<List<EquipmentDTO>> equiposFuture = CompletableFuture
                .supplyAsync(() -> fetchEquiposDisponibles(authHeader), executor);

        CompletableFuture<EntrenadorDTO> entrenadorFuture = CompletableFuture
                .supplyAsync(() -> fetchEntrenador(miembro.entrenadorPersonalId(), authHeader), executor);

        // 3. Esperar todas las respuestas
        CompletableFuture.allOf(clasesFuture, equiposFuture, entrenadorFuture).join();

        List<ClaseDTO> clases = clasesFuture.join();
        List<EquipmentDTO> equipos = equiposFuture.join();
        EntrenadorDTO entrenador = entrenadorFuture.join();

        // 4. Calcular estadísticas agregadas
        int plazasLibres = clases.stream()
                .mapToInt(c -> c.capacidadMaxima() - c.ocupacionActual())
                .sum();

        EstadisticasGimnasio stats = new EstadisticasGimnasio(
                clases.size(),
                equipos.size(),
                plazasLibres,
                entrenador != null
        );

        return new MiembroResumenDTO(miembro, entrenador, clases, equipos, stats);
    }

    // ================= Llamadas a microservicios =================

    private MiembroDTO fetchMiembro(Long miembroId, String authHeader) {
        log.info("Consultando miembro {} en miembros-service", miembroId);
        return restClient.get()
                .uri(miembrosUrl + "/api/miembros/obtenerMiembroPorId/{id}", miembroId)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .body(MiembroDTO.class);
    }

    private EntrenadorDTO fetchEntrenador(Long entrenadorId, String authHeader) {
        if (entrenadorId == null) {
            log.info("Miembro sin entrenador personal asignado");
            return null;
        }
        try {
            log.info("Consultando entrenador {} en trainer-service", entrenadorId);
            return restClient.get()
                    .uri(trainersUrl + "/api/entrenadores/{id}", entrenadorId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(EntrenadorDTO.class);
        } catch (Exception e) {
            log.warn("No se pudo obtener entrenador {}: {}", entrenadorId, e.getMessage());
            return null;
        }
    }

    private List<ClaseDTO> fetchClases(String authHeader) {
        try {
            log.info("Consultando clases en clases-service");
            List<ClaseDTO> result = restClient.get()
                    .uri(clasesUrl + "/gym/clases/obtener")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("No se pudo obtener clases: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EquipmentDTO> fetchEquiposDisponibles(String authHeader) {
        try {
            log.info("Consultando equipos disponibles en equipment-service");
            List<EquipmentDTO> result = restClient.get()
                    .uri(equipmentUrl + "/api/equipment/available")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("No se pudo obtener equipos: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
