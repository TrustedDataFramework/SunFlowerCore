package org.tdf.sunflower.types;

import lombok.NonNull;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.*;

@RLPEncoding(UnmodifiableHeader.Encoder.class)
@RLPDecoding(UnmodifiableHeader.Decoder.class)
public class UnmodifiableHeader extends Header {
    private Header modifiable;

    private UnmodifiableHeader(Header h) {
        super(h.version, h.hashPrev, h.transactionsRoot, h.stateRoot, h.height, h.createdAt, h.payload);
        modifiable = new Header(version, hashPrev, transactionsRoot, stateRoot, height, createdAt, payload);
    }

    public UnmodifiableHeader(int version, HexBytes hashPrev, HexBytes transactionsRoot, HexBytes stateRoot, long height, long createdAt, HexBytes payload) {
        super(version, hashPrev, transactionsRoot, stateRoot, height, createdAt, payload);
        modifiable = new Header(version, hashPrev, transactionsRoot, stateRoot, height, createdAt, payload);
    }

    public static UnmodifiableHeader of(Header header) {
        if (header instanceof UnmodifiableHeader) return ((UnmodifiableHeader) header);
        return new UnmodifiableHeader(header);
    }

    @Override
    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHashPrev(HexBytes hashPrev) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransactionsRoot(HexBytes transactionsRoot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStateRoot(HexBytes stateRoot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeight(long height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCreatedAt(long createdAt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPayload(HexBytes payload) {
        throw new UnsupportedOperationException();
    }

    static class Encoder implements RLPEncoder<UnmodifiableHeader> {
        @Override
        public RLPElement encode(@NonNull UnmodifiableHeader unmodifiableHeader) {
            return RLPElement.readRLPTree(unmodifiableHeader.modifiable);
        }
    }

    static class Decoder implements RLPDecoder<UnmodifiableHeader> {
        @Override
        public UnmodifiableHeader decode(@NonNull RLPElement element) {
            return new UnmodifiableHeader(element.as(Header.class));
        }
    }
}
