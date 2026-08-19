package com.aurix.platform.openfinance.discovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "resource_edges")
public class ResourceEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String graphId;

    @Column(nullable = false)
    private String sourceNodeId;

    @Column(nullable = false)
    private String targetNodeId;

    @Column(nullable = false)
    private String edgeType;

    @Column(length = 2000)
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ResourceEdge() {
    }

    public ResourceEdge(String graphId, String sourceNodeId, String targetNodeId,
                        String edgeType, String metadata) {
        this.graphId = graphId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.edgeType = edgeType;
        this.metadata = metadata;
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

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
        ResourceEdge that = (ResourceEdge) o;
        return Objects.equals(graphId, that.graphId)
                && Objects.equals(sourceNodeId, that.sourceNodeId)
                && Objects.equals(targetNodeId, that.targetNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphId, sourceNodeId, targetNodeId);
    }

    @Override
    public String toString() {
        return "ResourceEdge{graphId='" + graphId + "', source='" + sourceNodeId
                + "', target='" + targetNodeId + "', type='" + edgeType + "'}";
    }
}
