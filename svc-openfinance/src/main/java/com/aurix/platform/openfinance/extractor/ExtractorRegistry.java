package com.aurix.platform.openfinance.extractor;

import com.aurix.platform.openfinance.extractor.dto.ResourceType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de todos os extractors — resolve extractor para tipo de recurso.
 */
@Component
public class ExtractorRegistry {

    private final Map<ResourceType, DataExtractor> extractors = new ConcurrentHashMap<>();
    private final List<DataExtractor> extractorList;

    public ExtractorRegistry(List<DataExtractor> extractorList) {
        this.extractorList = extractorList;
    }

    @PostConstruct
    public void init() {
        for (DataExtractor extractor : extractorList) {
            ExtractorCapabilities caps = extractor.getCapabilities();
            for (ResourceType type : caps.getSupportedResourceTypes()) {
                extractors.put(type, extractor);
            }
        }
    }

    /**
     * Retorna o extractor para o tipo de recurso informado.
     */
    public Optional<DataExtractor> getExtractor(ResourceType type) {
        return Optional.ofNullable(extractors.get(type));
    }

    /**
     * Verifica se existe um extractor registrado para o tipo.
     */
    public boolean hasExtractor(ResourceType type) {
        return extractors.containsKey(type);
    }

    /**
     * Retorna todos os extractors registrados.
     */
    public List<DataExtractor> getAllExtractors() {
        return List.copyOf(extractorList);
    }
}
