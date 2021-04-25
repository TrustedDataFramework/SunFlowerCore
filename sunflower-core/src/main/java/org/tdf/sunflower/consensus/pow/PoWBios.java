package org.tdf.sunflower.consensus.pow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.fraction.BigFraction;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.BigEndian;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.rlp.RLPList;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Bios;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.vm.Backend;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "pow")
public class PoWBios implements Bios {
    public static final HexBytes ADDRESS = Constants.POW_BIOS_ADDR;
    static final byte[] N_BITS_KEY = "nbits".getBytes(StandardCharsets.US_ASCII);
    static final byte[] TIMESTAMPS_KEY = "ts".getBytes(StandardCharsets.US_ASCII);
    static final long MAX_ADJUST_RATE = 16;
    static final BigInteger MAX_UINT_256 = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    private final byte[] genesisNbits;

    private final PoWConfig config;

    public PoWBios(byte[] nbits, PoWConfig config) {
        this.genesisNbits = nbits;
        this.config = config;
    }

    @Override
    public Account getGenesisAccount() {
        return Account.emptyAccount(ADDRESS, Uint256.ZERO);
    }

    @Override
    @SneakyThrows
    public void update(Backend backend) {
        List<Long> ts = new ArrayList<>(
                Arrays.asList(
                        RLPCodec.decode(
                                backend.dbGet(Constants.POW_BIOS_ADDR, TIMESTAMPS_KEY), Long[].class
                        )
                )
        );
        ts.add(backend.getHeaderCreatedAt());
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
            BigInteger nbits = BigEndian.decodeUint256(backend.dbGet(Constants.POW_BIOS_ADDR, N_BITS_KEY));
            nbits = safeTyMul(nbits, rate);
            backend.dbSet(Constants.POW_BIOS_ADDR, N_BITS_KEY, BigEndian.encodeUint256(nbits));
            backend.dbSet(Constants.POW_BIOS_ADDR, TIMESTAMPS_KEY, RLPList.createEmpty().getEncoded());
        } else {
            backend.dbSet(Constants.POW_BIOS_ADDR, TIMESTAMPS_KEY, RLPCodec.encode(ts));
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
