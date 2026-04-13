package com.analisys.gimnasio.apigateway.dto;

import java.util.List;

/**
 * Respuesta agregada que combina información de múltiples microservicios
 * para presentar un resumen completo del miembro del gimnasio.
 *
 * Datos obtenidos de:
 * - miembros-service  → información personal del miembro
 * - trainer-service   → datos del entrenador personal asignado
 * - clases-service    → clases disponibles en el gimnasio
 * - equipment-service → equipos disponibles para usar
 */
public record MiembroResumenDTO(
    MiembroDTO miembro,
    EntrenadorDTO entrenadorPersonal,
    List<ClaseDTO> clasesDisponibles,
    List<EquipmentDTO> equiposDisponibles,
    EstadisticasGimnasio estadisticas
) {

    public record EstadisticasGimnasio(
        int totalClasesDisponibles,
        int totalEquiposDisponibles,
        int plazasLibresTotales,
        boolean tieneEntrenadorAsignado
    ) {}
}
