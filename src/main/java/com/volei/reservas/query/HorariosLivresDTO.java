package com.volei.reservas.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.volei.reservas.client.QuadraDisponibilidadeDTO;

import java.time.LocalTime;
import java.util.List;

public record HorariosLivresDTO(String quadra, @JsonFormat(pattern = "HH:mm") LocalTime abertura,
        @JsonFormat(pattern = "HH:mm") LocalTime fechamento, Integer intervaloMinutos, List<BlocoDTO> livres) {
    public static HorariosLivresDTO fechada(QuadraDisponibilidadeDTO quadra) {
        return new HorariosLivresDTO(quadra.codigo(), null, null, quadra.intervaloMinutos(), List.of());
    }
}
