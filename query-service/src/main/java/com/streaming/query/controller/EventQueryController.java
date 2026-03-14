package com.streaming.query.controller;

import com.streaming.query.dto.EventQueryResponse;
import com.streaming.query.service.EventQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para consulta de eventos processados.
 * Endpoints otimizados para leitura com suporte a paginação e cache Redis.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventQueryController {

    private final EventQueryService queryService;

    public EventQueryController(EventQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<Page<EventQueryResponse>> listEvents(
            @PageableDefault(size = 20, sort = "processedAt") Pageable pageable) {
        return ResponseEntity.ok(queryService.findAll(pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventQueryResponse> getEventById(@PathVariable UUID eventId) {
        return queryService.findById(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<Page<EventQueryResponse>> getByEventType(
            @PathVariable String eventType,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(queryService.findByEventType(eventType, pageable));
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<Page<EventQueryResponse>> getBySource(
            @PathVariable String source,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(queryService.findBySource(source, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<EventQueryResponse>> getByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(queryService.findByStatus(status, pageable));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<Page<EventQueryResponse>> getByPriority(
            @PathVariable String priority,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(queryService.findByPriority(priority, pageable));
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<EventQueryResponse>> getByCorrelationId(
            @PathVariable String correlationId) {
        List<EventQueryResponse> events = queryService.findByCorrelationId(correlationId);
        return events.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(events);
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<EventQueryResponse>> getByDateRange(
            @RequestParam Instant start,
            @RequestParam Instant end,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(queryService.findByDateRange(start, end, pageable));
    }
}
