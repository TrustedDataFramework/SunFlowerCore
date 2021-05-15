package org.tdf.sunflower.state;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.tdf.common.util.HexBytes;
import org.tdf.rlp.RLPCodec;
import org.tdf.sunflower.consensus.Proposer;
import org.tdf.sunflower.facade.RepositoryReader;
import org.tdf.sunflower.facade.RepositoryService;
import org.tdf.sunflower.types.ConsensusConfig;
import org.tdf.sunflower.types.Header;
import org.tdf.sunflower.types.StorageWrapper;
import org.tdf.sunflower.vm.Backend;
import org.tdf.sunflower.vm.CallData;
import org.tdf.sunflower.vm.abi.Abi;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * used for node join/exit
 */
public class Authentication extends AbstractBuiltIn {
    private static Optional<Proposer> getProposerInternal(Header parent, long currentEpochSeconds, List<HexBytes> minerAddresses, long blockInterval) {
        if (currentEpochSeconds - parent.getCreatedAt() < blockInterval) {
            return Optional.empty();
        }
        if (parent.getHeight() == 0) {
            return Optional.of(new Proposer(minerAddresses.get(0), 0, Long.MAX_VALUE));
        }

        HexBytes prev = parent.getCoinbase();

        int prevIndex = minerAddresses.indexOf(prev);

        if (prevIndex < 0)
            prevIndex += minerAddresses.size();

        long step = (currentEpochSeconds - parent.getCreatedAt())
            / blockInterval;

        int currentIndex = (int) ((prevIndex + step) % minerAddresses.size());
        long startTime = parent.getCreatedAt() + step * blockInterval;
        long endTime = startTime + blockInterval;

        return Optional.of(new Proposer(
            minerAddresses.get(currentIndex),
            startTime,
            endTime
        ));
    }

