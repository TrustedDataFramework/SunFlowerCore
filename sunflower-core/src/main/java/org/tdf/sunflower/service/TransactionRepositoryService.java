package org.tdf.sunflower.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.tdf.sunflower.dao.Mapping;
import org.tdf.sunflower.dao.TransactionDao;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.facade.TransactionRepository;
import org.tdf.sunflower.types.Transaction;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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
    public List<Transaction> getTransactionsByBlockHash(byte[] blockHash) {
        return transactionDao.findByBlockHash(blockHash)
                .stream().sorted(Comparator.comparingInt(TransactionEntity::getPosition))
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> getTransactionsByBlockHeight(long height) {
        return transactionDao.findByHeight(height)
                .stream().sorted(Comparator.comparingInt(TransactionEntity::getPosition))
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }
}
