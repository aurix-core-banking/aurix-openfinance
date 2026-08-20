package com.aurix.platform.openfinance.pipeline.pii.service;

import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult.PiiField;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiResult.PiiField.ProtectionStrategy;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiType;
import com.aurix.platform.openfinance.pipeline.pii.entity.PiiType.SensitivityLevel;
import com.aurix.platform.shared.crypto.PiiEncryptor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de classificação e proteção de dados PII do pipeline Open Finance.
 * Identifica campos com dados pessoais identificáveis e aplica estratégias
 * de proteção conforme a sensibilidade.
 */
@Service
public class PiiClassificationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PiiClassificationService.class);

    private final PiiEncryptor piiEncryptor;

    public PiiClassificationService(final PiiEncryptor piiEncryptor) {
        this.piiEncryptor = piiEncryptor;
    }

    /**
     * Padrão regex para CPF: 11 dígitos.
     */
    private static final Pattern CPF_PATTERN = Pattern.compile(
            "\\b(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})\\b");

    /**
     * Padrão regex para CNPJ: 14 dígitos.
     */
    private static final Pattern CNPJ_PATTERN = Pattern.compile(
            "\\b(\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2})\\b");

    /**
     * Padrão regex para e-mail.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b");

    /**
     * Padrão regex para telefone brasileiro.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4})\\b");

    /**
     * Padrão regex para chave PIX EVP (UUID).
     private static final Pattern PIX_EVP_PATTERN = Pattern.compile(
            "\\b([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\b");
     */

    /**
     * Classifica e protege dados PII em um registro canônico.
     *
     * @param record registro canônico.
     * @return resultado da classificação PII.
     */
    public PiiResult classify(final CanonicalRecord record) {
        log.info("Classificando PII do registro {}", record.getCanonicalId());

        List<PiiField> detectedFields = detectPii(record);
        SensitivityLevel highestLevel = SensitivityLevel.PUBLIC;
        List<PiiField> protectedFields = new ArrayList<>();

        for (PiiField field : detectedFields) {
            ProtectionStrategy strategy = getProtectionStrategy(field.getPiiType());
            String masked = protegerCampo(field.getOriginalValue(), field.getPiiType(), strategy);
            PiiField protectedField = new PiiField(
                    field.getFieldName(), field.getPiiType(),
                    field.getOriginalValue(), masked, strategy);
            protectedFields.add(protectedField);

            if (field.getPiiType().getDefaultSensitivity().ordinal() > highestLevel.ordinal()) {
                highestLevel = field.getPiiType().getDefaultSensitivity();
            }
        }

        String maskedRecord = aplicarMascarasNoRegistro(record.getCanonicalData(), protectedFields);

        log.info("PII classificado: nível={}, campos={}", highestLevel, protectedFields.size());
        return PiiResult.criar(highestLevel, protectedFields, maskedRecord);
    }

    /**
     * Detecta campos PII no registro canônico.
     *
     * @param record registro canônico.
     * @return lista de campos PII detectados.
     */
    private List<PiiField> detectPii(final CanonicalRecord record) {
        List<PiiField> fields = new ArrayList<>();
        String data = record.getCanonicalData();
        if (data == null) {
            return fields;
        }

        Matcher cpfMatcher = CPF_PATTERN.matcher(data);
        while (cpfMatcher.find()) {
            String cpf = cpfMatcher.group(1).replaceAll("[^0-9]", "");
            if (cpf.length() == 11) {
                fields.add(new PiiField("cpf", PiiType.CPF, cpfMatcher.group(1), null, null));
            }
        }

        Matcher cnpjMatcher = CNPJ_PATTERN.matcher(data);
        while (cnpjMatcher.find()) {
            String cnpj = cnpjMatcher.group(1).replaceAll("[^0-9]", "");
            if (cnpj.length() == 14) {
                fields.add(new PiiField("cnpj", PiiType.CNPJ, cnpjMatcher.group(1), null, null));
            }
        }

        Matcher emailMatcher = EMAIL_PATTERN.matcher(data);
        while (emailMatcher.find()) {
            fields.add(new PiiField("email", PiiType.EMAIL, emailMatcher.group(1), null, null));
        }

        Matcher phoneMatcher = PHONE_PATTERN.matcher(data);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group(1).replaceAll("[^0-9]", "");
            if (phone.length() >= 10 && phone.length() <= 11) {
                fields.add(new PiiField("telefone", PiiType.PHONE, phoneMatcher.group(1), null, null));
            }
        }

        return fields;
    }

    /**
     * Determina a estratégia de proteção ideal para um tipo PII.
     *
     * @param piiType tipo PII.
     * @return estratégia de proteção.
     */
    private ProtectionStrategy getProtectionStrategy(final PiiType piiType) {
        switch (piiType) {
            case CPF:
            case CNPJ:
            case ACCOUNT_NUMBER:
            case PIX_KEY:
            case CARD_NUMBER:
                return ProtectionStrategy.ENCRYPT;
            case NAME:
                return ProtectionStrategy.MASK;
            case EMAIL:
                return ProtectionStrategy.HASH;
            case PHONE:
                return ProtectionStrategy.TOKENIZE;
            case ADDRESS:
                return ProtectionStrategy.MASK;
            case BIRTH_DATE:
                return ProtectionStrategy.MASK;
            case FILIATION:
                return ProtectionStrategy.HASH;
            default:
                return ProtectionStrategy.MASK;
        }
    }

    /**
     * Aplica proteção a um valor com base no tipo PII e estratégia.
     *
     * @param value    valor original.
     * @param piiType  tipo PII.
     * @param strategy estratégia de proteção.
     * @return valor protegido.
     */
    private String protegerCampo(final String value, final PiiType piiType,
            final ProtectionStrategy strategy) {
        if (value == null || value.isBlank()) {
            return value;
        }

        switch (strategy) {
            case MASK:
                return mask(value, piiType);
            case HASH:
                return hash(value);
            case ENCRYPT:
                return encrypt(value, piiType);
            case TOKENIZE:
                return tokenize(value, piiType);
            default:
                return mask(value, piiType);
        }
    }

    /**
     * Mascara um valor parcialmente.
     *
     * @param value   valor original.
     * @param piiType tipo PII.
     * @return valor mascarado.
     */
    private String mask(final String value, final PiiType piiType) {
        if (value == null || value.length() < 4) {
            return "***";
        }

        switch (piiType) {
            case CPF:
                // 123.456.789-01 → ***.***.***-01
                if (value.length() >= 6) {
                    return "***.***.***-" + value.substring(value.length() - 2);
                }
                return "***";
            case CNPJ:
                // 12.345.678/0001-90 → **.***.***/****-90
                if (value.length() >= 6) {
                    return "**.***.***/****-" + value.substring(value.length() - 2);
                }
                return "***";
            case NAME:
                // João da Silva → J*** d* S****
                String[] partes = value.split("\\s+");
                StringBuilder masked = new StringBuilder();
                for (int i = 0; i < partes.length; i++) {
                    if (i > 0) {
                        masked.append(" ");
                    }
                    if (partes[i].length() > 1) {
                        masked.append(partes[i].charAt(0));
                        for (int j = 1; j < partes[i].length(); j++) {
                            masked.append("*");
                        }
                    } else {
                        masked.append("*");
                    }
                }
                return masked.toString();
            case EMAIL:
                // joao@exemplo.com → j***@***.com
                int atIndex = value.indexOf('@');
                if (atIndex > 0) {
                    return value.charAt(0) + "***@" + value.substring(atIndex + 1);
                }
                return "***";
            case PHONE:
                // (11) 91234-5678 → (***) *****-5678
                if (value.length() >= 4) {
                    return "(**) *****-" + value.substring(value.length() - 4);
                }
                return "***";
            default:
                if (value.length() > 4) {
                    return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
                }
                return "***";
        }
    }

    /**
     * Aplica hash SHA-256 a um valor.
     *
     * @param value valor a hashear.
     * @return hash hexadecimal.
     */
    private String hash(final String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 não disponível", e);
        }
    }

    /**
     * Criptografa um valor com AES/GCM real via {@link PiiEncryptor} (aurix-shared) —
     * chave configurada em {@code aurix.security.encryption.key-base64}. Sem a chave
     * configurada, {@link PiiEncryptor} retorna o valor original (ver seu próprio log
     * de warning), nunca um Base64 disfarçado de criptografia.
     *
     * @param value   valor a criptografar.
     * @param piiType tipo PII.
     * @return valor criptografado.
     */
    private String encrypt(final String value, final PiiType piiType) {
        return piiEncryptor.encrypt(value);
    }

    /**
     * Tokeniza um valor substituindo por token único.
     *
     * @param value   valor a tokenizar.
     * @param piiType tipo PII.
     * @return token.
     */
    private String tokenize(final String value, final PiiType piiType) {
        String tokenPrefix = piiType.getCode() + ":";
        return tokenPrefix + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Aplica mascaras no registro JSON completo substituindo valores PII.
     *
     * @param recordData     dados do registro.
     * @param protectedFields campos protegidos.
     * @return registro com mascaras aplicadas.
     */
    private String aplicarMascarasNoRegistro(final String recordData, final List<PiiField> protectedFields) {
        if (recordData == null || protectedFields.isEmpty()) {
            return recordData;
        }

        String result = recordData;
        for (PiiField field : protectedFields) {
            if (field.getOriginalValue() != null && field.getProtectedValue() != null) {
                result = result.replace(field.getOriginalValue(), field.getProtectedValue());
            }
        }
        return result;
    }
}
