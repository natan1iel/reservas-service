package com.volei.reservas.query;

import com.volei.reservas.client.QuadraClient;
import com.volei.reservas.client.QuadraDisponibilidadeDTO;
import com.volei.reservas.model.StatusReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservaQueryService {
    private static final int DURACAO_MINIMA_EM_MINUTOS = 60;

    private final ReservaConsultaRepository reservaConsultaRepository;
    private final QuadraClient quadraClient;

    public ReservaQueryService(ReservaConsultaRepository reservaConsultaRepository, QuadraClient quadraClient) {
        this.reservaConsultaRepository = reservaConsultaRepository;
        this.quadraClient = quadraClient;
    }

    @Transactional(readOnly = true)
    public HorariosLivresDTO horariosLivres(Long quadraId, LocalDate data) {
        QuadraDisponibilidadeDTO quadra = quadraClient.consultarDisponibilidade(quadraId, data);

        if (!Boolean.TRUE.equals(quadra.aberta())) {
            return HorariosLivresDTO.fechada(quadra);
        }

        int intervalo = quadra.intervaloMinutos();

        List<ReservaConsulta> ocupadas = reservaConsultaRepository
                .findByQuadraIdAndDataAndStatusOrderByHoraInicio(quadraId, data, StatusReserva.ATIVA.name());

        List<BlocoDTO> livres = new ArrayList<>();

        LocalTime cursor = quadra.abertura();

        for (ReservaConsulta reservada : ocupadas) {
            LocalTime limite = reservada.getHoraInicio().minusMinutes(intervalo);

            if (limite.isAfter(cursor) && duracaoEmMinutos(cursor, limite) >= DURACAO_MINIMA_EM_MINUTOS) {
                livres.add(new BlocoDTO(cursor, limite));
            }

            LocalTime proximo = reservada.getHoraFim().plusMinutes(intervalo);
            if (proximo.isAfter(cursor)) {
                cursor = proximo;
            }
        }

        if (
                quadra.fechamento().isAfter(cursor) && duracaoEmMinutos(cursor, quadra.fechamento()) >= DURACAO_MINIMA_EM_MINUTOS) {
            livres.add(new BlocoDTO(cursor, quadra.fechamento()));
        }

        return new HorariosLivresDTO(quadra.codigo(), quadra.abertura(), quadra.fechamento(), intervalo, livres);
    }

    @Transactional(readOnly = true)
    public List<ReservaConsulta> minhasReservas(Long usuarioId) {
        return reservaConsultaRepository.findByUsuarioIdOrderByDataDescHoraInicioDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<ReservaConsulta> reservasDaQuadra(Long quadraId, LocalDate data) {
        return reservaConsultaRepository.findByQuadraIdAndDataOrderByHoraInicio(quadraId, data);
    }

    private long duracaoEmMinutos(LocalTime inicio, LocalTime fim) {
        return Duration.between(inicio, fim).toMinutes();
    }
}
