package org.tdf.sunflower.controller

import org.springframework.stereotype.Service
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.controller.JsonRpc.BlockResult
import org.tdf.sunflower.controller.JsonRpc.CallArguments
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.facade.TransactionPool
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.state.Address
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.VMExecutor
import java.math.BigInteger


@Service
class JsonRpcImpl(
    private val accountTrie: AccountTrie,
    private val repo: RepositoryService,
    private val pool: TransactionPool,
    private val engine: ConsensusEngine
) : JsonRpc {
    private fun getByJsonBlockId(id: String): Block? {
        return repo.reader.use {
            when(id.trim().lowercase()) {
                "earliest" -> it.genesis
                "latest" -> it.bestBlock
                "pending" -> null
                else -> it.getCanonicalBlock(id.jsonHex.long)
            }
        }
    }

    override fun web3_clientVersion(): String {
        return "tdos"
    }

    override fun web3_sha3(data: String): String {
        return HashUtil.sha3(data.jsonHex.bytes).jsonHex
    }

    override fun net_version(): String {
        return engine.chainId.toString()
    }

    override fun eth_chainId(): String {
        return engine.chainId.jsonHex
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
        return Address.empty().jsonHex
    }

    override fun eth_mining(): Boolean {
        return false
    }

    override fun eth_hashrate(): String {
        return BigInteger.ZERO.jsonHex
    }

    override fun eth_gasPrice(): String {
        return (0).jsonHex
    }

    override fun eth_accounts(): Array<String> {
        return emptyArray()
    }

    override fun eth_blockNumber(): String {
        return repo.reader.use { it.bestHeader.height.jsonHex }
    }

    private fun getBackendByBlockId(blockId: String, isStatic: Boolean): Backend {
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
                        e.staticCall = isStatic
                        return e
                    }
                }
                "earliest" -> header = rd.genesis.header
                else -> {
                    header = rd.getCanonicalHeader(blockId.jsonHex.long)!!
                }
            }
            return accountTrie.createBackend(header, System.currentTimeMillis() / 1000, isStatic, header.stateRoot)
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
        val errors = repo.reader.use { pool.collect(it, tx) }
        if (errors[tx.hash] != null)
            throw RuntimeException(errors[tx.hash])
        return tx.hash.jsonHex
    }

    override fun eth_call(args: CallArguments, bnOrId: String): String {
        val start = System.currentTimeMillis()
        try {
            getBackendByBlockId(bnOrId, true).use { backend ->
                repo.reader.use {
                    val cd = args.toCallData()
                    val executor = VMExecutor(
                        it,
                        backend,
                        args.toCallContext(backend.getNonce(cd.caller)),
                        cd, AppConfig.INSTANCE.blockGasLimit
                    )
                    return executor.execute().executionResult.jsonHex
                }
            }
        } finally {
            println("eth call use " + (System.currentTimeMillis() - start) + " ms")
        }
    }

    override fun eth_estimateGas(args: CallArguments?): String {
        getBackendByBlockId("latest", false).use { backend ->
            repo.reader.use { rd ->
                val callData = args!!.toCallData()
                val executor = VMExecutor(
                    rd,
                    backend,
                    args.toCallContext(backend.getNonce(callData.caller)),
                    callData,
                    AppConfig.INSTANCE.blockGasLimit
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
            val h = rd.getHeaderByHash(info.blockHashHex)!!
            return TransactionResultDTO.create(h, info.index, info.receipt.transaction)
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
            return TransactionResultDTO.create(b.header, info.index, info.receipt.transaction)
        }
    }

    override fun eth_getTransactionByBlockNumberAndIndex(bnOrId: String, index: String): TransactionResultDTO? {
        return getTxByIndex(bnOrId, index)
    }

    override fun eth_getTransactionReceipt(transactionHash: String): TransactionReceiptDTO? {
        val hash = transactionHash.jsonHex.hex
        repo.reader.use { rd ->
            val info = rd.getTransactionInfo(hash)
            val tx = info?.receipt?.transaction
            val b = if (info == null) null else rd.getBlockByHash(HexBytes.fromBytes(info.blockHash))
            if (info == null || tx == null || b == null) return null
            info.setTransaction(tx)
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

    override fun eth_getLogs(fr: JsonRpc.FilterRequest?): Array<Any?>? {
        return arrayOfNulls(0)
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
        val br = BlockResult(
            block.height.jsonHex, block.hash.jsonHex, block.hashPrev.jsonHex,
            block.nonce.jsonHex, block.unclesHash.jsonHex, block.logsBloom.jsonHex,
            block.transactionsRoot.jsonHex, block.stateRoot.jsonHex, block.receiptTrieRoot.jsonHex,
            block.coinbase.jsonHex, block.difficulty.bytes.jsonHexNum, BigInteger.ZERO.jsonHex,
            block.extraData.jsonHex, null, block.gasLimit.jsonHex,
            block.gasUsed.jsonHex, block.createdAt.jsonHex
        )

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
        br.transactions = txs.toTypedArray()
        val ul = emptyList<String>()
        br.uncles = ul.toTypedArray()
        return br
    }
}