package com.aurix.platform.openfinance.discovery.controller;

import com.aurix.platform.openfinance.discovery.entity.ResourceGraph;
import com.aurix.platform.openfinance.discovery.entity.ResourceNode;
import com.aurix.platform.openfinance.discovery.service.ResourceDiscoveryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceDiscoveryController {

    private final ResourceDiscoveryService discoveryService;

    public ResourceDiscoveryController(ResourceDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @PostMapping("/discover")
    public ResponseEntity<ResourceGraph> discover(@RequestBody DiscoverRequest request) {
        ResourceGraph graph = discoveryService.discover(
                request.consentId,
                request.permissions,
                request.resourceMetadata
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(graph);
    }

    @GetMapping("/graph/{consentId}")
    public ResponseEntity<ResourceGraph> getGraph(@PathVariable String consentId) {
        return ResponseEntity.ok(discoveryService.getByConsentId(consentId));
    }

    @GetMapping("/graph/{consentId}/leaves")
    public ResponseEntity<List<ResourceNode>> getLeafNodes(
            @PathVariable String consentId) {
        ResourceGraph graph = discoveryService.getByConsentId(consentId);
        return ResponseEntity.ok(discoveryService.getLeafNodes(graph.getGraphId()));
    }

    @GetMapping("/graph/{consentId}/topo-sort")
    public ResponseEntity<List<String>> getTopologicalSort(
            @PathVariable String consentId) {
        ResourceGraph graph = discoveryService.getByConsentId(consentId);
        return ResponseEntity.ok(discoveryService.topologicalSort(graph.getGraphId()));
    }

    public static class DiscoverRequest {
        public String consentId;
        public List<String> permissions;
        public Map<String, String> resourceMetadata;
    }
}
