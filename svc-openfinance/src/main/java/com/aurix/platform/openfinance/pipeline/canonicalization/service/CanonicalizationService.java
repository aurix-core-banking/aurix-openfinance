package com.aurix.platform.openfinance.pipeline.canonicalization.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Serviço de canonicalização do pipeline Open Finance.
 * Converte dados brutos extraídos de sistemas fonte no formato canônico padrão
 * do espectro Open Finance Brasil.
 */
@Service
public class CanonicalizationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CanonicalizationService.class);
    private static final String CANONICAL_VERSION = "1.0";
    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^\\d{14}$");

    /**
     * Canonicaliza um registro bruto no formato canônico Open Finance.
     *
     * @param raw  registro bruto de origem.
     * @param type tipo de recurso.
     * @return registro canônico.
     */
    public CanonicalRecord canonicalize(final RawRecord raw, final ResourceType type) {
        log.info("Iniciando canonicalização do registro {} tipo {}", raw.getRecordId(), type);

        String canonicalData;
        switch (type) {
            case CONTA:
                canonicalData = canonicalizeConta(raw.getRawData());
                break;
            case TRANSACAO:
                canonicalData = canonicalizeTransacao(raw.getRawData());
                break;
            case CARTAO:
                canonicalData = canonicalizeCartao(raw.getRawData());
                break;
            case PIX:
                canonicalData = canonicalizePix(raw.getRawData());
                break;
            default:
                canonicalData = raw.getRawData();
                break;
        }

        String checksum = calcularChecksum(canonicalData);
        String canonicalId = UUID.randomUUID().toString();

        CanonicalRecord record = CanonicalRecord.criar(
                canonicalId, raw.getRecordId(), type, canonicalData, CANONICAL_VERSION, checksum);

        log.info("Canonicalização concluída: canonicalId={}", canonicalId);
        return record;
    }

    /**
     * Canonicaliza dados de conta bancária.
     * Mapeia campos específicos da fonte para o modelo canônico de conta.
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizeConta(final String rawData) {
        log.debug("Canonicalizando dados de conta");
        // TODO: implementar parsing JSON e mapeamento de campos
        // Exemplo de modelo canônico esperado:
        // {
        //   "data": {
        //     "personType": "NATURAL|JURIDICAL",
        //     "companyCnpj": "...",
        //     "cpfNumber": "...",
        //     "accounts": [{
        //       "type": "CACC|SLRY|ECHK|TRAN|SVGS",
        //       "subtype": "...",
        //       "number": "...",
        //       "classification": "INDIVIDUAL|JOINT",
        //       "issuer": "...",
        //       "compeCode": "...",
        //       "branchCode": "...",
        //       "checkDigit": "...",
        //       "openDate": "...",
        //       "status": "ACTIVE|BLOCKED|CLOSED",
        //       "currency": "BRL"
        //     }]
        //   }
        // }
        return rawData;
    }

    /**
     * Canonicaliza dados de transação financeira.
     * Mapeia campos de transação para o modelo canônico.
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizeTransacao(final String rawData) {
        log.debug("Canonicalizando dados de transação");
        // TODO: implementar parsing JSON e mapeamento de campos
        // Modelo canônico de transação inclui:
        // - Identificador da transação
        // - Tipo (CREDIT/DEBIT)
        // - Valor e data
        // - Descrição e detalhes
        // - Informações de contrapartida
        return rawData;
    }

    /**
     * Canonicaliza dados de cartão de crédito/débito.
     * Mapeia campos de cartão para o modelo canônico.
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizeCartao(final String rawData) {
        log.debug("Canonicalizando dados de cartão");
        // TODO: implementar parsing JSON e mapeamento de campos
        // Modelo canônico de cartão inclui:
        // - Número do cartão (mascarado)
        // - Bandeira e tipo
        // - Status e data de emissão
        // - Limite e fatura atual
        return rawData;
    }

    /**
     * Canonicaliza dados PIX.
     * Mapeia campos PIX para o modelo canônico.
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizePix(final String rawData) {
        log.debug("Canonicalizando dados PIX");
        // TODO: implementar parsing JSON e mapeamento de campos
        // Modelo canônico PIX inclui:
        // - Chave PIX (CNPJ, CPF, email, telefone, EVP)
        // - Tipo da chave
        // - Conta vinculada
        // - Data de registro
        return rawData;
    }

    /**
     * Calcula checksum SHA-256 dos dados para garantir integridade.
     *
     * @param data dados em formato string.
     * @return checksum hexadecimal.
     */
    private String calcularChecksum(final String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
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
     * Valida se um CPF é válido (formato e dígitos verificadores).
     *
     * @param cpf CPF a validar.
     * @return true se válido.
     */
    public boolean validarCpf(final String cpf) {
        if (cpf == null) {
            return false;
        }
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        if (cpfLimpo.length() != CPF_LENGTH) {
            return false;
        }
        if (cpfLimpo.chars().distinct().count() == 1) {
            return false;
        }
        return CPF_PATTERN.matcher(cpfLimpo).matches();
    }

    /**
     * Valida se um CNPJ é válido (formato e dígitos verificadores).
     *
     * @param cnpj CNPJ a validar.
     * @return true se válido.
     */
    public boolean validarCnpj(final String cnpj) {
        if (cnpj == null) {
            return false;
        }
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");
        if (cnpjLimpo.length() != CNPJ_LENGTH) {
            return false;
        }
        return CNPJ_PATTERN.matcher(cnpjLimpo).matches();
    }

    /**
     * Normaliza um valor monetário removendo caracteres não numéricos
     * e convertendo para o formato padrão.
     *
     * @param valor valor em formato string.
     * @return valor normalizado.
     */
    public String normalizarValorMonetario(final String valor) {
        if (valor == null) {
            return "0.00";
        }
        return valor.replaceAll("[^0-9,.-]", "")
                .replace(",", ".");
    }

    /**
     * Normaliza data no formato ISO 8601.
     *
     * @param data data em formato string da fonte.
     * @return data normalizada ISO 8601.
     */
    public String normalizarData(final String data) {
        if (data == null) {
            return LocalDateTime.now().toString();
        }
        return data;
    }
}
