package com.volei.reservas.exception;

import org.springframework.http.HttpStatus;

public class RegraDeNegocioException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    private RegraDeNegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public static RegraDeNegocioException invalida(String mensagem) {
        return new RegraDeNegocioException(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", mensagem);
    }

    public static RegraDeNegocioException operacaoInvalida(String mensagem) {
        return new RegraDeNegocioException(HttpStatus.BAD_REQUEST, "OPERACAO_INVALIDA", mensagem);
    }

    public static RegraDeNegocioException negada(String mensagem) {
        return new RegraDeNegocioException(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", mensagem);
    }

    public static RegraDeNegocioException quadraNaoEncontrada(Long quadraId) {
        return new RegraDeNegocioException(HttpStatus.NOT_FOUND, "QUADRA_NAO_ENCONTRADA",
                "Quadra " + quadraId + " não existe no cadastro de quadras");
    }

    public static RegraDeNegocioException reservaNaoEncontrada(Long reservaId) {
        return new RegraDeNegocioException(HttpStatus.NOT_FOUND, "RESERVA_NAO_ENCONTRADA",
                "Reserva " + reservaId + " não encontrada");
    }

    public static RegraDeNegocioException limiteDeReservas(int limite) {
        return new RegraDeNegocioException(HttpStatus.CONFLICT, "LIMITE_DE_RESERVAS",
                "Você já tem " + limite + " reservas ativas. Cancele uma para poder reservar de novo");
    }

    public static RegraDeNegocioException servicoIndisponivel(String mensagem) {
        return new RegraDeNegocioException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICO_INDISPONIVEL", mensagem);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
