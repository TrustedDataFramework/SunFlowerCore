package org.tdf.sunflower.controller

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.IntSerializer
import org.tdf.sunflower.consensus.poa.PoA
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.net.Peer
import org.tdf.sunflower.net.PeerServer
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.state.Address.empty
import org.tdf.sunflower.sync.SyncManager
import org.tdf.sunflower.types.*
import java.util.function.Function

@RestController
@RequestMapping("/rpc")
class EntryController constructor(
    private val accountTrie: AccountTrie,
    private val peerServer: PeerServer,
    private val repo: RepositoryService,
    private val syncManager: SyncManager,
    private val consensusEngine: ConsensusEngine,
) {

    private fun <T> getBlockOrHeader(
        rd: RepositoryReader, hashOrHeight: String, func: Function<Long, T>,
        func1: Function<HexBytes, T>
    ): T {
        var height: Long? = null
        try {
            height = hashOrHeight.toLong()
            val h = rd.bestHeader
            while (height < 0) {
                height += h.height + 1
            }
        } catch (ignored: Exception) {
        }
        if (height != null) {
            return func.apply(height)
        }
        return func1.apply(HexBytes.fromHex(hashOrHeight))
    }

    private fun Block.v1(): BlockV1{
        return BlockV1.fromV2(this)
    }

    private fun Header.v1(): HeaderV1{
        return HeaderV1.fromV2(this)
    }

    private fun Transaction.v1(): TransactionV1 {
        return TransactionV1.fromV2(this)
    }

    @GetMapping(value = ["/block/{hashOrHeight}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBlock(@PathVariable hashOrHeight: String): BlockV1? {
        repo.reader.use { rd ->
            val b = getBlockOrHeader(rd, hashOrHeight, { rd.getCanonicalBlock(it) }, { rd.getBlockByHash(it) })
            return b?.v1()
        }
    }

    @GetMapping(value = ["/header/{hashOrHeight}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getHeaders(@PathVariable hashOrHeight: String): HeaderV1? {
        repo.reader.use { rd ->
            val h = getBlockOrHeader(rd, hashOrHeight, { rd.getCanonicalHeader(it) }, { rd.getHeaderByHash(it) })
            return h?.v1()
        }
    }

    @GetMapping(value = ["/transaction/{hash}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTransaction(@PathVariable hash: String): TransactionV1? {
        repo.reader.use { rd ->
            return rd.getTransaction(HexBytes.fromHex(hash))?.v1()
        }
    }

    @GetMapping(value = ["/account/{addressOrPublicKey}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAccount(@PathVariable addressOrPublicKey: String): AccountView {
        val addressHex = Address.of(addressOrPublicKey)
        repo.reader.use { rd ->
            val a = accountTrie.get(rd.bestHeader.stateRoot, addressHex) ?: Account.empty()
            return AccountView.fromAccount(addressHex, a)
        }
    }

    @GetMapping(value = ["/peers"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun peers(): PeersInfo {
        return PeersInfo(peerServer.peers, peerServer.bootstraps)
    }

    @GetMapping(value = ["/miners"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun miners(): List<HexBytes> {
        return emptyList()
    }

    @GetMapping(value = ["/orphan"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun orphans(): List<Block> {
        return syncManager.orphans
    }


    // if thread node not receive farm base transaciont and gateway node restarted
    // needs to construct the transaction
    @GetMapping(value = ["/farmBaseTransactions"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun farmBaseTransactions(): List<HexBytes> {
        val poa = consensusEngine as PoA
        return poa.cache
            .asMap()
            .values
            .map { HexBytes.fromBytes(it.encoded) }
    }

    class PeersInfo(var peers: List<Peer>, var bootstraps: List<Peer>)
    data class AccountView(
        val address: HexBytes, // for normal account this field is continuous integer
        // for contract account this field is nonce of deploy transaction
        @JsonSerialize(using = IntSerializer::class) val nonce: Long, // the balance of account
        // for contract account, this field is zero
        @JsonSerialize(using = IntSerializer::class) val balance: Uint256, // for normal address this field is null
        // for contract address this field is creator of this contract
        val createdBy: HexBytes, // hash code of contract code
        // if the account contains none contract, contract hash will be null
        val contractHash: HexBytes, // root hash of contract db
        // if the account is not contract account, this field will be null
        val storageRoot: HexBytes
    ) {

        companion object {
            fun fromAccount(address: HexBytes, account: Account): AccountView {
                return AccountView(
                    address, account.nonce, account.balance,
                    empty(), account.contractHash,
                    account.storageRoot
                )
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger("rpc")
    }
}