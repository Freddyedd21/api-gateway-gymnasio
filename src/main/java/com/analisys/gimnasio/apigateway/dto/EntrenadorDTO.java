package com.analisys.gimnasio.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EntrenadorDTO(
    Long id,
    String nombre,
    String especialidad
) {}
