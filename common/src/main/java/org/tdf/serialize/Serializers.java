package org.tdf.serialize;

import org.tdf.common.Block;
import org.tdf.common.Header;
import org.tdf.common.HexBytes;
import org.tdf.common.Transaction;
import org.tdf.util.BufferUtil;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Serializers {
    /**
     * Converter from string to byte array and vice versa
     */
    public static final SerializeDeserializer<String> STRING = new SerializeDeserializer<String>() {
        @Override
        public String deserialize(byte[] data) {
            return new String(data, StandardCharsets.UTF_8);
        }

        @Override
        public byte[] serialize(String s) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
    };

    /**
     *  No conversion
     */
    public static final SerializeDeserializer<byte[]> IDENTITY = new SerializeDeserializer<byte[]>() {
        @Override
        public byte[] deserialize(byte[] data) {
            return data;
        }

        @Override
        public byte[] serialize(byte[] bytes) {
            return bytes;
        }
    };

    /**
     * Converter from transaction to byte array and vice versa
     */
    public static final SerializeDeserializer<Transaction> TRANSACTION = new SerializeDeserializer<Transaction>() {
        @Override
        public Transaction deserialize(byte[] data) {
            BufferUtil util = BufferUtil.newReadOnly(data);
            return Transaction.builder()
                    .version(util.getInt())
                    .type(util.getInt())
                    .createdAt(util.getLong())
                    .nonce(util.getLong())
                    .from(new HexBytes(util.getBytes()))
                    .gasPrice(util.getLong())
                    .amount(util.getLong())
                    .payload(new HexBytes(util.getBytes()))
                    .to(new HexBytes(util.getBytes()))
                    .signature(new HexBytes(util.getBytes()))
                    .build();
        }

        @Override
        public byte[] serialize(Transaction transaction) {
            BufferUtil util = BufferUtil.newWriteOnly();
            util.putInt(transaction.getVersion());
            util.putInt(transaction.getType());
            util.putLong(transaction.getCreatedAt());
            util.putLong(transaction.getNonce());
            util.putBytes(transaction.getFrom().getBytes());
            util.putLong(transaction.getGasPrice());
            util.putLong(transaction.getAmount());
            util.putBytes(transaction.getPayload().getBytes());
            util.putBytes(transaction.getTo().getBytes());
            util.putBytes(transaction.getSignature().getBytes());
            return util.toByteArray();
        }
    };

    /**
     * Converter from header to byte array and vice versa
     */
    public static final SerializeDeserializer<Header> HEADER = new SerializeDeserializer<Header>() {
        @Override
        public Header deserialize(byte[] data) {
            BufferUtil util = BufferUtil.newReadOnly(data);
            return Header.builder()
                    .version(util.getInt())
                    .hashPrev(new HexBytes(util.getBytes()))
                    .merkleRoot(new HexBytes(util.getBytes()))
                    .height(util.getLong())
                    .createdAt(util.getLong())
                    .payload(new HexBytes(util.getBytes()))
                    .build();
        }

        @Override
        public byte[] serialize(Header header) {
            BufferUtil util = BufferUtil.newWriteOnly();
            util.putInt(header.getVersion());
            util.putBytes(header.getHashPrev().getBytes());
            util.putBytes(header.getMerkleRoot().getBytes());
            util.putLong(header.getHeight());
            util.putLong(header.getCreatedAt());
            util.putBytes(header.getPayload().getBytes());
            return util.toByteArray();
        }
    };

    /**
     * Converter from block to byte array and vice versa
     */
    public static final SerializeDeserializer<Block> BLOCK = new SerializeDeserializer<Block>() {
        @Override
        public Block deserialize(byte[] data) {
            BufferUtil util = BufferUtil.newReadOnly(data);
            Header h = HEADER.deserialize(util.getBytes());
            Block b = new Block(h);
            StreamDeserializer<Transaction> deserializer = new StreamDeserializer<>(TRANSACTION);
            b.setBody(deserializer.deserialize(util.getRemained()).collect(Collectors.toList()));
            return b;
        }

        @Override
        public byte[] serialize(Block block) {
            BufferUtil util = BufferUtil.newWriteOnly();
            util.putBytes(HEADER.serialize(block.getHeader()));
            CollectionSerializer<Transaction> serializer = new CollectionSerializer<>(TRANSACTION);
            util.putBytes(serializer.serialize(block.getBody()));
            return util.toByteArray();
        }
    };


}
