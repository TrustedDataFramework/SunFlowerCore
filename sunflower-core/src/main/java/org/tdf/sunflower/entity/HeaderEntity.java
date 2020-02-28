package org.tdf.sunflower.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = HeaderEntity.TABLE_HEADER, indexes = {
        @Index(name = "block_hash_index", columnList = HeaderEntity.COLUMN_HASH),
        @Index(name = "hash_prev_index", columnList = HeaderEntity.COLUMN_HASH_PREV),
        @Index(name = "height_index", columnList = HeaderEntity.COLUMN_HEIGHT),
        @Index(name = "created_at_index", columnList = HeaderEntity.COLUMN_CREATED_AT)
})
public class HeaderEntity {
    static final String COLUMN_HASH = "block_hash";
    static final String COLUMN_VERSION = "version";
    static final String COLUMN_HASH_PREV = "hash_prev";
    static final String COLUMN_TRANSACTIONS_ROOT = "tx_root";
    static final String COLUMN_STATE_ROOT = "state_root";
    static final String COLUMN_HEIGHT = "block_height";
    static final String COLUMN_CREATED_AT = "created_at";
    static final String COLUMN_PAYLOAD = "payload";
    static final String TABLE_HEADER = "header";

    @Id
    @Column(name = COLUMN_HASH, nullable = false)
    private byte[] hash;

    @Column(name = COLUMN_VERSION, nullable = false)
    private int version;

    @Column(name = COLUMN_HASH_PREV, nullable = false)
    private byte[] hashPrev;

    @Column(name = COLUMN_TRANSACTIONS_ROOT, nullable = false)
    private byte[] transactionsRoot;

    @Column(name = COLUMN_STATE_ROOT, nullable = false)
    private byte[] stateRoot;

    @Column(name = COLUMN_HEIGHT, nullable = false)
    private long height;

    @Column(name = COLUMN_CREATED_AT, nullable = false)
    private long createdAt;

    @Column(name = COLUMN_PAYLOAD, nullable = false, length = Short.MAX_VALUE)
    private byte[] payload;
}