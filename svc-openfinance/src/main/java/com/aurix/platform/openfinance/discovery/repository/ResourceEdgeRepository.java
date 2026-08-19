package com.aurix.platform.openfinance.discovery.repository;

import com.aurix.platform.openfinance.discovery.entity.ResourceEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceEdgeRepository extends JpaRepository<ResourceEdge, Long> {

    List<ResourceEdge> findByGraphId(String graphId);

    List<ResourceEdge> findByGraphIdAndSourceNodeId(String graphId, String sourceNodeId);

    List<ResourceEdge> findByGraphIdAndTargetNodeId(String graphId, String targetNodeId);

    void deleteByGraphId(String graphId);
}
