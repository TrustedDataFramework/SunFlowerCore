package org.tdf.sunflower.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "tx_block_hash_index", columnList = TransactionEntity.COLUMN_BLOCK_HASH),
        @Index(name = "tx_block_height_index", columnList = TransactionEntity.COLUMN_BLOCK_HEIGHT),
        @Index(name = "tx_hash_index", columnList = TransactionEntity.COLUMN_TX_HASH),
        @Index(name = "tx_type_index", columnList = TransactionEntity.COLUMN_TX_TYPE),
        @Index(name = "tx_created_at_index", columnList = TransactionEntity.COLUMN_TX_CREATED_AT),
        @Index(name = "tx_nonce_index", columnList = TransactionEntity.COLUMN_TX_NONCE),
        @Index(name = "tx_from_index", columnList = TransactionEntity.COLUMN_TX_FROM),
        @Index(name = "tx_amount_index", columnList = TransactionEntity.COLUMN_TX_AMOUNT),
        @Index(name = "tx_to_index", columnList = TransactionEntity.COLUMN_TX_TO),
        @Index(name = "tx_position_index", columnList = TransactionEntity.COLUMN_TX_POSITION),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {
    static final String COLUMN_BLOCK_HASH = "tx_block_hash";
    static final String COLUMN_BLOCK_HEIGHT = "tx_block_height";
    static final String COLUMN_TX_HASH = "tx_hash";
    static final String COLUMN_TX_VERSION = "tx_version";
    static final String COLUMN_TX_TYPE = "tx_type";
    static final String COLUMN_TX_CREATED_AT = "tx_created_at";
    static final String COLUMN_TX_NONCE = "tx_nonce";
    static final String COLUMN_TX_FROM = "tx_from";
    static final String COLUMN_TX_GAS_PRICE = "tx_gas_price";
    static final String COLUMN_TX_AMOUNT = "tx_amount";
    static final String COLUMN_TX_PAYLOAD = "tx_payload";
    static final String COLUMN_TX_TO = "tx_to";
    static final String COLUMN_TX_SIGNATURE = "tx_signature";
    static final String COLUMN_TX_POSITION = "tx_position";

    @Column(name = COLUMN_BLOCK_HASH, nullable = false)
    private byte[] blockHash;

    @Column(name = COLUMN_BLOCK_HEIGHT, nullable = false)
    private long height;

    @Id
    @Column(name = COLUMN_TX_HASH, nullable = false)
    private byte[] hash;

    @Column(name = COLUMN_TX_VERSION, nullable = false)
    private int version;

    @Column(name = COLUMN_TX_TYPE, nullable = false)
    private int type;

    @Column(name = COLUMN_TX_CREATED_AT, nullable = false)
    private long createdAt;

    @Column(name = COLUMN_TX_NONCE, nullable = false)
    private long nonce;

    @Column(name = COLUMN_TX_FROM, nullable = false)
    private byte[] from;

    @Column(name = COLUMN_TX_GAS_PRICE, nullable = false)
    private long gasPrice;

    @Column(name = COLUMN_TX_AMOUNT, nullable = false)
    private long amount;

    @Column(name = COLUMN_TX_PAYLOAD, nullable = false, length = Short.MAX_VALUE)
    public byte[] payload;

    @Column(name = COLUMN_TX_TO, nullable = false)
    private byte[] to;

    @Column(name = COLUMN_TX_SIGNATURE, nullable = false)
    private byte[] signature;

    @Column(name = COLUMN_TX_POSITION, nullable = false)
    private int position;
}
