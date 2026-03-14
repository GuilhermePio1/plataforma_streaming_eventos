package com.streaming.ingestion.controller;

import com.streaming.ingestion.dto.EventRequest;
import com.streaming.ingestion.dto.EventResponse;
import com.streaming.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Controller REST para ingestão de eventos na plataforma.
 * Endpoint principal de entrada do sistema, recebendo eventos via HTTP POST.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventIngestionController {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionController.class);

    private final IngestionService ingestionService;

    public EventIngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Recebe um evento para ingestão na plataforma.
     * Valida o payload, cria o RawEvent e publica no Kafka de forma assíncrona.
     *
     * @param request corpo da requisição com os dados do evento
     * @return resposta com o identificador do evento e status de recebimento
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<EventResponse>> ingestEvent(@Valid @RequestBody EventRequest request) {
        log.info("Recebendo evento: source={}, type={}", request.source(), request.eventType());

        return ingestionService.ingest(request)
                .thenApply(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
    }
}
