package com.volei.reservas.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaConsultaRepository extends JpaRepository<ReservaConsulta, Long> {
    List<ReservaConsulta> findByQuadraIdAndDataAndStatusOrderByHoraInicio(Long quadraId, LocalDate data, String status);

    List<ReservaConsulta> findByUsuarioIdOrderByDataDescHoraInicioDesc(Long usuarioId);

    List<ReservaConsulta> findByQuadraIdAndDataOrderByHoraInicio(Long quadraId, LocalDate data);
}
