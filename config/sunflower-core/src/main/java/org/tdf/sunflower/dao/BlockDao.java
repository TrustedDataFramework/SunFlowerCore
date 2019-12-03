package org.tdf.sunflower.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tdf.sunflower.entity.BlockEntity;

import java.util.Optional;

public interface BlockDao extends JpaRepository<BlockEntity, byte[]> {
    Optional<BlockEntity> findTopByOrderByHeightDesc();
    Optional<BlockEntity> findByHeight(long height);
}
