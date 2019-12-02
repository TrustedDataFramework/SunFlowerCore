package org.tdf.consortium.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wisdom.consortium.entity.Block;

import java.util.Optional;

public interface BlockDao extends JpaRepository<Block, byte[]> {
    Optional<Block> findTopByOrderByHeightDesc();
    Optional<Block> findByHeight(long height);
}
