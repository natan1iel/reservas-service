package com.volei.reservas.query;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record BlocoDTO(@JsonFormat(pattern = "HH:mm") LocalTime inicio, @JsonFormat(pattern = "HH:mm") LocalTime fim) {
}
