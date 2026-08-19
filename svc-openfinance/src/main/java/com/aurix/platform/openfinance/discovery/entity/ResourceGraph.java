package com.aurix.platform.openfinance.discovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "resource_graphs")
public class ResourceGraph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String graphId;

    @Column(nullable = false)
    private String consentId;

    @OneToMany(mappedBy = "graphId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ResourceNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "graphId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ResourceEdge> edges = new ArrayList<>();

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ResourceGraph() {
    }

    public ResourceGraph(String graphId, String consentId, int version) {
        this.graphId = graphId;
        this.consentId = consentId;
        this.version = version;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public List<ResourceNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ResourceNode> nodes) {
        this.nodes = nodes;
    }

    public List<ResourceEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<ResourceEdge> edges) {
        this.edges = edges;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceGraph that = (ResourceGraph) o;
        return Objects.equals(graphId, that.graphId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphId);
    }

    @Override
    public String toString() {
        return "ResourceGraph{graphId='" + graphId + "', consentId='" + consentId
                + "', nodes=" + (nodes != null ? nodes.size() : 0)
                + ", edges=" + (edges != null ? edges.size() : 0) + "}";
    }
}
