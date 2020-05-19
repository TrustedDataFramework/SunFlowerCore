package org.tdf.sunflower.consensus.pow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.fraction.BigFraction;
import org.tdf.common.store.Store;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.types.Header;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "pow")
public class PoWBios implements Bios {
    static final byte[] N_BITS_KEY = "nbits".getBytes(StandardCharsets.US_ASCII);
    static final byte[] TIMESTAMPS_KEY = "ts".getBytes(StandardCharsets.US_ASCII);
    static final long MAX_ADJUST_RATE = 16;
    static final BigInteger MAX_UINT_256 = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    public static final HexBytes ADDRESS = Constants.POW_BIOS_ADDR;

    private final byte[] genesisNbits;

    private final PoWConfig config;

    public PoWBios(byte[] nbits, PoWConfig config) {
        this.genesisNbits = nbits;
        this.config = config;
    }

    @Override
    public Account getGenesisAccount() {
        return Account.emptyContract(ADDRESS);
    }

    @Override
    @SneakyThrows
    public void update(Header header, Store<byte[], byte[]> contractStorage) {
        List<Long> ts = new ArrayList<>(
                Arrays.asList(RLPCodec.decode(contractStorage.get(TIMESTAMPS_KEY).get(), Long[].class))
        );
        ts.add(header.getCreatedAt());
        log.debug("timestamps = {}", Start.MAPPER.writeValueAsString(ts));
        if (ts.size() == config.getBlocksPerEra()) {
            long duration = ts.get(ts.size() - 1) - ts.get(0);
            BigFraction rate = new BigFraction(
                    duration,
                    Long.valueOf(config.getBlockInterval() * (ts.size() - 1))
            );

            if (rate.compareTo(new BigFraction(MAX_ADJUST_RATE)) > 0) {
                rate = new BigFraction(MAX_ADJUST_RATE);
            }

            if (rate.compareTo(new BigFraction(1, MAX_ADJUST_RATE)) < 0) {
                rate = new BigFraction(1, MAX_ADJUST_RATE);
            }
            BigInteger nbits = BigEndian.decodeUint256(contractStorage.get(N_BITS_KEY).get());
            nbits = safeTyMul(nbits, rate);
            contractStorage.put(N_BITS_KEY, BigEndian.encodeUint256(nbits));
            contractStorage.put(TIMESTAMPS_KEY, RLPList.createEmpty().getEncoded());
        } else {
            contractStorage.put(TIMESTAMPS_KEY, RLPCodec.encode(ts));
        }
    }

    private BigInteger safeTyMul(BigInteger i, BigFraction f) {
        i = i.multiply(f.getNumerator());
        if (i.compareTo(MAX_UINT_256) > 0) {
            i = MAX_UINT_256;
        }
        return i.divide(f.getDenominator());
    }

    @Override
    public Map<byte[], byte[]> getGenesisStorage() {
        Map<byte[], byte[]> ret = new ByteArrayMap<>();
        ret.put(N_BITS_KEY, this.genesisNbits);
        ret.put(TIMESTAMPS_KEY, RLPList.createEmpty().getEncoded());
        return ret;
    }
}
