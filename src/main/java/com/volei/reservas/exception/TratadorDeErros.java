package com.volei.reservas.exception;

import com.volei.reservas.dto.ErroResponse;
import com.volei.reservas.model.Reserva;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorDeErros {
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> regraDeNegocio(RegraDeNegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErroResponse(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(ConflitoDeHorarioException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoDeHorarioException e) {
        Reserva bloqueou = e.getConflitante();

        return ResponseEntity.status(
                HttpStatus.CONFLICT).body(new ErroResponse("CONFLITO_DE_HORARIO", e.getMessage(), new ErroResponse.ReservaConflitante(bloqueou.getId(), bloqueou.getHoraInicio(), bloqueou.getHoraFim())));
    }
}
