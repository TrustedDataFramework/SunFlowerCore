package org.tdf.sunflower.consensus.pos

import org.tdf.sunflower.types.ValidateResult.Companion.fault
import org.tdf.sunflower.state.StateTrie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.consensus.AbstractValidator
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.ValidateResult
import org.tdf.sunflower.types.Block

class PoSValidator(accountTrie: StateTrie<HexBytes, Account>, private val pos: PoS, override val chainId: Int) :
    AbstractValidator(accountTrie) {
    override fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult {
        val res = super.commonValidate(rd, block, dependency)
        if (!res.success) return res

        val p = pos.getProposer(rd, dependency, block.createdAt)
        val eq = p?.first == block.coinbase
        return if (
            !eq
        ) fault("invalid proposer " + block.body[0].sender) else res
    }
}