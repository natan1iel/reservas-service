package com.volei.reservas.client;

import com.volei.reservas.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Component
public class QuadraClient {
    private final RestTemplate restTemplate;
    private final String quadrasUrl;

    public QuadraClient(RestTemplate restTemplate, @Value("${quadras.service.url}") String quadrasUrl) {
        this.restTemplate = restTemplate;
        this.quadrasUrl = quadrasUrl;
    }

    public QuadraDisponibilidadeDTO consultarDisponibilidade(Long quadraId, LocalDate data) {
        String url = quadrasUrl + "/quadras/" + quadraId + "/disponibilidade?data=" + data;

        try {
            return restTemplate.getForObject(url, QuadraDisponibilidadeDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw RegraDeNegocioException.quadraNaoEncontrada(quadraId);
        } catch (RestClientException e) {
            throw RegraDeNegocioException.servicoIndisponivel(
                    "quadras-service indisponível: não é possível validar a reserva agora");
        }
    }
}
