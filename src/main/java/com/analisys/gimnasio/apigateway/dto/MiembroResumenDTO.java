package com.analisys.gimnasio.apigateway.dto;

import java.util.List;

/**
 * Respuesta agregada que combina información de múltiples microservicios
 * para presentar un resumen completo del miembro del gimnasio.
 *
 * Datos obtenidos de:
 * - miembros-service  → información personal del miembro
 * - trainer-service   → datos del entrenador personal asignado
 * - clases-service    → clases en las que está inscrito el miembro
 * - equipment-service → equipos que el miembro está usando
 */
public record MiembroResumenDTO(
    MiembroDTO miembro,
    EntrenadorDTO entrenadorPersonal,
    List<ClaseDTO> clasesInscritas,
    List<EquipmentDTO> equiposEnUso,
    EstadisticasGimnasio estadisticas
) {

    public record EstadisticasGimnasio(
        int totalClasesInscritas,
        int totalEquiposEnUso,
        int plazasLibresEnClases,
        boolean tieneEntrenadorAsignado
    ) {}
}
