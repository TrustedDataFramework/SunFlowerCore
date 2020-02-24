package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.NonNull;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.rlp.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RLPEncoding(UnmodifiableBlock.Encoder.class)
@RLPDecoding(UnmodifiableBlock.Decoder.class)
public class UnmodifiableBlock extends Block{
    public UnmodifiableBlock of(Block b){
        if(b instanceof UnmodifiableBlock) return ((UnmodifiableBlock) b);
        return new UnmodifiableBlock(b);
    }

    @JsonIgnore
    @Override
    public Header getHeader() {
        return super.getHeader();
    }

    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    public long getCreatedAt() {
        return header.getCreatedAt();
    }

    @Override
    public void setBody(List<Transaction> body) {
        throw new UnsupportedOperationException();
    }

    private UnmodifiableBlock(Block b){
        this.header = UnmodifiableHeader.of(b.getHeader());
        this.body = Collections.unmodifiableList(
                b.body.stream().map(UnmodifiableTransaction::of)
                .collect(Collectors.toList())
        );
    }

    static class Encoder implements RLPEncoder<UnmodifiableBlock> {
        @Override
        public RLPElement encode(@NonNull UnmodifiableBlock block) {
            return RLPElement.readRLPTree(new Object[]{block.header, block.body});
        }
    }

    static class Decoder implements RLPDecoder<UnmodifiableBlock> {
        @Override
        public UnmodifiableBlock decode(@NonNull RLPElement element) {
            return new UnmodifiableBlock(element.as(Block.class));
        }
    }
}
