package org.tdf.sunflower.consensus.pow;

import com.github.salpadding.rlpstream.Rlp;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.*;
import org.tdf.sunflower.Start;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.facade.RepositoryService;
import org.tdf.sunflower.state.AbstractBuiltIn;
import org.tdf.sunflower.state.Account;
import org.tdf.sunflower.state.Constants;
import org.tdf.sunflower.state.StateTrie;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallContext;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.CallType;
import org.tdf.sunflower.vm.abi.Abi;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*
interface PowBios {
    function update();

    function nbits() external public returns (uint256);
}
 */
@Slf4j(topic = "pow")
public class PoWBios extends AbstractBuiltIn {
    public static final String ABI_JSON = "[{\"inputs\":[],\"name\":\"nbits\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"update\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    public static final Abi ABI = Abi.fromJson(ABI_JSON);
    public static final Abi.Function UPDATE = ABI.findFunction(f -> f.name.equals("update"));

    public static final HexBytes ADDRESS = Constants.POW_BIOS_ADDR;
    static final HexBytes N_BITS_KEY = HexBytes.fromBytes("nbits".getBytes(StandardCharsets.US_ASCII));
    static final HexBytes TIMESTAMPS_KEY = HexBytes.fromBytes("ts".getBytes(StandardCharsets.US_ASCII));
    static final long MAX_ADJUST_RATE = 16;
    static final BigInteger MAX_UINT_256 = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
    private final Uint256 genesisNbits;

    private final ConsensusConfig config;

    public PoWBios(HexBytes nbits, ConsensusConfig config,
                   StateTrie<HexBytes, Account> accounts,
                   RepositoryService repository) {
        super(Constants.POW_BIOS_ADDR, accounts, repository);
        this.genesisNbits = Uint256.of(nbits.getBytes());
        this.config = config;
    }

    public Uint256 getNBits(
        RepositoryReader rd,
        HexBytes parentHash
    ) {
        List<?> r = view(rd, parentHash, "nbits");
        return Uint256.of((BigInteger) r.get(0));
    }

    @Override
    @SneakyThrows
    public List<?> call(RepositoryReader rd, Backend backend, CallContext ctx, CallData callData, String method, Object... args) {


        if (method.equals("nbits")) {
            HexBytes nbits = backend.dbGet(Constants.POW_BIOS_ADDR, N_BITS_KEY);
            return Collections.singletonList(
                ByteUtil.bytesToBigInteger(nbits.getBytes())
            );
        }

        if (!method.equals("update")) {
            throw new RuntimeException("call to bios failed, method not found");
        }

        if (callData.getCallType() != CallType.COINBASE) {
            throw new RuntimeException("update should be called in coinbase transaction");
        }

        // find function by selector
        List<Long> ts = new ArrayList<>(
            Arrays.asList(
                RLPUtil.decode(
                    backend.dbGet(Constants.POW_BIOS_ADDR, TIMESTAMPS_KEY), Long[].class
                )
            )
        );
        log.debug("timestamps = {}", Start.MAPPER.writeValueAsString(ts));
        if (ts.size() == config.getBlocksPerEra()) {
            long x = ts.get(ts.size() - 1) - ts.get(0);
            long y = config.getBlockInterval() * (ts.size() - 1);

            // x > y * 16 -> x/y > 16
            if (x > y * MAX_ADJUST_RATE) {
                x = MAX_ADJUST_RATE;
                y = 1;
            }

            // x/y < 1 / 16 <=> x < y / 16 <=> 16 * x < y
            if (16 * x < y) {
                x = 1;
                y = MAX_ADJUST_RATE;
            }

            BigInteger nbits = ByteUtil.bytesToBigInteger(
                backend.dbGet(Constants.POW_BIOS_ADDR, N_BITS_KEY).getBytes()
            );

            nbits = safeTyMul(nbits, x, y);
            backend.dbSet(Constants.POW_BIOS_ADDR, N_BITS_KEY, HexBytes.fromBytes(BigIntegers.asUnsignedByteArray(nbits)));
            backend.dbSet(Constants.POW_BIOS_ADDR, TIMESTAMPS_KEY, HexBytes.fromBytes(Rlp.encodeElements()));
        } else {
            backend.dbSet(Constants.POW_BIOS_ADDR, TIMESTAMPS_KEY, RLPUtil.encode(ts));
        }

        throw new RuntimeException("pow");
    }

    @Override
    public Abi getAbi() {
        return ABI;
    }

    private BigInteger safeTyMul(BigInteger i, long x, long y) {
        i = i.multiply(BigInteger.valueOf(x));
        if (i.compareTo(MAX_UINT_256) > 0) {
            i = MAX_UINT_256;
        }
        return i.divide(BigInteger.valueOf(y));
    }

    @Override
    public Map<HexBytes, HexBytes> getGenesisStorage() {
        Map<HexBytes, HexBytes> ret = new HashMap<>();
        ret.put(N_BITS_KEY, HexBytes.fromBytes(genesisNbits.getData()));
        ret.put(TIMESTAMPS_KEY, HexBytes.fromBytes(Rlp.encodeElements()));
        return ret;
    }
}
