package com.volei.reservas.query;

import com.volei.reservas.client.QuadraClient;
import com.volei.reservas.client.QuadraDisponibilidadeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaQueryServiceTest {
    private static final LocalDate DATA = LocalDate.of(2026, 9, 23);

    @Mock
    private ReservaConsultaRepository reservaConsultaRepository;

    @Mock
    private QuadraClient quadraClient;

    @InjectMocks
    private ReservaQueryService reservaQueryService;

    @Test
    @DisplayName("dia sem nenhuma reserva devolve um bloco único, da abertura ao fechamento")
    void diaVazio() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas();

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals(1, resposta.livres().size());
        assertEquals(LocalTime.of(8, 0), resposta.livres().get(0).inicio());
        assertEquals(LocalTime.of(18, 0), resposta.livres().get(0).fim());
    }

    @Test
    @DisplayName("uma reserva no meio divide o dia em dois blocos, descontando o intervalo")
    void umaReservaNoMeio() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas(consulta(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals(2, resposta.livres().size());
        assertEquals(LocalTime.of(8, 0), resposta.livres().get(0).inicio());
        assertEquals(LocalTime.of(9, 45), resposta.livres().get(0).fim());
        assertEquals(LocalTime.of(12, 15), resposta.livres().get(1).inicio());
        assertEquals(LocalTime.of(18, 0), resposta.livres().get(1).fim());
    }

    @Test
    @DisplayName("com intervalo zero, o bloco seguinte começa na hora exata do fim")
    void intervaloZero() {
        prepararQuadra(0, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas(consulta(LocalTime.of(10, 0), LocalTime.of(12, 0)));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals(LocalTime.of(10, 0), resposta.livres().get(0).fim());
        assertEquals(LocalTime.of(12, 0), resposta.livres().get(1).inicio());
    }

    @Test
    @DisplayName("dia em que a quadra não abre devolve lista vazia, não erro")
    void diaFechado() {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenReturn(new QuadraDisponibilidadeDTO(1L, "Areia A", "AREIA", true, 15, false, null, null));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertTrue(resposta.livres().isEmpty());
        assertNull(resposta.abertura());
        assertNull(resposta.fechamento());

        verify(reservaConsultaRepository, never())
                .findByQuadraIdAndDataAndStatusOrderByHoraInicio(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("sobra menor que 1 hora é descartada")
    void sobraCurtaEhDescartada() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas(consulta(LocalTime.of(8, 45), LocalTime.of(17, 0)));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertTrue(resposta.livres().isEmpty());
    }

    @Test
    @DisplayName("duas reservas seguidas deixam três blocos")
    void duasReservas() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas(consulta(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                consulta(LocalTime.of(14, 0), LocalTime.of(16, 0)));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals(3, resposta.livres().size());
        assertEquals(LocalTime.of(12, 15), resposta.livres().get(1).inicio());
        assertEquals(LocalTime.of(13, 45), resposta.livres().get(1).fim());
        assertEquals(LocalTime.of(16, 15), resposta.livres().get(2).inicio());
    }

    @Test
    @DisplayName("reserva colada na abertura elimina o bloco inicial")
    void reservaColadaNaAbertura() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas(consulta(LocalTime.of(8, 0), LocalTime.of(10, 0)));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals(1, resposta.livres().size());
        assertEquals(LocalTime.of(10, 15), resposta.livres().get(0).inicio());
    }

    @Test
    @DisplayName("uma reserva contida em outra não faz o cursor andar para trás")
    void reservaContidaEmOutra() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas(consulta(LocalTime.of(10, 0), LocalTime.of(16, 0)),
                consulta(LocalTime.of(11, 0), LocalTime.of(12, 0)));

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals(2, resposta.livres().size());
        assertEquals(LocalTime.of(8, 0), resposta.livres().get(0).inicio());
        assertEquals(LocalTime.of(9, 45), resposta.livres().get(0).fim());

        assertEquals(LocalTime.of(16, 15), resposta.livres().get(1).inicio());
        assertEquals(LocalTime.of(18, 0), resposta.livres().get(1).fim());
    }

    @Test
    @DisplayName("a resposta repete os dados da quadra para a tela montar o cabeçalho")
    void respostaTrazOsDadosDaQuadra() {
        prepararQuadra(15, LocalTime.of(8, 0), LocalTime.of(18, 0));
        prepararReservas();

        HorariosLivresDTO resposta = reservaQueryService.horariosLivres(1L, DATA);

        assertEquals("Areia A", resposta.quadra());
        assertEquals(15, resposta.intervaloMinutos());
        assertEquals(LocalTime.of(8, 0), resposta.abertura());
        assertEquals(LocalTime.of(18, 0), resposta.fechamento());
    }

    @Test
    @DisplayName("minhas reservas NÃO chama o quadras-service")
    void minhasReservasNaoChamaOutroServico() {
        when(reservaConsultaRepository.findByUsuarioIdOrderByDataDescHoraInicioDesc(2L))
                .thenReturn(List.of(consulta(LocalTime.of(10, 0), LocalTime.of(12, 0))));

        List<ReservaConsulta> minhas = reservaQueryService.minhasReservas(2L);

        assertEquals(1, minhas.size());
        assertEquals("Areia A", minhas.get(0).getQuadraCodigo());

        verify(quadraClient, never()).consultarDisponibilidade(anyLong(), any());
    }

    @Test
    @DisplayName("reservas da quadra devolvem ativas e canceladas")
    void reservasDaQuadra() {
        when(reservaConsultaRepository.findByQuadraIdAndDataOrderByHoraInicio(1L, DATA))
                .thenReturn(List.of(consulta(LocalTime.of(10, 0), LocalTime.of(12, 0))));

        assertEquals(1, reservaQueryService.reservasDaQuadra(1L, DATA).size());
    }

    private void prepararQuadra(int intervalo, LocalTime abertura, LocalTime fechamento) {
        when(quadraClient.consultarDisponibilidade(eq(1L), any()))
                .thenReturn(
                        new QuadraDisponibilidadeDTO(1L, "Areia A", "AREIA", true, intervalo, true, abertura, fechamento));
    }

    private void prepararReservas(ReservaConsulta... reservas) {
        when(
                reservaConsultaRepository.findByQuadraIdAndDataAndStatusOrderByHoraInicio(1L, DATA, "ATIVA")).thenReturn(List.of(reservas));
    }

    private ReservaConsulta consulta(LocalTime inicio, LocalTime fim) {
        return new ReservaConsulta(1L, 1L, "Areia A", "AREIA", 15, 2L, "João", DATA, inicio, fim, "ATIVA");
    }
}
