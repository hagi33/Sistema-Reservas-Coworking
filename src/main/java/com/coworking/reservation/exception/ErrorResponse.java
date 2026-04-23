package com.coworking.reservation.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/*
* Esta clase define el formato estándar de error que devuelve la API*/
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)//Esta anotación hace que los campos con valor null no aparezcan en el JSON.
public class ErrorResponse {

    //Los campos de la clase son final porque una vez creado el error no debería modificarse.
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Map<String, String> validationErrors;
}
