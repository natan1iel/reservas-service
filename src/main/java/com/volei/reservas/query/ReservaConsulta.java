package com.volei.reservas.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class ReservaConsulta {
    @Id
    private Long id;

    private Long quadraId;

    private String quadraCodigo;
    private String quadraTipo;
    private Integer intervaloMinutos;

    private Long usuarioId;

    private String usuarioNome;

    private LocalDate data;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFim;

    private String status;

    public ReservaConsulta() {
    }

    public ReservaConsulta(Long id, Long quadraId, String quadraCodigo, String quadraTipo, Integer intervaloMinutos,
            Long usuarioId, String usuarioNome, LocalDate data, LocalTime horaInicio, LocalTime horaFim, String status) {
        this.id = id;
        this.quadraId = quadraId;
        this.quadraCodigo = quadraCodigo;
        this.quadraTipo = quadraTipo;
        this.intervaloMinutos = intervaloMinutos;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.data = data;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getQuadraId() {
        return quadraId;
    }

    public String getQuadraCodigo() {
        return quadraCodigo;
    }

    public String getQuadraTipo() {
        return quadraTipo;
    }

    public Integer getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
