package org.tdf.sunflower.dao;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.entity.HeaderEntity;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.types.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mapping {
    public static Header getFromHeaderEntity(HeaderEntity header) {
//        Header ret = Header.builder()
//                .version(header.getVersion())
//                .hashPrev(HexBytes.fromBytes(header.getHashPrev()))
//                .transactionsRoot(HexBytes.fromBytes(header.getTransactionsRoot()))
//                .stateRoot(HexBytes.fromBytes(header.getStateRoot()))
//                .height(header.getHeight())
//                .createdAt(header.getCreatedAt())
//                .payload(HexBytes.fromBytes(header.getPayload()))
//                .build();
        return null;
    }

    public static List<Header> getFromHeaderEntities(Collection<? extends HeaderEntity> headers) {
        return headers.stream().map(Mapping::getFromHeaderEntity).collect(Collectors.toList());
    }

    public static Transaction getFromTransactionEntity(TransactionEntity transaction) {
        return null;
    }

    public static List<Transaction> getFromTransactionEntities(Collection<TransactionEntity> transactions) {
        return transactions.stream()
                .sorted((x, y) -> x.getPosition() - y.getPosition())
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }

    public static HeaderEntity getEntityFromHeader(Header header) {
//        return HeaderEntity
//                .builder().hash(header.getHash().getBytes())
//                .version(header.getVersion())
//                .hashPrev(header.getHashPrev().getBytes())
//                .transactionsRoot(header.getTransactionsRoot().getBytes())
//                .stateRoot(header.getStateRoot().getBytes())
//                .height(header.getHeight())
//                .createdAt(header.getCreatedAt())
//                .payload(header.getPayload().getBytes()).build();
        return null;
    }

    public static Stream<TransactionEntity> getTransactionEntitiesFromBlock(Block block) {
        Stream.Builder<TransactionEntity> builder = Stream.builder();
        for (int i = 0; i < block.getBody().size(); i++) {
            builder.accept(getEntityFromTransactionAndHeader(block.getHeader(), i, block.getBody().get(i)));
        }
        return builder.build();
    }

    public static TransactionEntity getEntityFromTransactionAndHeader(Header header, int index, Transaction tx) {
        return null;
    }
}
