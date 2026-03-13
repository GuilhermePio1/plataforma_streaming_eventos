package com.streaming.events.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventPriority - Testes de Integridade")
class EventPriorityTest {

    @Test
    @DisplayName("deve conter exatamente 4 valores de prioridade")
    void deveConterQuatroValores() {
        assertThat(EventPriority.values()).hasSize(4);
    }

    @Test
    @DisplayName("os níveis devem ser crescentes de LOW a CRITICAL")
    void niveisDevemSerCrescentes() {
        assertThat(EventPriority.LOW.getLevel()).isLessThan(EventPriority.NORMAL.getLevel());
        assertThat(EventPriority.NORMAL.getLevel()).isLessThan(EventPriority.HIGH.getLevel());
        assertThat(EventPriority.HIGH.getLevel()).isLessThan(EventPriority.CRITICAL.getLevel());
    }

    @ParameterizedTest
    @EnumSource(EventPriority.class)
    @DisplayName("cada prioridade deve ter descrição não vazia")
    void cadaPrioridadeDeveTerDescricao(EventPriority priority) {
        assertThat(priority.getDescription()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("LOW e NORMAL não devem exigir alerta imediato")
    void lowENormalNaoDevemExigirAlerta() {
        assertThat(EventPriority.LOW.requiresImmediateAlert()).isFalse();
        assertThat(EventPriority.NORMAL.requiresImmediateAlert()).isFalse();
    }

    @Test
    @DisplayName("HIGH e CRITICAL devem exigir alerta imediato")
    void highECriticalDevemExigirAlerta() {
        assertThat(EventPriority.HIGH.requiresImmediateAlert()).isTrue();
        assertThat(EventPriority.CRITICAL.requiresImmediateAlert()).isTrue();
    }
}