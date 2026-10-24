package com.volei.reservas.client;

import java.time.LocalTime;

public record QuadraDisponibilidadeDTO(Long id, String codigo, String tipo, Boolean ativa, Integer intervaloMinutos,
        Boolean aberta, LocalTime abertura, LocalTime fechamento) {
}
