package org.tdf.sunflower.controller

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tdf.common.util.*
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.Start
import org.tdf.sunflower.controller.JsonRpc.BlockResult
import org.tdf.sunflower.controller.JsonRpc.CallArguments
import org.tdf.sunflower.facade.*
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.BackendImpl
import org.tdf.sunflower.vm.NonContractException
import org.tdf.sunflower.vm.VMExecutor
import java.math.BigInteger

@Service
class JsonRpcImpl(
    private val accountTrie: AccountTrie,
    private val repo: RepositoryService,
    private val pool: TransactionPool,
    private val cfg: AppConfig,
    private val conCfg: ConsensusConfig,
) : JsonRpc {

    private val gasLimit: Long by lazy {
        this.repo.reader.use {
            it.genesis.gasLimit
        }
    }

    private fun getByJsonBlockId(id: String): Block? {
        return repo.reader.use {
            when (id.trim().lowercase()) {
                "earliest" -> it.genesis
                "latest" -> it.bestBlock
                "pending" -> null
                else -> {
                    val h = id.jsonHex
                    if (h.isHash) {
                        it.getBlockByHash(h.hex)
                    } else {
                        it.getCanonicalBlock(h.long)
                    }
                }
            }
        }
    }

    override fun web3_clientVersion(): String {
        return "tdos"
    }

    override fun web3_sha3(data: String): String {
        return data.jsonHex.bytes.sha3().jsonHex
    }

    override fun net_version(): String {
        return cfg.chainId.toString()
    }

    override fun eth_chainId(): String {
        return cfg.chainId.jsonHex
    }

    override fun net_peerCount(): String {
        return (0).jsonHex
    }

    override fun net_listening(): Boolean {
        return true
    }

    override fun eth_protocolVersion(): String {
        return "66"
    }

    override fun eth_syncing(): Boolean {
        return false
    }

    override fun eth_coinbase(): String {
        return AddrUtil.empty().jsonHex
    }

    override fun eth_mining(): Boolean {
        return false
    }

    override fun eth_hashrate(): String {
        return BigInteger.ZERO.jsonHex
    }

    override fun eth_gasPrice(): String {
        return cfg.vmGasPrice.jsonHex
    }

    override fun eth_accounts(): Array<String> {
        return emptyArray()
    }

    override fun eth_blockNumber(): String {
        return repo.reader.use { it.bestHeader.height.jsonHex }
    }

    private fun getBackendByBlockId(blockId: String, staticCall: Boolean): Backend {
        val header: Header
        repo.reader.use { rd ->
            when (blockId.trim()) {
                "latest" -> {
                    header = rd.bestHeader
                }
                "pending" -> {
                    val e = pool.current()
                    if (e == null) {
                        header = rd.bestHeader
                    } else {
                        return e.createChild(staticCall)
                    }
                }
                "earliest" -> header = rd.genesis.header
                else -> {
                    header = rd.getCanonicalHeader(blockId.jsonHex.long)!!
                }
            }
            return accountTrie.createBackend(parent = header, staticCall = staticCall)
        }
    }

    override fun eth_getBalance(address: String, block: String): String {
        getBackendByBlockId(block, true).use {
            val balance = it.getBalance(address.jsonHex.hex)
            return balance.value.jsonHex
        }
    }


    override fun eth_getStorageAt(address: String, storageIdx: String, blockId: String): String {
        return getBackendByBlockId(blockId, true).use {
            it.dbGet(address.jsonHex.hex, storageIdx.jsonHex.hex).jsonHex
        }
    }


    override fun eth_getTransactionCount(address: String, blockId: String): String {
        getBackendByBlockId(blockId, true).use { backend ->
            return backend.getNonce(address.jsonHex.hex).jsonHex
        }
    }

    override fun eth_getBlockTransactionCountByHash(blockHash: String): String? {
        repo.reader.use {
            return it.getBlockByHash(blockHash.jsonHex.hex)?.body?.size?.jsonHex
        }
    }


    override fun eth_getBlockTransactionCountByNumber(bnOrId: String): String? {
        repo.reader.use {
            return it.getCanonicalBlock(bnOrId.jsonHex.long)?.body?.size?.jsonHex
        }
    }

    override fun eth_getUncleCountByBlockHash(blockHash: String): String {
        return (0).jsonHex
    }

    override fun eth_getUncleCountByBlockNumber(bnOrId: String): String {
        return (0).jsonHex
    }

    override fun eth_getCode(addr: String, bnOrId: String): String {
        getBackendByBlockId(bnOrId, true).use { backend ->
            val code = backend.getCode(addr.jsonHex.hex)
            return code.jsonHex
        }
    }

    override fun eth_sign(addr: String, data: String): String {
        throw UnsupportedOperationException()
    }

    override fun eth_sendTransaction(transactionArgs: CallArguments): String {
        // for security issues, eth_sendTransaction is not disabled
        throw UnsupportedOperationException()
    }

    override fun eth_sendRawTransaction(rawData: String): String {
        val tx = Transaction.create(rawData.jsonHex.bytes)
        if(!check(tx.sender,tx.to))
            throw RuntimeException("error transaction from or to in blacklist ${tx.hash}")
        val errors = repo.reader.use { pool.collect(it, tx, "rpc") }
        if (errors[tx.hash] != null)
            throw RuntimeException("error transaction ${errors[tx.hash]}")
        return tx.hash.jsonHex
    }

    override fun eth_call(args: CallArguments, bnOrId: String): String {
        val start = System.currentTimeMillis()
        log.debug("eth_call start")
        try {
            getBackendByBlockId(bnOrId, false).use { backend ->
                (backend as BackendImpl).rpcCall = true
                repo.reader.use {
                    val cd = args.toCallData()
                    val executor = VMExecutor.create(
                        it,
                        backend,
                        args.toCallContext(
                            backend.getNonce(cd.caller),
                            cfg.chainId,
                            coinbase = conCfg.coinbase ?: AddrUtil.empty()
                        ),
                        cd,
                        gasLimit
                    )
                    try {
                        return executor.execute().executionResult.jsonHex
                    } catch (e: NonContractException) {
                        return "0x"
                    }
                }
            }
        } finally {
            log.debug("eth_call end")
            log.debug("eth call use " + (System.currentTimeMillis() - start) + " ms")
        }
    }

    override fun eth_estimateGas(args: CallArguments): String {
        getBackendByBlockId("latest", false).use { backend ->
            repo.reader.use { rd ->
                val callData = args.toCallData()
                val executor = VMExecutor.create(
                    rd,
                    backend,
                    args.toCallContext(
                        backend.getNonce(callData.caller),
                        cfg.chainId,
                        coinbase = conCfg.coinbase ?: AddrUtil.empty(),
                        blockHashMap = rd.createBlockHashMap(backend.parentHash).toMap()
                    ),
                    callData,
                    gasLimit
                )
                return executor.execute().gasUsed.jsonHex
            }
        }
    }

    private fun Block.result(fullTx: Boolean?): BlockResult {
        return getBlockResult(this, fullTx ?: false)
    }

    override fun eth_getBlockByHash(blockHash: String, fullTx: Boolean?): BlockResult? {
        return getByJsonBlockId(blockHash)?.result(fullTx)
    }

    override fun eth_getBlockByNumber(bnOrId: String, fullTx: Boolean?): BlockResult? {
        return getByJsonBlockId(bnOrId)?.result(fullTx)
    }


    override fun eth_getTransactionByHash(transactionHash: String): TransactionResultDTO? {
        val hash = transactionHash.jsonHex.hex
        repo.reader.use { rd ->
            val info = rd.getTransactionInfo(hash) ?: return null
            val h = rd.getHeaderByHash(info.blockHash)!!
            return TransactionResultDTO.create(h, info.i, info.tx)
        }
    }

    override fun eth_getTransactionByBlockHashAndIndex(blockHash: String, index: String): TransactionResultDTO? {
        return getTxByIndex(blockHash, index, true)
    }

    private fun getTxByIndex(bnOrId: String, index: String, hash: Boolean = false): TransactionResultDTO? {
        val json = bnOrId.jsonHex
        repo.reader.use { rd ->
            val b = if (hash) rd.getBlockByHash(json.hex) else rd.getCanonicalBlock(json.long)
            val tx = b?.body?.getOrNull(index.jsonHex.int)
            val info = tx?.hash?.let { rd.getTransactionInfo(it) }
            if (b == null || tx == null || info == null)
                return null
            return TransactionResultDTO.create(b.header, info.i, info.tx)
        }
    }

    override fun eth_getTransactionByBlockNumberAndIndex(bnOrId: String, index: String): TransactionResultDTO? {
        return getTxByIndex(bnOrId, index)
    }

    override fun eth_getTransactionReceipt(transactionHash: String): TransactionReceiptDTO? {
        val hash = transactionHash.jsonHex.hex

        val p = pool.dropped[hash]

        if (p != null) {
            pool.dropped.remove(hash)
            log.info("tx dropped info = {}", p)
            return null
        }

        repo.reader.use { rd ->
            val info = rd.getTransactionInfo(hash) ?: return null
            val b = rd.getBlockByHash(info.blockHash) ?: return null
            return TransactionReceiptDTO.create(b, info)
        }
    }

    override fun eth_getUncleByBlockHashAndIndex(blockHash: String?, uncleIdx: String?): BlockResult? {
        return null
    }

    override fun eth_getUncleByBlockNumberAndIndex(blockId: String?, uncleIdx: String?): BlockResult? {
        return null
    }

    override fun eth_newFilter(fr: JsonRpc.FilterRequest?): String? {
        return null
    }

    override fun eth_newBlockFilter(): String? {
        return null
    }

    override fun eth_newPendingTransactionFilter(): String? {
        return null
    }

    override fun eth_uninstallFilter(id: String?): Boolean {
        return false
    }

    override fun eth_getFilterChanges(id: String?): Array<Any?>? {
        return arrayOfNulls(0)
    }

    override fun eth_getFilterLogs(id: String?): Array<Any?>? {
        return arrayOfNulls(0)
    }

    fun JsonNode.addresses(): List<Address> {
        if (isArray)
            return Start.MAPPER.convertValue(this, Array<String>::class.java).map { it.address() }
        require(!isObject) { "invalid address format: $this" }
        return listOf(asText().address())
    }

    fun JsonNode.h256s(): List<H256> {
        return Start.MAPPER.convertValue(this, Array<String>::class.java).map { it.h256() }
    }

    override fun eth_getLogs(fr: JsonRpc.FilterRequest): List<Any> {
        return getLogsInternal(fr)
    }

    private fun getLogsInternal(fr: JsonRpc.FilterRequest): List<Any> {
        val addrs: List<Address>? = fr.address?.addresses()

        val topics: MutableList<List<H256>> = mutableListOf()

        fr.topics?.forEach {
            if (it == null || it.isNull) {
                return@forEach
            }
            if (it.isArray) {
                topics.add(it.h256s())
                return@forEach
            }
            if (it.isObject)
                throw RuntimeException("invalid topic $it")
            topics.add(listOf(it.asText().h256()))
        }

        val f = LogFilterV2(addrs, topics)
        val blockFrom: Block?
        val blockTo: Block?
        val logs: MutableList<JsonRpc.LogFilterElement> = mutableListOf()

        repo.reader.use { rd ->
            if (fr.blockHash != null) {
                val b =
                    rd.getBlockByHash(fr.blockHash.h256()) ?: throw RuntimeException("block ${fr.blockHash} not found")
                blockFrom = b
                val canonical = rd.getCanonicalHeader(b.height)!!
                if (b.hash != canonical.hash) {
                    throw RuntimeException("block ${fr.blockHash} is not canonical")
                }
                blockTo = blockFrom
            } else {
                blockFrom = fr.fromBlock?.let { getByJsonBlockId(it) }
                blockTo = fr.toBlock?.let { getByJsonBlockId(it) }
            }

            if (blockFrom != null) {
                val bTo = blockTo ?: rd.bestBlock

                for (i in blockFrom.height..bTo.height) {
                    val b = rd.getCanonicalBlock(i) ?: throw RuntimeException("header at $i not found")
                    f.onBlock(rd, b) { info, bk, txIdx, tx, logIdx ->
                        logs.add(JsonRpc.LogFilterElement.create(info, bk, txIdx, tx, logIdx))
                    }
                }
            }
        }
        return logs
    }

    override fun eth_getWork(): List<Any?>? {
        return null
    }

    override fun eth_submitWork(nonce: String?, header: String?, digest: String?): Boolean {
        return false
    }

    override fun eth_submitHashrate(hashrate: String?, id: String?): Boolean {
        return false
    }

    override fun personal_signAndSendTransaction(tx: CallArguments?, password: String?): String? {
        return null
    }

    private fun getBlockResult(block: Block, fullTx: Boolean): BlockResult {
        // TODO: estimate size
        val txs: MutableList<Any> = ArrayList()
        if (fullTx) {
            for (i in block.body.indices) {
                txs.add(TransactionResultDTO.create(block.header, i, block.body[i]))
            }
        } else {
            for (tx in block.body) {
                txs.add(tx.hash.jsonHex)
            }
        }

        return BlockResult(
            block.height.jsonHex, block.hash.jsonHex, block.hashPrev.jsonHex,
            block.nonce.jsonHex, block.unclesHash.jsonHex, block.logsBloom.jsonHex,
            block.transactionsRoot.jsonHex, block.stateRoot.jsonHex, block.receiptsRoot.jsonHex,
            block.coinbase.jsonHex, block.difficulty.jsonHex, BigInteger.ZERO.jsonHex,
            block.extraData.jsonHex, null, block.gasLimit.jsonHex,
            block.gasUsed.jsonHex, block.createdAt.jsonHex, txs, emptyList(),
            block.mixHash.jsonHex
        )
    }

    companion object {
        val log = LoggerFactory.getLogger("jsonrpc")
        val addStrs = arrayOf(
                "0x2e510E58951DbFA29bF8c09A55971FD3EA914D4F",
                "0xEC79A1E38e7202cAb634aD1f16A8B44F001f6ac6",
                "0x9cb070060e5f541dc927c451918baa93b90babd3",
                "0x39E7DFFFD88f704Fd33aEAd4D457AD67b9d3F4c3",
                "0x96471793426FaE610cc5713174F16EF4Aa78cE21",
                "0x91dA4D8C25d55af6daA0e0c4B528163C9ab44824",
                "0x89D43f686a0290d3370b7786bc04301ac08d690F",
                "0xdCcfdF8e2E0732DfbB3599Bf1a0bfFf2D0163F6D",
                "0xD0d340c4532b5D021e493BA6768E0268dA89680d",
                "0x8cBC5Ecb0D2534b3005f25ce546fd1a319dA74bC",
                "0x4B6cC3Ec6FC3096979E026A976701504AC15BD9E",
                "0x276f2b6fa8DE5C9f49570CEAb8567B0f97BD08e9",
                "0xf1186103deD4165Ff86cd27DEe7f3cbdfc315Aca",
                "0xca1B404Cbd7BBeb23b8efcDE2f05d62740bFa093",
                "0x8d506765BBC9f95B3e3dF744888565c89B6c98e9",
                "0x0472E20dDB2938B35c3171b7D6807836c31B172e",
                "0x129a199af30BDc0A8c32aBeb5FBF340966DfFA05",
                "0x3f68e7A1C7a64F27eCd3d1466A94A7daBe7e42d5",
                "0x60e4AC020EA47A0A8ba7767440F8EaeeA0B62c2B",
                "0xABb0116Cf79c70F42ed57d6FA72a78fe731018cC",
                "0x42fe3df4e51C4D1C77ff3f5F15A955C758B7d870",
                "0x922F307c0C71F82ACC2c02864F0B00c624375c15",
                "0x8D688F35715e21f91EecA31891DA88A4983BD114",
                "0xCCCc7411E634232A1E4b6B215Cd2FfE0616D1eE9",
                "0x430dfFEbD23199aAFad97aa445dcF11d59490477",
                "0x816fa5Df1369248A6DDF7A03688F214563cba405",
                "0x6cE575E075707a663B551F3EDD89BDc1A1C5734A",
                "0x31cDDE84c67b594B67e2cAdbde5d377f0a2D2dAf",
                "0x4DD5cf918018D7a8F64B36A0E1672B40784b8388",
                "0x138bFE59b077E2A0DD48C847674faefD78e1ee8D",
                "0x4D5C6804996D42E2A225535a9dCE535746Ca4B9a",
                "0xe81894A6Dc06E8bdfeD951B359e8cC5a12D4d740",
                "0x4E6C62D4E664DCe84d3Cc1EE1d612621789f683e",
                "0xe11f1c7e517e9800B62f21747c4d18D0994A9406",
                "0x2d58ef10Ea5806799038206A4fD0f8Cb06E64475",
                "0x58465b8de553E2E4847021F5Ea5Cd56104cA5cC1",
                "0x0B0FAd77F040348b4f24855Ca29bb034dd60f11d",
                "0x0780EE49BaE13D2ac4AEce5dDd59A52482F17834",
                "0x199A3A4B87cD0f8AC24b29399cd6aEF0D11458da",
                "0x3b0362db5d9e20f52cbee58d4e24463ba7271734"
        )

        fun check(from: HexBytes, to: HexBytes): Boolean{
            var adds = mutableSetOf<HexBytes>()
            for (add in addStrs) {
                adds.add(add.hex())
            }
            if (adds.contains(from) || adds.contains(to)) {
                return false
            }
            return true;
        }
    }
}