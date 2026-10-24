package com.volei.reservas.command;

public record CancelarReservaCommand(Long reservaId, Long usuarioId, String papel) {
}
