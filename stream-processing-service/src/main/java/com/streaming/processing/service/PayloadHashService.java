package com.streaming.processing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Serviço para geração de hash de payload para controle de idempotência.
 * Utiliza SHA-256 para garantir unicidade com baixa probabilidade de colisão.
 */
@Service
public class PayloadHashService {

    private final ObjectMapper objectMapper;

    public PayloadHashService() {
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * Gera um hash SHA-256 do payload para controle de idempotência.
     */
    public String generateHash(Map<String, Object> payload) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(jsonBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash do payload", e);
        }
    }
}
