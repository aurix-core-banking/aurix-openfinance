package com.aurix.platform.openfinance.pipeline.canonicalization.service;

import com.aurix.platform.openfinance.pipeline.ResourceType;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.CanonicalRecord;
import com.aurix.platform.openfinance.pipeline.canonicalization.entity.RawRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Serviço de canonicalização do pipeline Open Finance.
 * Converte dados brutos extraídos de sistemas fonte no formato canônico padrão
 * definido em {@code architecture/data-models.yaml} (fonte de verdade do módulo).
 */
@Service
public class CanonicalizationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CanonicalizationService.class);
    private static final String CANONICAL_VERSION = "1.0";
    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^\\d{14}$");

    private static final Set<String> TIPOS_CONTA = Set.of("CONTA_CORRENTE", "POUPANCA", "SALARIO");
    private static final Set<String> STATUS_CONTA = Set.of("ATIVA", "INATIVA", "BLOQUEADA");
    private static final Set<String> TIPOS_TRANSACAO = Set.of("CREDITO", "DEBITO");
    private static final Set<String> BANDEIRAS_CARTAO = Set.of("MASTERCARD", "VISA", "ELO", "AMEX");
    private static final Set<String> STATUS_CARTAO = Set.of("ATIVO", "BLOQUEADO", "CANCELADO");
    private static final Set<String> STATUS_PIX = Set.of("PENDENTE", "LIQUIDADO", "DEVOLVIDO");

    private static final Map<String, String> SINONIMOS_TIPO_CONTA = Map.ofEntries(
            Map.entry("CORRENTE", "CONTA_CORRENTE"),
            Map.entry("CC", "CONTA_CORRENTE"),
            Map.entry("CONTA_CORRENTE", "CONTA_CORRENTE"),
            Map.entry("POUPANCA", "POUPANCA"),
            Map.entry("SALARIO", "SALARIO"));
    private static final Map<String, String> SINONIMOS_BANDEIRA = Map.ofEntries(
            Map.entry("MASTERCARD", "MASTERCARD"),
            Map.entry("MASTER", "MASTERCARD"),
            Map.entry("VISA", "VISA"),
            Map.entry("ELO", "ELO"),
            Map.entry("AMEX", "AMEX"),
            Map.entry("AMERICAN_EXPRESS", "AMEX"));

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * Canonicaliza dados de conta bancária conforme {@code data-models.yaml}
     * ({@code account}). Tolerante aos nomes de campos do core (camelCase ou
     * snake_case) e às enums da origem (ex.: tipoConta CORRENTE/CC).
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizeConta(final String rawData) {
        log.debug("Canonicalizando dados de conta");
        return mapearCampos(rawData, List.of(
                campo("accountId", "accountId", "idConta", "contaId", "id"),
                campo("documentNumber", "documentNumber", "cpfCnpj", "cpf", "cnpj", "documento", "cpfNumber", "companyCnpj"),
                campo("accountType", TipoCampo.ENUM, SINONIMOS_TIPO_CONTA, TIPOS_CONTA, "accountType", "tipoConta", "tipo", "contaTipo"),
                campo("agency", "agency", "agencia", "branchCode"),
                campo("number", "number", "numero", "numeroConta", "accountNumber"),
                campo("status", TipoCampo.ENUM, null, STATUS_CONTA, "status", "statusConta"),
                campo("createdAt", TipoCampo.DATA, "createdAt", "dataCriacao", "dataAbertura", "openDate", "dataAtualizacao")));
    }

    /**
     * Canonicaliza dados de transação financeira conforme {@code data-models.yaml}
     * ({@code transaction}).
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizeTransacao(final String rawData) {
        log.debug("Canonicalizando dados de transação");
        return mapearCampos(rawData, List.of(
                campo("transactionId", "transactionId", "idTransacao", "transacaoId", "id"),
                campo("accountId", "accountId", "contaId", "idConta"),
                campo("type", TipoCampo.ENUM, null, TIPOS_TRANSACAO, "type", "tipo", "tipoTransacao"),
                campo("amount", TipoCampo.DECIMAL, "amount", "valor", "value", "montante"),
                campo("description", "description", "descricao", "estabelecimento", "text"),
                campo("merchantCategoryCode", "merchantCategoryCode", "mcc", "categoriaEstabelecimento"),
                campo("timestamp", TipoCampo.DATA, "timestamp", "dataTransacao", "dataProcessamento", "data", "date")));
    }

    /**
     * Canonicaliza dados de cartão conforme {@code data-models.yaml}
     * ({@code creditCard}). O mascaramento do número é responsabilidade da
     * etapa de classificação PII do pipeline.
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizeCartao(final String rawData) {
        log.debug("Canonicalizando dados de cartão");
        return mapearCampos(rawData, List.of(
                campo("cardId", "cardId", "idCartao", "id"),
                campo("documentNumber", "documentNumber", "cpf", "cnpj", "documento"),
                campo("brand", TipoCampo.ENUM, SINONIMOS_BANDEIRA, BANDEIRAS_CARTAO, "brand", "bandeira", "brandName"),
                campo("cardNumber", "cardNumber", "numero", "numeroCartao", "pan"),
                campo("status", TipoCampo.ENUM, null, STATUS_CARTAO, "status", "statusCartao"),
                campo("creditLimit", TipoCampo.DECIMAL, "creditLimit", "limite", "limiteCredito", "limit"),
                campo("availableLimit", TipoCampo.DECIMAL, "availableLimit", "limiteDisponivel", "available", "disponivel")));
    }

    /**
     * Canonicaliza dados PIX conforme {@code data-models.yaml} ({@code pix}).
     *
     * @param rawData dados brutos JSON.
     * @return JSON canônico.
     */
    public String canonicalizePix(final String rawData) {
        log.debug("Canonicalizando dados PIX");
        return mapearCampos(rawData, List.of(
                campo("pixId", "pixId", "idPix", "id"),
                campo("endToEndId", "endToEndId", "e2eId", "endToEnd"),
                campo("payerAccountId", "payerAccountId", "contaOrigem", "payerAccount", "contaPagador", "idContaOrigem"),
                campo("payeeAccountId", "payeeAccountId", "contaDestino", "payeeAccount", "contaRecebedor", "idContaDestino"),
                campo("amount", TipoCampo.DECIMAL, "amount", "valor", "value"),
                campo("status", TipoCampo.ENUM, null, STATUS_PIX, "status", "situacao"),
                campo("timestamp", TipoCampo.DATA, "timestamp", "dataRegistro", "data", "createdAt")));
    }

    /**
     * Mapeia os campos presentes no JSON bruto para o modelo canônico,
     * resolvendo os valores pelas aliases (tolerantes a camelCase/snake_case)
     * e normalizando conforme o tipo do campo.
     *
     * @param rawDataJSON dados brutos JSON.
     * @param campos       definição dos campos canônicos.
     * @return JSON canônico array-escrito.
     */
    private String mapearCampos(final String rawDataJSON, final List<CampoCanonico> campos) {
        if (rawDataJSON == null || rawDataJSON.isBlank()) {
            return rawDataJSON;
        }
        try {
            JsonNode node = objectMapper.readTree(rawDataJSON);
            if (node != null && node.isArray() && node.size() > 0) {
                node = node.get(0);
            }
            if (node == null || !node.isObject()) {
                log.warn("Dados brutos não são um objeto JSON; mantendo conteúdo original: {}", abreviar(rawDataJSON));
                return rawDataJSON;
            }
            ObjectNode canonic = objectMapper.createObjectNode();
            for (CampoCanonico campo : campos) {
                JsonNode valor = null;
                for (String alias : campo.aliases) {
                    if (node.has(alias)) {
                        valor = node.get(alias);
                        break;
                    }
                    java.util.Iterator<Map.Entry<String, JsonNode>> camposBrutos = node.fields();
                    while (camposBrutos.hasNext()) {
                        Map.Entry<String, JsonNode> campoBruto = camposBrutos.next();
                        if (campoBruto.getKey().equalsIgnoreCase(alias)) {
                            valor = campoBruto.getValue();
                            break;
                        }
                    }
                    if (valor != null) {
                        break;
                    }
                }
                canonic.put(campo.nome, normalizarValor(campo, valor));
            }
            return objectMapper.writeValueAsString(canonic);
        } catch (Exception e) {
            log.warn("Falha ao canonicalizar dados brutos; mantendo conteúdo original: {}", e.getMessage());
            return rawDataJSON;
        }
    }

    /**
     * Normaliza o valor extraído conforme o tipo do campo canônico.
     *
     * @param campo definição do campo.
     * @param valor valor extraído (pode ser nulo/ausente).
     * @return valor normalizado como string ou null.
     */
    private String normalizarValor(final CampoCanonico campo, final JsonNode valor) {
        if (valor == null || valor.isNull()) {
            return null;
        }
        String texto;
        if (valor.isTextual()) {
            texto = valor.asText();
        } else if (valor.isNumber()) {
            texto = valor.asText();
        } else {
            texto = valor.toString();
        }
        if (texto.isBlank()) {
            return null;
        }
        switch (campo.tipo) {
            case DECIMAL:
                return normalizarValorMonetario(texto);
            case DATA:
                return normalizarData(texto);
            case ENUM:
                return normalizarEnum(texto, campo);
            default:
                return texto;
        }
    }

    /**
     * Normaliza um valor enum para o conjunto canônico, usando sinônimos
     * conhecidos e caindo para o valor normalizado original quando desconhecido.
     *
     * @param raw   valor bruto.
     * @param campo campo enum.
     * @return valor enum canônico.
     */
    private String normalizarEnum(final String raw, final CampoCanonico campo) {
        String normalizado = normalizarIdentificador(raw);
        if (campo.valoresCanonicos.contains(normalizado)) {
            return normalizado;
        }
        if (campo.sinonimos != null) {
            String sinonimo = campo.sinonimos.get(normalizado);
            if (sinonimo != null) {
                return sinonimo;
            }
        }
        return normalizado;
    }

    /**
     * Remove acentuação e normaliza espaços/símbolos em identificadores.
     *
     * @param valor texto a normalizar.
     * @return identificador em caixa alta com separadores vira underscore.
     */
    private String normalizarIdentificador(final String valor) {
        if (valor == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.trim()
                .replaceAll("[\\s\\-/.()]+", "_")
                .replaceAll("__+", "_")
                .toUpperCase(Locale.ROOT);
    }

    private CampoCanonico campo(final String nome, final String... aliases) {
        return new CampoCanonico(nome, TipoCampo.TEXTO, null, null, List.of(aliases));
    }

    private CampoCanonico campo(final String nome, final TipoCampo tipo,
            final Map<String, String> sinonimos, final Set<String> valoresCanonicos,
            final String... aliases) {
        return new CampoCanonico(nome, tipo, sinonimos, valoresCanonicos, List.of(aliases));
    }

    private CampoCanonico campo(final String nome, final TipoCampo tipo, final String... aliases) {
        return new CampoCanonico(nome, tipo, null, null, List.of(aliases));
    }

    private String abreviar(final String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
    }

    private enum TipoCampo {
        TEXTO, DECIMAL, DATA, ENUM
    }

    private record CampoCanonico(String nome, TipoCampo tipo,
            Map<String, String> sinonimos, Set<String> valoresCanonicos, List<String> aliases) {
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