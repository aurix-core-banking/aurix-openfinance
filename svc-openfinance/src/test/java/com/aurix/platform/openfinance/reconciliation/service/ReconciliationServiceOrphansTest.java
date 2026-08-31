package com.aurix.platform.openfinance.reconciliation.service;

import com.aurix.platform.openfinance.event.OpenFinanceEventPublisher;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationRecord;
import com.aurix.platform.openfinance.reconciliation.entity.ReconciliationStatus;
import com.aurix.platform.openfinance.reconciliation.repository.ReconciliationRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prova que a reconciliação pós-publicação não deixa órfãos (Gap 8):
 * reconcileAfterPublish é idempotente (não duplica registros do mesmo plan/node)
 * e a varredura repara registros abandonados em divergência.
 */
class ReconciliationServiceOrphansTest {

    @Test
    void deveReconciliarAposPublicacaoSemDuplicarRegistrosDoMesmoNode() {
        ReconciliationRecordRepository repository = mock(ReconciliationRecordRepository.class);
        OpenFinanceEventPublisher publisher = mock(OpenFinanceEventPublisher.class);

        when(repository.save(any(ReconciliationRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReconciliationService service = new ReconciliationService(repository, publisher);

        ReconciliationService.ReconciliationResult result = service.reconcileAfterPublish(
                "plan-1", Map.of("node-contas", new ReconciliationService.NodeCounts(10, 10)));

        assertFalse(result.isHasDivergences(), "extraído=publicado não deveria gerar divergência");
        assertEquals(1, result.getRecords().size());

        result = service.reconcileAfterPublish(
                "plan-1", Map.of("node-contas", new ReconciliationService.NodeCounts(10, 10)));

        assertFalse(result.isHasDivergences(), "segunda execução também não deveria divergir");
        assertEquals(1, result.getRecords().size(),
                "reconcileAfterPublish não deveria duplicar registros do mesmo node");
    }

    @Test
    void deveDetectarDivergenciaQuandoPublicadoMenorQueExtraido() {
        ReconciliationRecordRepository repository = mock(ReconciliationRecordRepository.class);
        OpenFinanceEventPublisher publisher = mock(OpenFinanceEventPublisher.class);

        when(repository.save(any(ReconciliationRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReconciliationService service = new ReconciliationService(repository, publisher);

        ReconciliationService.ReconciliationResult result = service.reconcileAfterPublish(
                "plan-2", Map.of("node-pix", new ReconciliationService.NodeCounts(100, 80)));

        assertTrue(result.isHasDivergences(), "publicado(80) < extraído(100) deveria divergir");
        ReconciliationRecord record = result.getRecords().get(0);
        assertEquals(ReconciliationStatus.DIVERGENCIA_DETECTADA, record.getStatus());
        assertEquals(80, record.getActualCount());
        assertEquals(100, record.getExpectedCount());
        verify(publisher).publishReconciliationDivergenceDetected(eq("plan-2"), eq("plan-2"), anyString());
    }

    @Test
    void deveRepararOrfaosEmDivergencia() {
        ReconciliationRecordRepository repository = mock(ReconciliationRecordRepository.class);
        OpenFinanceEventPublisher publisher = mock(OpenFinanceEventPublisher.class);

        ReconciliationRecord orfao = new ReconciliationRecord();
        orfao.setReconciliationId("recon-orphan-1");
        orfao.setPlanId("plan-3");
        orfao.setNodeId("node-x");
        orfao.setExpectedCount(50);
        orfao.setActualCount(30);
        orfao.setStatus(ReconciliationStatus.DIVERGENCIA_DETECTADA);
        orfao.setVersao(1);

        when(repository.findOrfaosByStatusAndOlderThan(anyList(), any()))
                .thenReturn(List.of(orfao));
        when(repository.findByReconciliationId("recon-orphan-1")).thenReturn(java.util.Optional.of(orfao));
        when(repository.save(any(ReconciliationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ReconciliationService service = new ReconciliationService(repository, publisher);

        int reparados = service.repararOrfaos(30, 5);

        assertEquals(1, reparados, "órfão em divergência deveria ser reparado");
        verify(repository).findOrfaosByStatusAndOlderThan(anyList(), any());
    }
}
