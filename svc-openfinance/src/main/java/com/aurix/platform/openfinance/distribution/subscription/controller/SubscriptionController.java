package com.aurix.platform.openfinance.distribution.subscription.controller;

import com.aurix.platform.openfinance.distribution.subscription.entity.Subscription;
import com.aurix.platform.openfinance.distribution.subscription.dto.SubscriptionRequest;
import com.aurix.platform.openfinance.distribution.subscription.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller de assinaturas.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    /**
     * Cria uma nova assinatura.
     */
    @PostMapping
    public ResponseEntity<Subscription> create(@RequestBody SubscriptionRequest request) {
        Subscription subscription = service.subscribe(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    /**
     * Lista assinaturas por participante.
     */
    @GetMapping
    public ResponseEntity<List<Subscription>> list(
            @RequestParam(required = false) String participantId,
            @RequestParam(required = false) String dataProductId) {
        List<Subscription> subscriptions;

        if (participantId != null) {
            subscriptions = service.listByParticipant(participantId);
        } else if (dataProductId != null) {
            subscriptions = service.listByDataProduct(dataProductId);
        } else {
            subscriptions = List.of();
        }

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Cancela uma assinatura.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        service.unsubscribe(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Rotaciona o segredo de assinatura de webhook — invalida o anterior.
     * O novo segredo só é exposto nesta resposta.
     */
    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<Subscription> rotateSecret(@PathVariable String id) {
        return ResponseEntity.ok(service.rotateSecret(id));
    }
}
