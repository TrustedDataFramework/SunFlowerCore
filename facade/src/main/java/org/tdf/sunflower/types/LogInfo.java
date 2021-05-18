package org.tdf.sunflower.types;

import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.rlpstream.Rlp;
import org.tdf.rlpstream.RlpEncodable;
import org.tdf.rlpstream.RlpList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.tdf.common.util.ByteUtil.toHexString;

public class LogInfo implements RlpEncodable {


    byte[] address = new byte[]{};
    List<Uint256> topics = new ArrayList<>();
    byte[] data = new byte[]{};

    public LogInfo(byte[] rlp) {
        RlpList params = Rlp.decodeList(rlp);
        RlpList logInfo = params.listAt(0);

        RlpList topics = logInfo.listAt(1);

        this.address = logInfo.bytesAt(0);
        this.data = logInfo.bytesAt(2);

        for(int i = 0; i < topics.size(); i++){
            byte[] topic = topics.bytesAt(i);
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

        byte[] addressEncoded = Rlp.encodeBytes(this.address);

        byte[][] topicsEncoded = null;
        if (topics != null) {
            topicsEncoded = new byte[topics.size()][];
            int i = 0;
            for (Uint256 topic : topics) {
                byte[] topicData = topic.getData();
                topicsEncoded[i] = Rlp.encodeBytes(topicData);
                ++i;
            }
        }

        byte[] dataEncoded = Rlp.encodeBytes(data);
        return Rlp.encodeElements(
            Arrays.asList(
                addressEncoded,
                Rlp.encodeElements(Arrays.asList(topicsEncoded)), dataEncoded
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

