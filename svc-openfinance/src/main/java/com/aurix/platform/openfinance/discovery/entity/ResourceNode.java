package com.aurix.platform.openfinance.discovery.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "resource_nodes")
public class ResourceNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nodeId;

    @Column(nullable = false)
    private String graphId;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false, length = 4000)
    private String capabilities;

    @Column(nullable = false, length = 4000)
    private String dependencies;

    @Column(nullable = false, length = 4000)
    private String metadata;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ResourceNode() {
    }

    public ResourceNode(String nodeId, String graphId, String resourceType,
                        String path, String capabilities, String dependencies,
                        String metadata) {
        this.nodeId = nodeId;
        this.graphId = graphId;
        this.resourceType = resourceType;
        this.path = path;
        this.capabilities = capabilities;
        this.dependencies = dependencies;
        this.metadata = metadata;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        ResourceNode that = (ResourceNode) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return "ResourceNode{nodeId='" + nodeId + "', resourceType='" + resourceType
                + "', path='" + path + "'}";
    }
}
