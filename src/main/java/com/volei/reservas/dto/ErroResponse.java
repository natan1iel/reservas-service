package com.volei.reservas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(String erro, String mensagem, ReservaConflitante reservaConflitante) {
    public record ReservaConflitante(Long id, @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaFim) {
    }

    public ErroResponse(String erro, String mensagem) {
        this(erro, mensagem, null);
    }
}
