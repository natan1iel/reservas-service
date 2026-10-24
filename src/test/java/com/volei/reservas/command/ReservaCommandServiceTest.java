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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaCommandServiceTest {
    private static final Long USUARIO_JOAO = 2L;
    private static final Long USUARIO_MARIA = 3L;
    private static final LocalDate AMANHA = LocalDate.now().plusDays(1);

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ReservaConsultaRepository reservaConsultaRepository;

    @Mock
    private QuadraClient quadraClient;

    @InjectMocks
    private ReservaCommandService reservaCommandService;

    @Test
    @DisplayName("reserva válida é gravada nas DUAS tabelas do CQRS")
    void criacaoGravaNasDuasTabelas() {
        prepararQuadraAberta();
        prepararUsuarioSemReservas();
        when(reservaRepository.findByQuadraIdAndDataAndStatus(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacao -> {
            Reserva salva = invocacao.getArgument(0);
            salva.setId(10L);
            return salva;
        });
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO, "João");

        verify(reservaRepository).save(any(Reserva.class));

        ArgumentCaptor<ReservaConsulta> capturada = ArgumentCaptor.forClass(ReservaConsulta.class);
        verify(reservaConsultaRepository).save(capturada.capture());
        ReservaConsulta consulta = capturada.getValue();

        assertEquals(10L, consulta.getId(), "a leitura tem que usar o MESMO id da escrita");
        assertEquals("Areia A", consulta.getQuadraCodigo());
        assertEquals("AREIA", consulta.getQuadraTipo());
        assertEquals(15, consulta.getIntervaloMinutos());
        assertEquals("João", consulta.getUsuarioNome());
        assertEquals("ATIVA", consulta.getStatus());
    }

    @Test
    @DisplayName("duração de 90 minutos é rejeitada")
    void duracaoDe90MinutosEhRejeitada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(11, 30)), USUARIO_JOAO,
                "João"));

        assertEquals(true, erro.getMessage().contains("1h ou 2h"));

        verify(quadraClient, never()).consultarDisponibilidade(anyLong(), any());
    }

    @Test
    @DisplayName("data no passado é rejeitada antes de qualquer chamada HTTP")
    void dataNoPassadoEhRejeitada() {
        CriarReservaCommand comando = new CriarReservaCommand(1L, LocalDate.now().minusDays(1), LocalTime.of(10, 0),
                LocalTime.of(12, 0));

        assertThrows(RegraDeNegocioException.class, () -> reservaCommandService.criar(comando, USUARIO_JOAO, "João"));

        verify(quadraClient, never()).consultarDisponibilidade(anyLong(), any());
    }

    @Test
    @DisplayName("horário fora da janela do dia é rejeitado")
    void foraDaJanelaEhRejeitado() {
        prepararQuadraAberta();

        assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(21, 0), LocalTime.of(23, 0)), USUARIO_JOAO,
                "João"));

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("dia em que a quadra não funciona é rejeitado")
    void diaFechadoEhRejeitado() {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenReturn(new QuadraDisponibilidadeDTO(1L, "Areia A", "AREIA", true, 15, false, null, null));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO,
                "João"));

        assertEquals(true, erro.getMessage().contains("não abre"));
    }

    @Test
    @DisplayName("quadra inativa é rejeitada")
    void quadraInativaEhRejeitada() {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenReturn(
                        new QuadraDisponibilidadeDTO(1L, "Areia A", "AREIA", false, 15, true, LocalTime.of(7, 0), LocalTime.of(22, 0)));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO,
                "João"));

        assertEquals(true, erro.getMessage().contains("inativa"));
    }

    @Test
    @DisplayName("campo faltando vira 400, não erro de servidor")
    void campoFaltandoEhRejeitado() {
        CriarReservaCommand semQuadra = new CriarReservaCommand(null, AMANHA, LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThrows(RegraDeNegocioException.class, () -> reservaCommandService.criar(semQuadra, USUARIO_JOAO, "João"));
    }

    @Test
    @DisplayName("quadra inexistente propaga o 404 do quadras-service")
    void quadraInexistentePropaga404() {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenThrow(RegraDeNegocioException.quadraNaoEncontrada(1L));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO,
                "João"));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
    }

    @Test
    @DisplayName("quadras-service fora do ar vira 503 e NADA é gravado")
    void servicoForaDoArNaoGravaNada() {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenThrow(RegraDeNegocioException.servicoIndisponivel("quadras-service indisponível"));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO,
                "João"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, erro.getStatus());

        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(reservaConsultaRepository, never()).save(any(ReservaConsulta.class));
    }

    @Test
    @DisplayName("reserva sobreposta é recusada e informa QUAL reserva bloqueou")
    void reservaSobrepostaEhRecusada() {
        prepararQuadraAberta();
        prepararUsuarioSemReservas();

        Reserva existente = reservaExistente(7L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(reservaRepository.findByQuadraIdAndDataAndStatus(1L, AMANHA, StatusReserva.ATIVA))
                .thenReturn(List.of(existente));

        ConflitoDeHorarioException erro = assertThrows(ConflitoDeHorarioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(11, 0), LocalTime.of(13, 0)), USUARIO_MARIA,
                "Maria"));

        assertEquals(7L, erro.getConflitante().getId());
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("reserva que respeita o intervalo exato é aceita")
    void reservaQueRespeitaOIntervaloEhAceita() {
        prepararQuadraAberta();
        prepararUsuarioSemReservas();

        when(reservaRepository.findByQuadraIdAndDataAndStatus(1L, AMANHA, StatusReserva.ATIVA))
                .thenReturn(List.of(reservaExistente(7L, LocalTime.of(10, 0), LocalTime.of(12, 0))));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> {
            Reserva salva = i.getArgument(0);
            salva.setId(11L);
            return salva;
        });
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        reservaCommandService.criar(comando(LocalTime.of(12, 15), LocalTime.of(14, 15)), USUARIO_MARIA, "Maria");

        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("reserva cancelada não bloqueia o horário")
    void reservaCanceladaNaoBloqueia() {
        prepararQuadraAberta();
        prepararUsuarioSemReservas();

        when(reservaRepository.findByQuadraIdAndDataAndStatus(1L, AMANHA, StatusReserva.ATIVA))
                .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> {
            Reserva salva = i.getArgument(0);
            salva.setId(12L);
            return salva;
        });
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_MARIA, "Maria");

        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("terceira reserva ativa do mesmo usuário é recusada")
    void terceiraReservaAtivaEhRecusada() {
        prepararQuadraAberta();
        when(
                reservaRepository.countByUsuarioIdAndStatusAndDataGreaterThanEqual(eq(USUARIO_JOAO), eq(StatusReserva.ATIVA), any())).thenReturn(2L);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO,
                "João"));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        assertEquals("LIMITE_DE_RESERVAS", erro.getCodigo());

        verify(reservaRepository, never())
                .findByQuadraIdAndDataAndStatus(anyLong(), any(), any());
    }

    @Test
    @DisplayName("com uma reserva ativa, a segunda ainda passa")
    void segundaReservaAindaPassa() {
        prepararQuadraAberta();
        when(
                reservaRepository.countByUsuarioIdAndStatusAndDataGreaterThanEqual(eq(USUARIO_JOAO), eq(StatusReserva.ATIVA), any())).thenReturn(1L);
        when(reservaRepository.findByQuadraIdAndDataAndStatus(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> {
            Reserva salva = i.getArgument(0);
            salva.setId(13L);
            return salva;
        });
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        reservaCommandService.criar(comando(LocalTime.of(10, 0), LocalTime.of(12, 0)), USUARIO_JOAO, "João");

        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("cancelamento por outro usuário sem ser ADMIN é bloqueado")
    void outroUsuarioNaoCancela() {
        Reserva doJoao = reservaExistente(5L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(doJoao));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.cancelar(new CancelarReservaCommand(5L, USUARIO_MARIA, "USUARIO")));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("ADMIN cancela reserva alheia")
    void adminCancelaReservaAlheia() {
        Reserva doJoao = reservaExistente(5L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(doJoao));
        when(reservaConsultaRepository.findById(5L)).thenReturn(Optional.of(consultaExistente(5L)));
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        ReservaConsulta resultado = reservaCommandService.cancelar(new CancelarReservaCommand(5L, 99L, "ADMIN"));

        assertEquals(StatusReserva.CANCELADA, doJoao.getStatus());
        assertEquals("CANCELADA", resultado.getStatus());
    }

    @Test
    @DisplayName("o dono cancela a própria reserva")
    void donoCancelaAPropria() {
        Reserva doJoao = reservaExistente(5L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(doJoao));
        when(reservaConsultaRepository.findById(5L)).thenReturn(Optional.of(consultaExistente(5L)));
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        reservaCommandService.cancelar(new CancelarReservaCommand(5L, USUARIO_JOAO, "USUARIO"));

        assertEquals(StatusReserva.CANCELADA, doJoao.getStatus());
    }

    @Test
    @DisplayName("o cancelamento atualiza as DUAS tabelas")
    void cancelamentoAtualizaAsDuasTabelas() {
        Reserva doJoao = reservaExistente(5L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        ReservaConsulta consulta = consultaExistente(5L);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(doJoao));
        when(reservaConsultaRepository.findById(5L)).thenReturn(Optional.of(consulta));
        when(reservaConsultaRepository.save(any(ReservaConsulta.class)))
                .thenAnswer(i -> i.getArgument(0));

        reservaCommandService.cancelar(new CancelarReservaCommand(5L, USUARIO_JOAO, "USUARIO"));

        verify(reservaRepository).save(doJoao);
        verify(reservaConsultaRepository).save(consulta);
        assertEquals("CANCELADA", consulta.getStatus());
    }

    @Test
    @DisplayName("cancelar uma reserva já cancelada é erro")
    void cancelarDuasVezesEhErro() {
        Reserva cancelada = reservaExistente(5L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        cancelada.setStatus(StatusReserva.CANCELADA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(cancelada));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.cancelar(new CancelarReservaCommand(5L, USUARIO_JOAO, "USUARIO")));

        assertEquals("OPERACAO_INVALIDA", erro.getCodigo());
    }

    @Test
    @DisplayName("cancelar reserva inexistente devolve 404")
    void cancelarInexistente() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> reservaCommandService.cancelar(new CancelarReservaCommand(99L, USUARIO_JOAO, "USUARIO")));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
        assertEquals("RESERVA_NAO_ENCONTRADA", erro.getCodigo());
    }

    private void prepararQuadraAberta() {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenReturn(
                        new QuadraDisponibilidadeDTO(1L, "Areia A", "AREIA", true, 15, true, LocalTime.of(7, 0), LocalTime.of(22, 0)));
    }

    private void prepararUsuarioSemReservas() {
        when(
                reservaRepository.countByUsuarioIdAndStatusAndDataGreaterThanEqual(anyLong(), eq(StatusReserva.ATIVA), any())).thenReturn(0L);
    }

    private CriarReservaCommand comando(LocalTime inicio, LocalTime fim) {
        return new CriarReservaCommand(1L, AMANHA, inicio, fim);
    }

    private Reserva reservaExistente(Long id, LocalTime inicio, LocalTime fim) {
        Reserva reserva = new Reserva(1L, USUARIO_JOAO, AMANHA, inicio, fim);
        reserva.setId(id);
        return reserva;
    }

    private ReservaConsulta consultaExistente(Long id) {
        return new ReservaConsulta(id, 1L, "Areia A", "AREIA", 15, USUARIO_JOAO, "João", AMANHA, LocalTime.of(10, 0),
                LocalTime.of(12, 0), "ATIVA");
    }
}
