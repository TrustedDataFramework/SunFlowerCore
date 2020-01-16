package org.tdf.sunflower.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tdf.sunflower.entity.TransactionEntity;

import java.util.Collection;
import java.util.List;

public interface TransactionDao extends JpaRepository<TransactionEntity, byte[]> {
    List<TransactionEntity> findByBlockHashIn(Collection<byte[]> blockHashes);

    List<TransactionEntity> findByBlockHash(byte[] blockHash);

    List<TransactionEntity> findByHeight(long height);
}
