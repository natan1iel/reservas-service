package com.volei.reservas.repository;

import com.volei.reservas.model.Reserva;
import com.volei.reservas.model.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByQuadraIdAndDataAndStatus(Long quadraId, LocalDate data, StatusReserva status);

    long countByUsuarioIdAndStatusAndDataGreaterThanEqual(Long usuarioId, StatusReserva status, LocalDate data);
}
