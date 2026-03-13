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
        // Tratamento inline: se for nulo ou vazio, coloca uma mensagem padrão.
        // Se tiver conteúdo, faz o String.join normalmente.
        super(
                eventId,
                "Evento falhou na validação: " + (violations == null || violations.isEmpty() ? "Nenhum detalhe informado" : String.join("; ", violations)),
                false // isRetryable = false
        );

        // Cópia defensiva segura para o atributo da classe
        this.violations = (violations == null || violations.isEmpty())
                ? List.of()
                : List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
