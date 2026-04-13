package com.analisys.gimnasio.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EquipmentDTO(
    Long id,
    String nombre,
    String descripcion,
    Integer cantidadTotal,
    Integer cantidadDisponible,
    String estado,
    boolean disponible
) {}
