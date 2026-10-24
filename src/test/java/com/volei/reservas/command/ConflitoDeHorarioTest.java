package com.volei.reservas.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConflitoDeHorarioTest {
    private static final LocalTime INICIO_EXISTENTE = LocalTime.of(10, 0);
    private static final LocalTime FIM_EXISTENTE = LocalTime.of(12, 0);

    @ParameterizedTest(name = "{3}: {0}-{1} com intervalo de {2}min -> conflito={4}")
    @CsvSource({
            "10:00, 12:00,  0, 'idêntica',                                      true",
            "09:00, 11:00,  0, 'invade o início',                               true",
            "11:00, 13:00,  0, 'invade o fim',                                  true",
            "10:30, 11:30,  0, 'contida na existente',                          true",
            "09:00, 13:00,  0, 'contém a existente',                            true",
            "12:00, 14:00,  0, 'encosta no fim, sem folga exigida',             false",
            "12:00, 14:00, 15, 'encosta no fim, mas invade a folga de 15min',   true",
            "12:15, 14:00, 15, 'respeita a folga exatamente',                   false",
            "08:00, 10:00, 15, 'encosta no início, invade a folga',             true",
            "07:45, 09:45, 15, 'respeita a folga antes',                        false",
            "14:00, 16:00, 15, 'distante, sem chance de conflito',              false",
            "10:00, 12:00, 30, 'idêntica, com folga grande',                    true"
    })
    void tabelaDeConflito(String inicioNovo, String fimNovo, int intervalo, String situacao, boolean esperado) {
        boolean resultado = ReservaCommandService.conflita(LocalTime.parse(inicioNovo), LocalTime.parse(fimNovo),
                INICIO_EXISTENTE, FIM_EXISTENTE, intervalo);

        assertEquals(esperado, resultado, situacao);
    }

    @DisplayName("a regra é simétrica: trocar nova por existente dá o mesmo resultado")
    @ParameterizedTest(name = "{0}-{1} com intervalo {2}")
    @CsvSource({
            "12:00, 14:00, 15",
            "12:15, 14:00, 15",
            "09:00, 11:00,  0",
            "14:00, 16:00, 15"
    })
    void regraEhSimetrica(String inicio, String fim, int intervalo) {
        LocalTime i = LocalTime.parse(inicio);
        LocalTime f = LocalTime.parse(fim);

        assertEquals(ReservaCommandService.conflita(i, f, INICIO_EXISTENTE, FIM_EXISTENTE, intervalo),
                ReservaCommandService.conflita(INICIO_EXISTENTE, FIM_EXISTENTE, i, f, intervalo));
    }
}
