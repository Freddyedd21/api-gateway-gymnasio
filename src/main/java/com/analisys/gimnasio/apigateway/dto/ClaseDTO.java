package com.analisys.gimnasio.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaseDTO(
    Long id,
    String nombre,
    String horario,
    int capacidadMaxima,
    int ocupacionActual,
    Long entrenadorId
) {}