    public static final String ABI_JSON = "[{\"inputs\":[{\"internalType\":\"address\",\"name\":\"dst\",\"type\":\"address\"}],\"name\":\"approve\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"approved\",\"outputs\":[{\"internalType\":\"address[]\",\"name\":\"\",\"type\":\"address[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"exit\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"timestamp\",\"type\":\"uint256\"}],\"name\":\"getProposer\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"},{\"internalType\":\"uint256\",\"name\":\"start\",\"type\":\"uint256\"},{\"internalType\":\"uint256\",\"name\":\"end\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"join\",\"outputs\":[],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"dst\",\"type\":\"address\"}],\"name\":\"pending\",\"outputs\":[{\"internalType\":\"address[]\",\"name\":\"\",\"type\":\"address[]\"}],\"stateMutability\":\"view\",\"type\":\"function\"}]";
    public static final Abi ABI = Abi.fromJson(ABI_JSON);

    static final HexBytes NODES_KEY = HexBytes.fromBytes("nodes".getBytes(StandardCharsets.US_ASCII));
    static final HexBytes PENDING_NODES_KEY = HexBytes.fromBytes("pending".getBytes(StandardCharsets.US_ASCII));
    private final Collection<? extends HexBytes> nodes;
    private final ConsensusConfig config;

    public Authentication(
        @NonNull Collection<? extends HexBytes> nodes,
        @NonNull HexBytes contractAddress,
        StateTrie<HexBytes, Account> accounts,
        RepositoryService repo,
        ConsensusConfig config
    ) {
        super(contractAddress, accounts, repo);
        this.config = config;
        this.nodes = nodes;
    }

    static int divideAndCeil(int a, int b) {
        int ret = a / b;
        if (a % b != 0)
            return ret + 1;
        return ret;
    }


    @Override
    @SneakyThrows
    public List<?> call(Backend backend, CallData callData, String method, Object... args) {


        StorageWrapper wrapper = new StorageWrapper(backend.getAsStore(address));
        List<HexBytes> nodes = wrapper.getList(NODES_KEY, HexBytes.class, new ArrayList<>());

        StorageWrapper
            pending = new StorageWrapper(PENDING_NODES_KEY, backend.getAsStore(address));

        switch (method) {
            case "approved": {
                return Collections.singletonList(
                    nodes.stream().map(HexBytes::getBytes).toArray()
                );
            }
            case "pending": {
                byte[] dstBytes = (byte[]) args[0];
                HexBytes dst = HexBytes.fromBytes(dstBytes);
                Set<HexBytes> r = pending.getSet(dst, HexBytes.class, new TreeSet<>());
                if (r == null)
                    r = Collections.emptySet();
                return Collections.singletonList(
                    r.stream().map(HexBytes::getBytes).toArray()
                );
            }
            case "join": {
                HexBytes fromAddr = callData.getCaller();
                if (nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in nodes");

                Set<HexBytes> s = pending.getSet(fromAddr, HexBytes.class, null);
                if (s != null) {
                    throw new RuntimeException("authentication contract error: " + fromAddr + " has already in pending");
                }
                pending.save(fromAddr, new TreeSet<>());
                return Collections.emptyList();
            }
            case "approve": {
                byte[] toApproveBytes = (byte[]) args[0];
                HexBytes toApprove = HexBytes.fromBytes(toApproveBytes);

                if (callData.getTo().equals(Constants.VALIDATOR_CONTRACT_ADDR)) {
                    if (nodes.contains(toApprove))
                        return Collections.emptyList();
                    pending.remove(toApprove);
                    nodes.add(toApprove);
                    wrapper.save(NODES_KEY, nodes);
                    return Collections.emptyList();
                }

                if (!nodes.contains(callData.getCaller())) {
                    throw new RuntimeException("authentication contract error: cannot approve " + callData.getCaller() + " is not in nodes list");
                }

                Set<HexBytes> approves = pending.getSet(toApprove, HexBytes.class, null);
                if (approves == null)
                    throw new RuntimeException("authentication contract error: cannot approve " + toApprove + " not in pending");

                if (approves.contains(callData.getCaller())) {
                    throw new RuntimeException("authentication contract error: cannot approve " + toApprove + " has approved");
                }

                approves.add(callData.getCaller());

                if (approves.size() >= divideAndCeil(nodes.size() * 2, 3)) {
                    pending.remove(toApprove);
                    nodes.add(toApprove);
                } else {
                    pending.save(toApprove, approves);
                }

                wrapper.save(NODES_KEY, nodes);
                return Collections.emptyList();
            }
            case "exit": {

                HexBytes fromAddr = callData.getCaller();
                if (!nodes.contains(fromAddr))
                    throw new RuntimeException("authentication contract error: " + fromAddr + " not in nodes");
                if (nodes.size() <= 1)
                    throw new RuntimeException("authentication contract error: cannot exit, at least one miner");

                nodes.remove(fromAddr);
                wrapper.save(NODES_KEY, nodes);
                return Collections.emptyList();
            }
            case "getProposer": {
                try (RepositoryReader rd = repo.getReader()) {
                    Header parent = rd.getHeaderByHash(backend.getParentHash());
                    Optional<Proposer> o = Authentication.getProposerInternal(parent, ((BigInteger) args[0]).longValue(), nodes, this.config.getBlockInterval());
                    Proposer p = o.orElse(new Proposer(Address.empty(), 0, 0));
                    return Arrays.asList(
                        p.getAddress().getBytes(),
                        BigInteger.valueOf(p.getStartTimeStamp()),
                        BigInteger.valueOf(p.getEndTimeStamp())
                    );
                }
            }
            default:
                throw new RuntimeException("method not found");
        }
    }

    public Proposer getProposer(HexBytes parentHash, long now) {
        List<?> li = view(parentHash, "getProposer", BigInteger.valueOf(now));
        byte[] address = (byte[]) li.get(0);
        BigInteger start = (BigInteger) li.get(1);
        BigInteger end = (BigInteger) li.get(2);
        return new Proposer(
            HexBytes.fromBytes(address),
            start.longValue(),
            end.longValue()
        );
    }

    public List<HexBytes> getApproved(HexBytes parentHash) {
        List<?> li = view(parentHash, "approved");
        Object[] addresses = (Object[]) li.get(0);
        List<HexBytes> r = new ArrayList<>();

        for (Object bytes : addresses) {
            r.add(HexBytes.fromBytes((byte[]) bytes));
        }
        return r;
    }

    @Override
    public Abi getAbi() {
        return ABI;
    }

    @Override
    public Map<HexBytes, HexBytes> getGenesisStorage() {
        HashMap<HexBytes, HexBytes> ret = new HashMap<>();
        ret.put(NODES_KEY, HexBytes.fromBytes(RLPCodec.encode(this.nodes)));
        return ret;
    }

    public enum Method {
        JOIN_NODE,
        APPROVE_JOIN,
        EXIT
    }

}
