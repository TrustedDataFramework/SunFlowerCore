package org.tdf.sunflower.consensus.pos

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.slf4j.LoggerFactory
import org.tdf.common.serialize.Codecs
import org.tdf.common.serialize.Codecs.newRLPCodec
import org.tdf.common.store.PrefixStore
import org.tdf.common.store.Store
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.common.util.RLPUtil
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.state.BuiltinContract
import org.tdf.sunflower.state.Constants
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.function.Predicate

class PosPreBuilt(private val nodes: Map<HexBytes, NodeInfo>) : BuiltinContract {
    lateinit var accountTrie: AccountTrie

    private fun getValue(stateRoot: HexBytes, key: HexBytes): HexBytes? {
        val a = accountTrie.get(stateRoot, Constants.POS_CONTRACT_ADDR)!!
        val db: Store<HexBytes, HexBytes> = accountTrie.contractStorageTrie.revert(a.storageRoot)
        return db[key]
    }

    fun getNodes(stateRoot: HexBytes): List<HexBytes> {
        val v = getValue(stateRoot, NODE_INFO_KEY)!!
        return RLPUtil.decode(v, Array<NodeInfo>::class.java).map { it.address }
    }

    fun getNodeInfos(stateRoot: HexBytes): List<NodeInfo> {
        val v = getValue(stateRoot, NODE_INFO_KEY)
        return RLPUtil.decode(v!!, Array<NodeInfo>::class.java).toList()

    }

    fun getVoteInfo(stateRoot: HexBytes, txHash: HexBytes): VoteInfo? {
        val a = accountTrie.get(stateRoot, Constants.POS_CONTRACT_ADDR)!!
        val db: Store<HexBytes, HexBytes> = accountTrie.contractStorageTrie.revert(a.storageRoot)
        val store = getVoteInfoStore(db)
        return store[txHash]
    }

    private fun getVoteInfoStore(contractStore: Store<HexBytes, HexBytes>): PrefixStore<HexBytes, VoteInfo> {
        return PrefixStore(
            contractStore,
            VOTE_INFO_KEY,
            Codecs.HEX,
            newRLPCodec(VoteInfo::class.java)
        )
    }

    override val address: HexBytes
        get() = Constants.POS_CONTRACT_ADDR
    override val genesisStorage: Map<HexBytes, HexBytes>
        get() {
            val map: MutableMap<HexBytes, HexBytes> = HashMap()
            val nodeInfos = nodes.values.toMutableList()
            nodeInfos.sortWith{ obj: NodeInfo, o: NodeInfo -> obj.compareTo(o) }
            map[NODE_INFO_KEY] = RLPUtil.encode(nodeInfos)
            map[VOTE_INFO_KEY] = HexBytes.fromBytes(Rlp.encodeElements())
            return map
        }

    override fun call(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData): ByteArray {
        val payload = callData.data
        val type = Type.values()[payload[0]]
        val args = payload.slice(1)
        val nodeInfos: MutableList<NodeInfo> =
            RLPUtil.decode(backend.dbGet(Constants.POS_CONTRACT_ADDR, NODE_INFO_KEY), Array<NodeInfo>::class.java)
                .toMutableList()


        val voteInfos = getVoteInfoStore(backend.getAsStore(Constants.POS_CONTRACT_ADDR))
        when (type) {
            Type.VOTE -> {
                if (callData.value.compareTo(Uint256.ZERO) == 0) throw RuntimeException("amount of vote cannot be 0 ")
                val (key, value) = findFirst(nodeInfos) { it.address == args }
                val n = if (key < 0) NodeInfo(args, Uint256.ZERO, ArrayList()) else value!!
                val n1 = n.copy(vote = n.vote + callData.value, txHash = n.txHash + ctx.txHash)
                if (key < 0) nodeInfos.add(n1) else nodeInfos[key] = n1
                voteInfos[ctx.txHash] = VoteInfo(
                    ctx.txHash, callData.caller,
                    args, callData.value
                )
            }
            Type.CANCEL_VOTE -> {
                if (callData.value != Uint256.ZERO)
                    throw RuntimeException("amount of cancel vote should be 0")

                val voteInfo = voteInfos[args] ?: throw RuntimeException("$args voting business does not exist and cannot be withdrawn")
                voteInfos.remove(args)
                if (voteInfo.from != callData.caller) {
                    throw RuntimeException("vote transaction from " + voteInfo.from + " not equals to " + callData.caller)
                }
                val (key, ninfo) = findFirst(nodeInfos) { it.address == voteInfo.to }
                if (ninfo == null) {
                    throw RuntimeException(voteInfo.to.toString() + " abnormal withdrawal of vote")
                }
                if (!ninfo.txHash.contains(args)) throw RuntimeException("vote $args not exists")
                val n1 = ninfo.copy(vote = ninfo.vote - voteInfo.amount, txHash = ninfo.txHash - args)
                if (ninfo.vote == Uint256.ZERO) {
                    nodeInfos.removeAt(key)
                } else {
                    nodeInfos[key] = n1
                }
                val callerBalance = backend.getBalance(callData.caller)
                backend.setBalance(callData.caller, callerBalance.plus(voteInfo.amount))
                val thisBalance = backend.getBalance(Constants.POS_CONTRACT_ADDR)
                backend.setBalance(Constants.POS_CONTRACT_ADDR, thisBalance.minus(voteInfo.amount))
            }
        }
        nodeInfos.sortWith { obj: NodeInfo, o: NodeInfo -> obj.compareTo(o) }
        nodeInfos.reverse()
        backend.dbSet(Constants.POS_CONTRACT_ADDR, NODE_INFO_KEY, RLPUtil.encode(nodeInfos))
        return ByteUtil.EMPTY_BYTE_ARRAY
    }

    override val abi: Abi
        get() = Abi.fromJson("[]")

    enum class Type {
        VOTE, CANCEL_VOTE
    }

    @RlpProps("address", "vote", "txHash")
    data class NodeInfo @RlpCreator constructor(
        val address: HexBytes = HexBytes.empty(),
        val vote: Uint256 = Uint256.ZERO,
        val txHash: List<HexBytes> = emptyList()
    ): Comparable<NodeInfo> {
        override fun compareTo(other: NodeInfo): Int {
            return vote.compareTo(other.vote)
        }
    }


    @RlpProps("txHash", "from", "to", "amount")
    class VoteInfo @RlpCreator constructor(
        val txHash: HexBytes = HexBytes.empty(),
        val from: HexBytes = HexBytes.empty(),
        val to: HexBytes = HexBytes.empty(),
        val amount: Uint256 = Uint256.ZERO
    ): Comparable<VoteInfo> {
        override fun compareTo(other: VoteInfo): Int {
            return txHash.compareTo(other.txHash)
        }
    }

    companion object {
        val NODE_INFO_KEY = "nodes".ascii()
        val VOTE_INFO_KEY = "votes".ascii()
        private val log = LoggerFactory.getLogger("pos")

        private fun <T> findFirst(c: List<T>, predicate: Predicate<T>): Map.Entry<Int, T?> {
            for (i in c.indices) {
                if (predicate.test(c[i])) {
                    return SimpleImmutableEntry(i, c[i])
                }
            }
            return SimpleImmutableEntry<Int, T?>(-1, null)
        }
    }
}


internal fun String.ascii(): HexBytes {
    return HexBytes.fromBytes(this.toByteArray(StandardCharsets.US_ASCII))
}
