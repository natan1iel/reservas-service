package com.volei.reservas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long quadraId;

    private Long usuarioId;

    private LocalDate data;

    private LocalTime horaInicio;

    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    private StatusReserva status;

    public Reserva() {
    }

    public Reserva(Long quadraId, Long usuarioId, LocalDate data, LocalTime horaInicio, LocalTime horaFim) {
        this.quadraId = quadraId;
        this.usuarioId = usuarioId;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.status = StatusReserva.ATIVA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuadraId() {
        return quadraId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }
}
