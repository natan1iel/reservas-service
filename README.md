# reservas-service

[![CI](https://github.com/natan1iel/reservas-service/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/natan1iel/reservas-service/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=natan1iel_reservas-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=natan1iel_reservas-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=natan1iel_reservas-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=natan1iel_reservas-service)

Agenda de reservas do **Sistema de Reserva de Quadras de Vôlei**. É aqui que vive o padrão CQRS e a regra central do sistema.

> A documentação geral do projeto — arquitetura, pipeline, ambientes e execução local — está no repositório [api-gateway](https://github.com/natan1iel/api-gateway).

---

## Requisitos implementados

**1. Consulta de horários livres** — recebe quadra e data, obtém a janela de funcionamento e o intervalo junto ao `quadras-service`, lê as reservas do dia e devolve os blocos vagos. É a tela principal do sistema.

**2. Criação validada contra o cadastro de quadras** — confirma que a quadra existe, está ativa e abre naquele dia, e que o horário cabe na janela. A duração é fixa de uma hora e o `horaFim` é calculado no servidor.

**3. Impedimento de sobreposição, considerando o intervalo** — regra central. Conflito retorna `409` com os dados da reserva que bloqueou.

**4. Cancelamento com controle de autoria** — apenas o autor ou um `ADMIN`. O cancelamento libera o horário na agenda.

## A regra de sobreposição

```java
conflita = inicioNovo.isBefore(fimExistente.plusMinutes(intervalo))
        && fimNovo.plusMinutes(intervalo).isAfter(inicioExistente);
```

Os sinais são estritos de propósito: com folga de 10 minutos, uma reserva das 10h às 11h e outra das 11h10 às 12h10 não conflitam, porque o intervalo foi respeitado exatamente. O valor do intervalo não é constante — vem do cadastro da quadra, definido pelo ADMIN.

É o trecho com maior densidade de testes do projeto, coberto por um teste parametrizado.

## CQRS

O serviço mantém dois caminhos separados sobre o mesmo banco:

| Tabela | Papel |
|---|---|
| `reserva` | **Escrita.** Normalizada, guarda apenas IDs. É contra ela que as regras são validadas. |
| `reserva_consulta` | **Leitura.** Desnormalizada, já com o código da quadra e o nome do usuário. |

Os campos desnormalizados vêm da chamada REST que o serviço já faz ao `quadras-service` no momento da criação. Aproveitando esses dados, a listagem de reservas nunca precisa chamar o outro serviço — e continua funcionando mesmo com ele fora do ar.

A sincronização entre as duas tabelas é síncrona, no mesmo método do comando. Não há broker de mensagens.

## Endpoints

| Método | Rota | Lado |
|---|---|---|
| GET | `/reservas/horarios-livres?quadraId=&data=` | Query |
| POST | `/reservas` | Command |
| DELETE | `/reservas/{id}` | Command |
| GET | `/reservas/minhas` | Query |

## Comunicação com o quadras-service

Única chamada entre microsserviços de domínio do sistema, feita pelo `QuadraClient` com `RestTemplate`:

```
GET {QUADRAS_SERVICE_URL}/quadras/{id}/disponibilidade?data=
```

Se o `quadras-service` estiver indisponível, a criação de reserva falha com `503` e mensagem clara, em vez de ficar pendurada.

## Ambientes

| Ambiente | Branch | URL |
|---|---|---|
| DEV | `develop` | https://reservas-service-dev.onrender.com |
| HOMOL | `main` | https://reservas-service-homol-devops.onrender.com |

As chamadas de verdade passam pelo gateway; essas URLs servem para o Swagger e o health check.

## Observabilidade

`/actuator/health` · `/actuator/prometheus` · `/swagger-ui.html` (habilitado por `SWAGGER_ENABLED`)

As métricas também são enviadas ao Grafana Cloud por OTLP, com as tags `application=reservas-service` e `ambiente`. Detalhes no [api-gateway](https://github.com/natan1iel/api-gateway#observabilidade).

## Variáveis de ambiente

`PORT` · `DB_URL` · `DB_USER` · `DB_PASSWORD` · `SWAGGER_ENABLED` · `QUADRAS_SERVICE_URL` · `AMBIENTE` · `OTLP_ENABLED` · `OTLP_URL` · `OTLP_AUTH`
