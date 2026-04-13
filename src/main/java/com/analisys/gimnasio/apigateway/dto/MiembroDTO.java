package com.analisys.gimnasio.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MiembroDTO(
    Long id,
    String nombre,
    String email,
    String fechaInscripcion,
    Long entrenadorPersonalId
) {}
