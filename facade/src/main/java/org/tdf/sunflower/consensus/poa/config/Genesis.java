package org.tdf.sunflower.consensus.poa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.types.AbstractGenesis;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.Header;

import java.util.List;
import java.util.stream.Collectors;

public class Genesis extends AbstractGenesis {
    public Genesis(JsonNode parsed) {
        super(parsed);
    }

    private List<HexBytes> getAddressList(String field) {
        return getArray(field).stream()
            .map(n -> HexBytes.fromHex(n.get("addr").asText()))
            .collect(Collectors.toList());
    }

    public List<HexBytes> getMiners() {
        return getAddressList("miners");
    }

    public List<HexBytes> getValidators() {
        return getAddressList("validators");
    }

    @JsonIgnore
    public Block getBlock() {
        Header h = Header.builder()
            .gasLimit(
                getGasLimitHex()
            )
            .createdAt(getTimestamp())
            .build();

        return new Block(h);
    }
}
