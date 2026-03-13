package com.streaming.events.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventStatus - Testes de Integridade")
class EventStatusTest {

    @Test
    @DisplayName("deve conter exatamente 10 status no ciclo de vida")
    void deveConterDezStatus() {
        assertThat(EventStatus.values()).hasSize(10);
    }

    @ParameterizedTest
    @EnumSource(EventStatus.class)
    @DisplayName("cada status deve ter descrição não vazia")
    void cadaStatusDeveTerDescricao(EventStatus status) {
        assertThat(status.getDescription()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("status terminais devem ser PERSISTED, NOTIFIED, REJECTED, DEAD_LETTER e RECOVERED")
    void statusTerminaisDevemEstarCorretos() {
        var terminais = Set.of(
                EventStatus.PERSISTED,
                EventStatus.NOTIFIED,
                EventStatus.REJECTED,
                EventStatus.DEAD_LETTER,
                EventStatus.RECOVERED
        );

        for (EventStatus status : EventStatus.values()) {
            if (terminais.contains(status)) {
                assertThat(status.isTerminal())
                        .as("Status %s deveria ser terminal", status.name())
                        .isTrue();
            } else {
                assertThat(status.isTerminal())
                        .as("Status %s não deveria ser terminal", status.name())
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("status do fluxo feliz devem estar na ordem correta")
    void fluxoFelizNaOrdemCorreta() {
        var fluxoFeliz = new EventStatus[]{
                EventStatus.RECEIVED,
                EventStatus.VALIDATED,
                EventStatus.ENRICHED,
                EventStatus.PROCESSED,
                EventStatus.PERSISTED,
                EventStatus.NOTIFIED
        };

        for (int i = 0; i < fluxoFeliz.length - 1; i++) {
            assertThat(fluxoFeliz[i].ordinal())
                    .as("Ordinal de %s deve ser menor que %s", fluxoFeliz[i], fluxoFeliz[i + 1])
                    .isLessThan(fluxoFeliz[i + 1].ordinal());
        }
    }

    @Test
    @DisplayName("deve resolver valueOf corretamente")
    void deveResolverValueOf() {
        assertThat(EventStatus.valueOf("DEAD_LETTER")).isEqualTo(EventStatus.DEAD_LETTER);
        assertThat(EventStatus.valueOf("RECOVERED")).isEqualTo(EventStatus.RECOVERED);
    }
}