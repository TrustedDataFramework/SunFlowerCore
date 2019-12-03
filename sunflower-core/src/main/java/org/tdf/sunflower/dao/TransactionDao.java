package org.tdf.sunflower.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tdf.sunflower.entity.TransactionEntity;

import java.util.Collection;
import java.util.List;

public interface TransactionDao extends JpaRepository<TransactionEntity, byte[]> {
    List<TransactionEntity> findByBlockHashIn(Collection<byte[]> blockHashes);

    List<TransactionEntity> findByBlockHashOrderByPosition(byte[] blockHash, Pageable pageable);

    List<TransactionEntity> findByFromOrderByHeightAscPositionAsc(byte[] from, Pageable pageable);

    List<TransactionEntity> findByFromAndTypeOrderByHeightAscPositionAsc(byte[] from, int type, Pageable pageable);

    List<TransactionEntity> findByToOrderByHeightAscPositionAsc(byte[] to, Pageable pageable);

    List<TransactionEntity> findByToAndTypeOrderByHeightAscPositionAsc(byte[] to, int type, Pageable pageable);

    List<TransactionEntity> findByFromAndToOrderByHeightAscPositionAsc(byte[] from, byte[] to, Pageable pageable);

    List<TransactionEntity> findByFromAndToAndTypeOrderByHeightAscPositionAsc(byte[] from, byte[] to, int type, Pageable pageable);

    List<TransactionEntity> findByPayloadOrderByHeightAscPositionAsc(byte[] payload, Pageable pageable);

    List<TransactionEntity> findByPayloadAndTypeOrderByHeightAscPositionAsc(byte[] payload, int type, Pageable pageable);

    List<TransactionEntity> findByHeightOrderByPosition(long height, Pageable pageable);

    boolean existsByPayload(byte[] payload);
}
