package com.volei.reservas.command;

import com.volei.reservas.query.ReservaConsulta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservas")
public class ReservaCommandController {
    private final ReservaCommandService reservaCommandService;

    public ReservaCommandController(ReservaCommandService reservaCommandService) {
        this.reservaCommandService = reservaCommandService;
    }

    @PostMapping
    public ResponseEntity<ReservaConsulta> criar(@RequestHeader("X-Usuario-Id") Long usuarioId,
            @RequestHeader("X-Usuario-Nome") String usuarioNome, @RequestBody CriarReservaCommand comando) {
        ReservaConsulta criada = reservaCommandService.criar(comando, usuarioId, usuarioNome);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @DeleteMapping("/{id}")
    public ReservaConsulta cancelar(@RequestHeader("X-Usuario-Id") Long usuarioId,
            @RequestHeader(value = "X-Usuario-Papel", required = false) String papel, @PathVariable Long id) {
        return reservaCommandService.cancelar(new CancelarReservaCommand(id, usuarioId, papel));
    }
}
