package com.volei.reservas.command;

import com.volei.reservas.client.QuadraClient;
import com.volei.reservas.client.QuadraDisponibilidadeDTO;
import com.volei.reservas.exception.ConflitoDeHorarioException;
import com.volei.reservas.exception.RegraDeNegocioException;
import com.volei.reservas.model.Reserva;
import com.volei.reservas.model.StatusReserva;
import com.volei.reservas.query.ReservaConsulta;
import com.volei.reservas.query.ReservaConsultaRepository;
import com.volei.reservas.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaCommandService {
    public static final int LIMITE_DE_RESERVAS_ATIVAS = 2;

    private final ReservaRepository reservaRepository;
    private final ReservaConsultaRepository reservaConsultaRepository;
    private final QuadraClient quadraClient;

    public ReservaCommandService(ReservaRepository reservaRepository,
            ReservaConsultaRepository reservaConsultaRepository, QuadraClient quadraClient) {
        this.reservaRepository = reservaRepository;
        this.reservaConsultaRepository = reservaConsultaRepository;
        this.quadraClient = quadraClient;
    }

    @Transactional
    public ReservaConsulta criar(CriarReservaCommand comando, Long usuarioId, String usuarioNome) {
        validarCamposObrigatorios(comando);

        if (comando.data().isBefore(LocalDate.now())) {
            throw RegraDeNegocioException.invalida("Não é possível reservar uma data que já passou");
        }

        long duracao = Duration.between(comando.horaInicio(), comando.horaFim()).toMinutes();
        if (duracao != 60 && duracao != 120) {
            throw RegraDeNegocioException.invalida(
                    "A reserva precisa ter exatamente 1h ou 2h de duração (pedido: " + duracao + " min)");
        }

        QuadraDisponibilidadeDTO quadra =
                quadraClient.consultarDisponibilidade(comando.quadraId(), comando.data());

        if (!Boolean.TRUE.equals(quadra.ativa())) {
            throw RegraDeNegocioException.invalida("A quadra " + quadra.codigo() + " está inativa");
        }

        if (!Boolean.TRUE.equals(quadra.aberta())) {
            throw RegraDeNegocioException.invalida("A quadra " + quadra.codigo() + " não abre em " + comando.data());
        }

        if (comando.horaInicio().isBefore(quadra.abertura()) || comando.horaFim().isAfter(quadra.fechamento())) {
            throw RegraDeNegocioException.invalida(
                    "A quadra " + quadra.codigo() + " funciona das " + quadra.abertura() + " às " + quadra.fechamento() + " neste dia");
        }

        validarLimiteDoUsuario(usuarioId);

        validarConflito(comando, quadra.intervaloMinutos());

        Reserva reserva = new Reserva(comando.quadraId(), usuarioId, comando.data(), comando.horaInicio(),
                comando.horaFim());

        Reserva salva = reservaRepository.save(reserva);
        return reservaConsultaRepository.save(montarConsulta(salva, quadra, usuarioNome));
    }

    @Transactional
    public ReservaConsulta cancelar(CancelarReservaCommand comando) {
        Reserva reserva = reservaRepository.findById(comando.reservaId())
                .orElseThrow(() -> RegraDeNegocioException.reservaNaoEncontrada(comando.reservaId()));

        if (!reserva.getUsuarioId().equals(comando.usuarioId()) && !"ADMIN".equals(comando.papel())) {
            throw RegraDeNegocioException.negada("Você só pode cancelar suas próprias reservas");
        }

        if (reserva.getStatus() == StatusReserva.CANCELADA) {
            throw RegraDeNegocioException.operacaoInvalida("Esta reserva já está cancelada");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reservaRepository.save(reserva);

        ReservaConsulta consulta = reservaConsultaRepository.findById(comando.reservaId())
                .orElseThrow(
                        () -> new IllegalStateException("Reserva " + comando.reservaId() + " existe na escrita mas não na leitura"));
        consulta.setStatus(StatusReserva.CANCELADA.name());

        return reservaConsultaRepository.save(consulta);
    }

    public static boolean conflita(LocalTime inicioNovo, LocalTime fimNovo, LocalTime inicioExistente,
            LocalTime fimExistente, int intervaloMinutos) {
        return inicioNovo.isBefore(fimExistente.plusMinutes(intervaloMinutos))
            && fimNovo.plusMinutes(intervaloMinutos).isAfter(inicioExistente);
    }

    private void validarConflito(CriarReservaCommand comando, int intervaloMinutos) {
        List<Reserva> doDia = reservaRepository.findByQuadraIdAndDataAndStatus(comando.quadraId(), comando.data(),
                StatusReserva.ATIVA);

        for (Reserva existente : doDia) {
            if (
                    conflita(comando.horaInicio(), comando.horaFim(), existente.getHoraInicio(), existente.getHoraFim(), intervaloMinutos)) {
                throw new ConflitoDeHorarioException(existente);
            }
        }
    }

    private void validarLimiteDoUsuario(Long usuarioId) {
        long ativas = reservaRepository.countByUsuarioIdAndStatusAndDataGreaterThanEqual(usuarioId, StatusReserva.ATIVA,
                LocalDate.now());

        if (ativas >= LIMITE_DE_RESERVAS_ATIVAS) {
            throw RegraDeNegocioException.limiteDeReservas(LIMITE_DE_RESERVAS_ATIVAS);
        }
    }

    private void validarCamposObrigatorios(CriarReservaCommand comando) {
        if (comando.quadraId() == null) {
            throw RegraDeNegocioException.invalida("Informe a quadra");
        }
        if (comando.data() == null) {
            throw RegraDeNegocioException.invalida("Informe a data");
        }
        if (comando.horaInicio() == null || comando.horaFim() == null) {
            throw RegraDeNegocioException.invalida("Informe a hora de início e a de fim");
        }
    }

    private ReservaConsulta montarConsulta(Reserva reserva, QuadraDisponibilidadeDTO quadra, String usuarioNome) {
        return new ReservaConsulta(reserva.getId(), reserva.getQuadraId(), quadra.codigo(), quadra.tipo(),
                quadra.intervaloMinutos(), reserva.getUsuarioId(), usuarioNome, reserva.getData(),
                reserva.getHoraInicio(), reserva.getHoraFim(), reserva.getStatus().name());
    }
}
