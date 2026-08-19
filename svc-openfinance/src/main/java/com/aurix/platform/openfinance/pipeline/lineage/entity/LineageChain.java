package com.aurix.platform.openfinance.pipeline.lineage.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cadeia de linhagem completa de um dado no pipeline Open Finance.
 * Representa a trilha completa desde a publicação até o consentimento original.
 * INV03: Sem linhagem, sem publicação.
 */
public class LineageChain {

    /**
     * ID da publicação consultada.
     */
    private final String publicationId;

    /**
     * Cadeia ordenada de registros de linhagem (publicação → consentimento).
     */
    private final List<LineageRecord> records;

    /**
     * Indica se a cadeia está completa (todos os elos presentes).
     */
    private final boolean completa;

    /**
     * Construtor completo.
     *
     * @param publicationId ID da publicação.
     * @param records       cadeia de registros.
     * @param completa      se a cadeia está completa.
     */
    public LineageChain(final String publicationId, final List<LineageRecord> records,
            final boolean completa) {
        this.publicationId = publicationId;
        this.records = records != null
                ? Collections.unmodifiableList(new ArrayList<>(records))
                : Collections.emptyList();
        this.completa = completa;
    }

    /**
     * Cria uma cadeia de linhagem completa.
     *
     * @param publicationId ID da publicação.
     * @param records       cadeia de registros.
     * @return cadeia completa.
     */
    public static LineageChain completa(final String publicationId,
            final List<LineageRecord> records) {
        return new LineageChain(publicationId, records, true);
    }

    /**
     * Cria uma cadeia de linhagem incompleta.
     *
     * @param publicationId ID da publicação.
     * @param records       registros parciais.
     * @return cadeia incompleta.
     */
    public static LineageChain incompleta(final String publicationId,
            final List<LineageRecord> records) {
        return new LineageChain(publicationId, records, false);
    }

    /**
     * Cria uma cadeia vazia.
     *
     * @param publicationId ID da publicação.
     * @return cadeia vazia.
     */
    public static LineageChain vazia(final String publicationId) {
        return new LineageChain(publicationId, Collections.emptyList(), false);
    }

    /**
     * Retorna o ID da publicação.
     *
     * @return publicationId.
     */
    public String getPublicationId() {
        return publicationId;
    }

    /**
     * Retorna a cadeia de registros.
     *
     * @return lista imutável de LineageRecord.
     */
    public List<LineageRecord> getRecords() {
        return records;
    }

    /**
     * Retorna se a cadeia está completa.
     *
     * @return completa.
     */
    public boolean isCompleta() {
        return completa;
    }

    /**
     * Retorna o registro de consentimento (início da cadeia).
     *
     * @return primeiro registro ou null.
     */
    public LineageRecord getConsentimento() {
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * Retorna o registro de publicação (fim da cadeia).
     *
     * @return último registro ou null.
     */
    public LineageRecord getPublicacao() {
        return records.isEmpty() ? null : records.get(records.size() - 1);
    }

    /**
     * Retorna a profundidade da cadeia (número de elos).
     *
     * @return profundidade.
     */
    public int getProfundidade() {
        return records.size();
    }

    /**
     * Resume a cadeia em formato legível.
     *
     * @return resumo da cadeia.
     */
    public String resumir() {
        if (records.isEmpty()) {
            return "Cadeia vazia para publicação: " + publicationId;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Linhagem [").append(publicationId).append("]: ");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            LineageRecord r = records.get(i);
            sb.append("[").append(r.getConsentId());
            sb.append("/").append(r.getResourceId());
            sb.append("/").append(r.getCanonicalRecordId());
            sb.append("]");
        }
        sb.append(" (completa: ").append(completa).append(")");
        return sb.toString();
    }
}
