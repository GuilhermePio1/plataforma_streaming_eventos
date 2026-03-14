package com.streaming.processing.service;

import com.streaming.events.model.RawEvent;
import com.streaming.events.model.ValidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serviço de validação de eventos brutos.
 * Verifica regras de schema e negócio antes do processamento.
 */
@Service
public class EventValidationService {

    private static final Logger log = LoggerFactory.getLogger(EventValidationService.class);
    private static final String CURRENT_SCHEMA_VERSION = "1.0.0";

    /**
     * Valida um evento bruto e retorna o evento validado ou null se inválido.
     *
     * @param rawEvent evento bruto a ser validado
     * @return ValidatedEvent se válido, null se inválido
     */
    public ValidatedEvent validate(RawEvent rawEvent) {
        List<String> violations = new ArrayList<>();

        if (rawEvent.header().source() == null || rawEvent.header().source().isBlank()) {
            violations.add("Campo 'source' é obrigatório");
        }
        if (rawEvent.header().eventType() == null || rawEvent.header().eventType().isBlank()) {
            violations.add("Campo 'eventType' é obrigatório");
        }
        if (rawEvent.payload() == null || rawEvent.payload().isEmpty()) {
            violations.add("O payload não pode estar vazio");
        }

        if (!violations.isEmpty()) {
            log.warn("Evento inválido eventId={}: {}", rawEvent.header().eventId(), violations);
            return null;
        }

        log.debug("Evento validado com sucesso: eventId={}", rawEvent.header().eventId());
        return ValidatedEvent.from(rawEvent, CURRENT_SCHEMA_VERSION);
    }

    /**
     * Enriquece o payload do evento com metadados adicionais.
     */
    public ValidatedEvent validateAndEnrich(RawEvent rawEvent) {
        ValidatedEvent validated = validate(rawEvent);
        if (validated == null) {
            return null;
        }

        Map<String, Object> enrichedPayload = new java.util.HashMap<>(rawEvent.payload());
        enrichedPayload.put("_enriched", true);
        enrichedPayload.put("_enrichedAt", java.time.Instant.now().toString());

        return ValidatedEvent.fromEnriched(rawEvent, enrichedPayload, CURRENT_SCHEMA_VERSION);
    }
}
