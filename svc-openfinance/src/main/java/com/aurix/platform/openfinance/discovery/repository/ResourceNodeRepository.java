package com.aurix.platform.openfinance.discovery.repository;

import com.aurix.platform.openfinance.discovery.entity.ResourceNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceNodeRepository extends JpaRepository<ResourceNode, Long> {

    Optional<ResourceNode> findByNodeId(String nodeId);

    List<ResourceNode> findByGraphIdAndActiveTrue(String graphId);

    List<ResourceNode> findByGraphIdAndResourceTypeAndActiveTrue(
            String graphId, String resourceType);

    @Query("SELECT n FROM ResourceNode n WHERE n.graphId = :graphId " +
            "AND n.active = true AND n.dependencies = '[]'")
    List<ResourceNode> findLeafNodes(@Param("graphId") String graphId);

    @Query("SELECT n FROM ResourceNode n WHERE n.graphId = :graphId " +
            "AND n.active = true AND n.path LIKE CONCAT(:pathPrefix, '%')")
    List<ResourceNode> findByPathPrefix(
            @Param("graphId") String graphId, @Param("pathPrefix") String pathPrefix);
}
