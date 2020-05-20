package org.tdf.common.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.Bytes;
import lombok.NonNull;
import org.apache.commons.codec.binary.Hex;
import org.tdf.common.serialize.HexBytesDecoder;
import org.tdf.common.serialize.HexBytesEncoder;
import org.tdf.rlp.RLPDecoding;
import org.tdf.rlp.RLPEncoding;

import java.io.Serializable;
import java.util.Arrays;

/**
 * hex bytes helper for json marshal/unmarshal
 * non-null immutable wrapper fo byte[] inspired by ByteArrayWrapper
 * non-null
 * <p>
 * HexBytes bytes = mapper.readValue("ffff", HexBytes.class);
 * String json = mapper.writeValueAsString(new HexBytes(new byte[32]));
 */
@JsonDeserialize(using = HexBytesUtils.HexBytesDeserializer.class)
@JsonSerialize(using = HexBytesUtils.HexBytesSerializer.class)
@RLPEncoding(HexBytesEncoder.class)
@RLPDecoding(HexBytesDecoder.class)
public final class HexBytes implements Comparable<HexBytes>, Serializable {

    public static final byte[] EMPTY_BYTES = new byte[0];
    // singleton zero value of HexBytes
    public static final HexBytes EMPTY = new HexBytes(EMPTY_BYTES);
    /**
     * generated by vscode
     */
    private static final long serialVersionUID = -1770889227783524732L;
    private final byte[] bytes;
    private final int hashCode;
    private String hexCache;

    private HexBytes(@NonNull byte[] bytes) {
        this.bytes = bytes;
        this.hashCode = Arrays.hashCode(bytes);
    }

    public static String encode(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    public static byte[] decode(@NonNull String hex) throws RuntimeException {
        try {
            return Hex.decodeHex(hex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HexBytes fromHex(@NonNull String hex) throws RuntimeException {
        try {
            return fromBytes(Hex.decodeHex(hex));
        } catch (Exception e) {
            throw new RuntimeException("invalid hex string " + hex);
        }
    }

    public static HexBytes fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return EMPTY;
        return new HexBytes(bytes);
    }

    public static HexBytes empty() {
        return EMPTY;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int size() {
        return bytes.length;
    }

    public boolean isEmpty() {
        return this == EMPTY || bytes.length == 0;
    }

    public String toString() {
        return toHex();
    }

    public String toHex() {
        if (hexCache != null) return hexCache;
        hexCache = Hex.encodeHexString(bytes);
        return hexCache;
    }

    public HexBytes concat(HexBytes another) {
        return new HexBytes(Bytes.concat(bytes, another.bytes));
    }

    public HexBytes slice(int start, int end) {
        if (isEmpty()) return EMPTY;
        while (start < 0) start += bytes.length;
        while (end < 0) end += bytes.length;
        return new HexBytes(Arrays.copyOfRange(bytes, start, end));
    }

    public HexBytes slice(int start) {
        return this.slice(start, bytes.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HexBytes hexBytes = (HexBytes) o;
        return FastByteComparisons.equal(bytes, hexBytes.bytes);
    }

    @Override
    public int compareTo(HexBytes o) {
        return FastByteComparisons.compareTo(
                bytes, 0, bytes.length,
                o.getBytes(), 0, o.getBytes().length);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
