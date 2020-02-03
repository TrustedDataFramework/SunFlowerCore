package org.tdf.sunflower.dao;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.entity.HeaderEntity;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mapping {
    public static Header getFromHeaderEntity(HeaderEntity header) {
        return Header.builder()
                .hash(HexBytes.fromBytes(header.getHash()))
                .version(header.getVersion())
                .hashPrev(HexBytes.fromBytes(header.getHashPrev()))
                .transactionsRoot(HexBytes.fromBytes(header.getTransactionsRoot()))
                .stateRoot(HexBytes.fromBytes(header.getStateRoot()))
                .height(header.getHeight())
                .createdAt(header.getCreatedAt())
                .payload(HexBytes.fromBytes(header.getPayload()))
                .build();
    }

    public static List<Header> getFromHeaderEntities(Collection<? extends HeaderEntity> headers) {
        return headers.stream().map(Mapping::getFromHeaderEntity).collect(Collectors.toList());
    }

    public static Transaction getFromTransactionEntity(TransactionEntity transaction) {
        return Transaction.builder()
                .blockHash(HexBytes.fromBytes(transaction.getBlockHash()))
                .height(transaction.getHeight())
                .version(transaction.getVersion())
                .type(transaction.getType()).createdAt(transaction.getCreatedAt())
                .nonce(transaction.getNonce()).from(HexBytes.fromBytes(transaction.getFrom()))
                .gasPrice(transaction.getGasPrice()).amount(transaction.getAmount())
                .payload(HexBytes.fromBytes(transaction.getPayload())).to(HexBytes.fromBytes(transaction.getTo()))
                .signature(HexBytes.fromBytes(transaction.getSignature())).hash(HexBytes.fromBytes(transaction.getHash()))
                .build();
    }

    public static List<Transaction> getFromTransactionEntities(Collection<TransactionEntity> transactions) {
        return transactions.stream()
                .sorted((x, y) -> x.getPosition() - y.getPosition())
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }

    public static HeaderEntity getEntityFromHeader(Header header) {
        return HeaderEntity
                .builder().hash(header.getHash().getBytes())
                .version(header.getVersion())
                .hashPrev(header.getHashPrev().getBytes())
                .transactionsRoot(header.getTransactionsRoot().getBytes())
                .stateRoot(header.getStateRoot().getBytes())
                .height(header.getHeight())
                .createdAt(header.getCreatedAt())
                .payload(header.getPayload().getBytes()).build();
    }

    public static Stream<TransactionEntity> getTransactionEntitiesFromBlock(Block block) {
        Stream.Builder<TransactionEntity> builder = Stream.builder();
        for (int i = 0; i < block.getBody().size(); i++) {
            builder.accept(getEntityFromTransactionAndHeader(block.getHeader(), i, block.getBody().get(i)));
        }
        return builder.build();
    }

    public static TransactionEntity getEntityFromTransactionAndHeader(Header header, int index, Transaction tx) {
        return TransactionEntity.builder()
                .blockHash(header.getHash().getBytes())
                .height(tx.getHeight())
                .hash(tx.getHash().getBytes())
                .version(tx.getVersion())
                .type(tx.getType())
                .createdAt(tx.getCreatedAt())
                .nonce(tx.getNonce())
                .from(tx.getFrom().getBytes())
                .gasPrice(tx.getGasPrice())
                .amount(tx.getAmount())
                .payload(tx.getPayload().getBytes())
                .to(tx.getTo().getBytes())
                .signature(tx.getSignature().getBytes())
                .position(index).build();
    }
}
