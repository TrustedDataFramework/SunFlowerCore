package org.tdf.sunflower.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tdf.sunflower.entity.HeaderEntity;

import java.util.List;
import java.util.Optional;


public interface HeaderDao extends JpaRepository<HeaderEntity, byte[]> {
    List<HeaderEntity> findByHeightBetweenOrderByHeight(long start, long end);

    List<HeaderEntity> findByHeightBetweenOrderByHeightAsc(long start, long end, Pageable pageable);

    List<HeaderEntity> findByHeightBetweenOrderByHeightDesc(long start, long end, Pageable pageable);

    Optional<HeaderEntity> findTopByOrderByHeightDesc();

    List<HeaderEntity> findByHeightGreaterThanEqual(long height, Pageable pageable);

    Optional<HeaderEntity> findByHeight(long height);
}
