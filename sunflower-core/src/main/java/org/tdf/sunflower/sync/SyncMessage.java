package org.tdf.sunflower.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Transaction;

import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
public class SyncMessage {
    public static final int UNKNOWN = 0;

    public static final int STATUS = 4;
    public static final int GET_BLOCKS = 5;
    public static final int BLOCKS = 6;
    public static final int PROPOSAL = 7;
    public static final int TRANSACTIONS = 8;

    public static byte[] encode(int code, Object msg){
        RLPList li = RLPElement.readRLPTree(msg).asRLPList();
        li.add(0, RLPElement.readRLPTree(code));
        return li.getEncoded();
    }

    public static Optional<SyncMessage> decode(byte[] rlp){
        RLPList li = RLPElement.fromEncoded(rlp).asRLPList();
        int code = li.get(0).asInt();
        if(code < STATUS) return Optional.empty();
        SyncMessage ret = new SyncMessage();
        ret.code = code;
        switch (code){
            case STATUS:
                ret.data = li.subList(1, li.size()).as(Status.class);
                return Optional.of(ret);
            case GET_BLOCKS:
                ret.data = li.subList(1, li.size()).as(GetBlocks.class);
                return Optional.of(ret);
            case BLOCKS:
                ret.data = li.subList(1, li.size()).as(Block[].class);
                return Optional.of(ret);
            case PROPOSAL:
                ret.data = li.subList(1, li.size()).as(Block.class);
                return Optional.of(ret);
            case TRANSACTIONS:
                ret.data = li.subList(1, li.size()).as(Transaction.class);
                return Optional.of(ret);
            default:
                return Optional.empty();
        }
    }

    @Getter@Setter
    private int code;

    private Object data;

    public <T> T getBodyAs(Class<T> clazz){
        return (T) this.data;
    }
}
