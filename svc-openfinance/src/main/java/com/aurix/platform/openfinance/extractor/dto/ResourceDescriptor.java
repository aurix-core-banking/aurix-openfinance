package com.aurix.platform.openfinance.extractor.dto;

/**
 * Descritor de recurso a ser extraido.
 */
public class ResourceDescriptor {

    private final String resourceId;
    private final ResourceType resourceType;
    private final String resourcePath;

    public ResourceDescriptor(String resourceId, ResourceType resourceType, String resourcePath) {
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.resourcePath = resourcePath;
    }

    public String getResourceId() {
        return resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public static ResourceDescriptor of(String resourceId, ResourceType type) {
        return new ResourceDescriptor(resourceId, type, null);
    }

    public static ResourceDescriptor of(String resourceId, ResourceType type, String path) {
        return new ResourceDescriptor(resourceId, type, path);
    }
}
