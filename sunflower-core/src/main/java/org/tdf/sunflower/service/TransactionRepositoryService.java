package org.tdf.sunflower.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.tdf.sunflower.dao.Mapping;
import org.tdf.sunflower.dao.TransactionDao;
import org.tdf.sunflower.facade.TransactionRepository;
import org.tdf.sunflower.types.Transaction;

import java.util.List;
import java.util.Optional;


@Service
public class TransactionRepositoryService implements TransactionRepository {
    @Autowired
    private TransactionDao transactionDao;

    @Override
    public boolean containsTransaction(@NonNull byte[] hash) {
        return transactionDao.existsById(hash);
    }


    @Override
    public Optional<Transaction> getTransactionByHash(byte[] hash) {
        return transactionDao.findById(hash).map(Mapping::getFromTransactionEntity);
    }

    @Override
    public List<Transaction> getTransactionsByBlockHash(byte[] blockHash, int page, int size) {
        return Mapping.getFromTransactionEntities(
                transactionDao.findByBlockHashOrderByPosition(blockHash, PageRequest.of(page, size))
        );
    }

    @Override
    public List<Transaction> getTransactionsByBlockHeight(long height, int page, int size) {
        return Mapping.getFromTransactionEntities(
                transactionDao.findByHeightOrderByPosition(height, PageRequest.of(page, size))
        );
    }
}
