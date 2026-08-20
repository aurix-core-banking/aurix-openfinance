package com.aurix.platform.openfinance.pipeline.pii.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult.PiiField;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult.PiiField.ProtectionStrategy;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que campos ENCRYPT usam AES/GCM real (via PiiEncryptor de aurix-shared),
 * não Base64 disfarçado de criptografia — decodificar o valor "criptografado" em
 * Base64 não deve devolver o CPF original em texto puro.
 */
@SpringBootTest
@ActiveProfiles("test")
class PiiClassificationServiceTest {

    @Autowired
    private PiiClassificationService piiClassificationService;

    @Test
    void deveCriptografarCpfDeVerdadeNaoApenasBase64() {
        String cpf = "123.456.789-01";
        String canonicalData = "{\"nome\":\"Fulano\",\"cpf\":\"" + cpf + "\"}";

        CanonicalRecord record = CanonicalRecord.criar(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                ResourceType.CONTA, canonicalData, "1.0", "checksum-fake-0000000000000000000000000000000000000000000000000000000000");

        PiiResult result = piiClassificationService.classify(record);

        PiiField cpfField = result.getProtectedFields().stream()
                .filter(f -> f.getPiiType() == PiiType.CPF)
                .findFirst()
                .orElseThrow();

        assertEquals(ProtectionStrategy.ENCRYPT, cpfField.getStrategy());
        assertNotEquals(cpf, cpfField.getProtectedValue());

        // O "ataque" que a simulação antiga (Base64 puro) não resistia:
        // decodificar o valor protegido em Base64 e reinterpretar como UTF-8
        // não deve devolver o CPF original.
        byte[] decoded = Base64.getDecoder().decode(cpfField.getProtectedValue());
        String asUtf8 = new String(decoded, StandardCharsets.UTF_8);
        assertNotEquals(cpf, asUtf8);
        assertTrue(decoded.length > cpf.getBytes(StandardCharsets.UTF_8).length,
                "AES/GCM real deve incluir IV + tag de autenticação, aumentando o tamanho");
    }
}
