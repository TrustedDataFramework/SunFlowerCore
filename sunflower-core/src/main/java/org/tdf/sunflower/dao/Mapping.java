package org.tdf.sunflower.dao;

import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.entity.BlockEntity;
import org.tdf.sunflower.entity.HeaderAdapter;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Mapping {
    public static Block getFromBlockEntity(BlockEntity block){
        Header header = getFromHeaderEntity(block);
        Block res = new Block(header);
        res.setBody(getFromTransactionEntities(block.getBody()));
        return res;
    }

    public static List<Block> getFromBlocksEntity(Collection<BlockEntity> blocks){
        return blocks.stream().map(Mapping::getFromBlockEntity).collect(Collectors.toList());
    }

    public static Header getFromHeaderEntity(HeaderAdapter header){
        return Header.builder()
                .hash(new HexBytes(header.getHash()))
                .version(header.getVersion())
                .hashPrev(new HexBytes(header.getHashPrev()))
                .merkleRoot(new HexBytes(header.getMerkleRoot()))
                .height(header.getHeight())
                .createdAt(header.getCreatedAt())
                .payload(new HexBytes(header.getPayload()))
                .build();
    }

    public static List<Header> getFromHeaderEntities(Collection<? extends HeaderAdapter> headers){
        return headers.stream().map(Mapping::getFromHeaderEntity).collect(Collectors.toList());
    }

    public static Transaction getFromTransactionEntity(TransactionEntity transaction){
        return Transaction.builder()
                .blockHash(new HexBytes(transaction.getBlockHash()))
                .height(transaction.getHeight())
                .version(transaction.getVersion())
                .type(transaction.getType()).createdAt(transaction.getCreatedAt())
                .nonce(transaction.getNonce()).from(new HexBytes(transaction.getFrom()))
                .gasPrice(transaction.getGasPrice()).amount(transaction.getAmount())
                .payload(new HexBytes(transaction.getPayload())).to(new HexBytes(transaction.getTo()))
                .signature(new HexBytes(transaction.getSignature())).hash(new HexBytes(transaction.getHash()))
                .build();
    }

    public static List<Transaction> getFromTransactionEntities(Collection<TransactionEntity> transactions){
        return transactions.stream()
                .sorted((x, y) -> x.getPosition() - y.getPosition())
                .map(Mapping::getFromTransactionEntity)
                .collect(Collectors.toList());
    }

    public static BlockEntity getEntityFromBlock(Block block){
        HeaderAdapter adapter = HeaderAdapter.builder().hash(block.getHash().getBytes())
                .version(block.getVersion())
                .hashPrev(block.getHashPrev().getBytes())
                .merkleRoot(block.getMerkleRoot().getBytes())
                .height(block.getHeight())
                .createdAt(block.getCreatedAt())
                .payload(block.getPayload().getBytes()).build();
        BlockEntity b = new BlockEntity(adapter);
        b.setBody(new ArrayList<>(block.getBody().size()));
        for(int i = 0; i < block.getBody().size(); i++){
            Transaction tx = block.getBody().get(i);
            TransactionEntity mapped = TransactionEntity.builder()
                    .blockHash(block.getHash().getBytes())
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
                    .position(i).build();
            b.getBody().add(mapped);
        }
        return b;
    }
}
