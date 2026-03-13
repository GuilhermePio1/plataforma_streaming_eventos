package com.streaming.events.exception;

import java.util.List;
import java.util.UUID;

/**
 * Exceção lançada quando um evento falha na validação de schema ou regras de negócio.
 * Sendo um erro de contrato, é considerado um erro FATAL (não deve sofrer retentativas).
 */
public class EventValidationException extends EventProcessingException {

    private final List<String> violations;

    public EventValidationException(UUID eventId, List<String> violations) {
        // Passamos 'false' para isRetryable, indicando que o erro deve ir direto para a DLQ
        super(eventId, "Evento falhou na validação: " + String.join(", ", violations), false);

        // Cópia defensiva
        this.violations = (violations == null || violations.isEmpty())
                ? List.of()
                : List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
