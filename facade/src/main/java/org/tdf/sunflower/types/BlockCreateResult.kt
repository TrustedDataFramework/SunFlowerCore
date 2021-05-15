package org.tdf.sunflower.types

data class BlockCreateResult(val block: Block?, val infos: List<TransactionInfo>) {
    val isEmpty: Boolean
        get() = this === EMPTY || block == null

    companion object {
        val EMPTY = BlockCreateResult(null, emptyList())

        @JvmStatic
        fun empty(): BlockCreateResult {
            return EMPTY
        }
    }
}