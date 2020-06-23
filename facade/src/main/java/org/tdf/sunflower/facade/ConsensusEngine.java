package org.tdf.sunflower.facade;

import com.fasterxml.jackson.databind.JsonNode;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.Block;
import org.tdf.sunflower.types.ValidateResult;

import java.util.*;

public interface ConsensusEngine {


    // the below getters shouldn't return null after initialized
    Miner getMiner();

    Validator getValidator();

    StateTrie<HexBytes, Account> getAccountTrie();

    Block getGenesisBlock();

    ConfirmedBlocksProvider getConfirmedBlocksProvider();

    PeerServerListener getPeerServerListener();

    default Optional<Set<HexBytes>> getApprovedNodes() {
        return Optional.empty();
    }

    // inject configurations, throw exception if configuration is invalid
    void init(Properties properties);

    String getName();

    default List<HexBytes> getMinerAddresses() {
        return Collections.emptyList();
    }

    default ValidateResult validateHeader(String header) {
        return ValidateResult.success();
    }

    default Object rpcQuery(HexBytes address, JsonNode body){
        throw new UnsupportedOperationException("unsupported yet");
    }
}
