package com.volei.reservas.exception;

import com.volei.reservas.model.Reserva;

public class ConflitoDeHorarioException extends RuntimeException {
    private final Reserva conflitante;

    public ConflitoDeHorarioException(Reserva conflitante) {
        super("Já existe reserva nesta quadra no horário solicitado");
        this.conflitante = conflitante;
    }

    public Reserva getConflitante() {
        return conflitante;
    }
}
