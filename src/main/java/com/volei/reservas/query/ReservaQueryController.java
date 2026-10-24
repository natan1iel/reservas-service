package com.volei.reservas.query;

import com.volei.reservas.exception.RegraDeNegocioException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaQueryController {
    private final ReservaQueryService reservaQueryService;

    public ReservaQueryController(ReservaQueryService reservaQueryService) {
        this.reservaQueryService = reservaQueryService;
    }

    @GetMapping("/horarios-livres")
    public HorariosLivresDTO horariosLivres(@RequestParam Long quadraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return reservaQueryService.horariosLivres(quadraId, data);
    }

    @GetMapping("/minhas")
    public List<ReservaConsulta> minhas(@RequestHeader("X-Usuario-Id") Long usuarioId) {
        return reservaQueryService.minhasReservas(usuarioId);
    }

    @GetMapping("/quadra/{quadraId}")
    public List<ReservaConsulta> daQuadra(@RequestHeader(value = "X-Usuario-Papel", required = false) String papel,
            @PathVariable Long quadraId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        if (!"ADMIN".equals(papel)) {
            throw RegraDeNegocioException.negada("Apenas ADMIN pode ver as reservas de uma quadra");
        }

        return reservaQueryService.reservasDaQuadra(quadraId, data);
    }
}
