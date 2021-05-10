package org.tdf.sunflower.types;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPElement;
import org.tdf.rlp.RLPItem;
import org.tdf.rlp.RLPList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.tdf.common.util.ByteUtil.toHexString;

public class LogInfo {


    byte[] address = new byte[]{};
    List<Uint256> topics = new ArrayList<>();
    byte[] data = new byte[]{};

    public LogInfo(byte[] rlp) {

        RLPList params = RLPElement.fromEncoded(rlp).asRLPList();
        RLPList logInfo = (RLPList) params.get(0);

        RLPItem address = (RLPItem) logInfo.get(0);
        RLPList topics = (RLPList) logInfo.get(1);
        RLPItem data = (RLPItem) logInfo.get(2);

        this.address = address.asBytes() != null ? address.asBytes() : new byte[]{};
        this.data = data.asBytes() != null ? data.asBytes() : new byte[]{};

        for (RLPElement topic1 : topics) {
            byte[] topic = topic1.asBytes();
            this.topics.add(Uint256.of(topic));
        }
    }

    public LogInfo(byte[] address, List<Uint256> topics, byte[] data) {
        this.address = (address != null) ? address : new byte[]{};
        this.topics = (topics != null) ? topics : new ArrayList<Uint256>();
        this.data = (data != null) ? data : new byte[]{};
    }

    public byte[] getAddress() {
        return address;
    }

    public List<Uint256> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    /*  [address, [topic, topic ...] data] */
    public byte[] getEncoded() {

        byte[] addressEncoded = RLPCodec.encodeBytes(this.address);

        byte[][] topicsEncoded = null;
        if (topics != null) {
            topicsEncoded = new byte[topics.size()][];
            int i = 0;
            for (Uint256 topic : topics) {
                byte[] topicData = topic.getData();
                topicsEncoded[i] = RLPCodec.encodeBytes(topicData);
                ++i;
            }
        }

        byte[] dataEncoded = RLPCodec.encodeBytes(data);
        return RLPCodec.encodeElements(
            Arrays.asList(
                addressEncoded,
                RLPCodec.encodeElements(Arrays.asList(topicsEncoded)), dataEncoded
            )
        );
    }

    public Bloom getBloom() {
        Bloom ret = Bloom.create(HashUtil.sha3(address));
        for (Uint256 topic : topics) {
            byte[] topicData = topic.getData();
            ret.or(Bloom.create(HashUtil.sha3(topicData)));
        }
        return ret;
    }

    @Override
    public String toString() {

        StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");

        for (Uint256 topic : topics) {
            String topicStr = toHexString(topic.getData());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");


        return "LogInfo{" +
            "address=" + toHexString(address) +
            ", topics=" + topicsStr +
            ", data=" + toHexString(data) +
            '}';
    }
}

