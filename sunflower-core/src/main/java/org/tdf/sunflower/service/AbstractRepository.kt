package org.tdf.sunflower.service

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.tdf.common.event.EventBus
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.facade.DatabaseStoreFactory
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.RepositoryWriter
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import java.util.stream.Collectors

abstract class AbstractRepository
    (context: ApplicationContext) : RepositoryReader, RepositoryWriter {
    protected val eventBus: EventBus = context.getBean(EventBus::class.java)
    protected val factory: DatabaseStoreFactory = context.getBean(DatabaseStoreFactory::class.java)
    var accountTrie: StateTrie<HexBytes, Account>? = null

    var genesisCache: Block? = null

    override val genesis: Block
        get() = genesisCache!!

    protected abstract fun writeGenesis(genesis: Block)

    override fun saveGenesis(b: Block) {
        genesisCache = b
        val o = getBlocksByHeight(0)
        if (o.isEmpty()) {
            writeGenesis(genesisCache!!)
            return
        }
        if (o.size > 1 || o.stream().anyMatch { x: Block -> x.hash != b.hash }) {
            log.error("genesis in db not equals to genesis in configuration")
        }
    }

    protected abstract fun getBlockFromHeader(header: Header): Block

    private fun getBlocksFromHeaders(headers: Collection<Header>): List<Block> {
        return headers.stream().map { getBlockFromHeader(it) }.collect(Collectors.toList())
    }

    override fun getCanonicalBlock(height: Long): Block? {
        return getCanonicalHeader(height)?.let { getBlockFromHeader(it) }
    }

    override val bestBlock: Block
        get() = getBlockFromHeader(bestHeader)


    override fun getBlocksBetween(startHeight: Long, stopHeight: Long, limit: Int, descend: Boolean): List<Block> {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight, limit, descend))
    }

    override fun getBlocksByHeight(height: Long): List<Block> {
        return getBlocksFromHeaders(getHeadersByHeight(height))
    }

    override fun getBlockByHash(hash: HexBytes): Block? {
        return getHeaderByHash(hash)?.let { getBlockFromHeader(it) }
    }

    companion object {
        val log = LoggerFactory.getLogger("db")
    }
}