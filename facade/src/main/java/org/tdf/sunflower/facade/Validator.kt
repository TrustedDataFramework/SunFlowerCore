package org.tdf.sunflower.facade

import org.tdf.sunflower.types.ValidateResult.Companion.success
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.ValidateResult


/**
 * validate blocks
 */
interface BlockValidator {
    /**
     * validate the block
     *
     * @param block      the block to validate
     * @param dependency parent block
     * @return validate result
     */
    fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult
}

interface PendingTransactionValidator {
    fun validate(rd: RepositoryReader, dependency: Header, transaction: Transaction): ValidateResult
}

interface Validator : BlockValidator, PendingTransactionValidator {
    companion object {
        val NONE: Validator = object : Validator {
            override fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult {
                return success()
            }

            override fun validate(rd: RepositoryReader, dependency: Header, transaction: Transaction): ValidateResult {
                return success()
            }
        }
    }
}