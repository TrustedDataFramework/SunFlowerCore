package org.tdf.sunflower.consensus.poa


import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.crypto.ECDSASignature
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.BigEndian
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.common.util.u256
import org.tdf.sunflower.consensus.AbstractValidator
import org.tdf.sunflower.consensus.poa.PoAUtils.getRawHash
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.ValidateResult
import org.tdf.sunflower.types.ValidateResult.Companion.fault
import org.tdf.sunflower.types.ValidateResult.Companion.success

class PoAValidator(accountTrie: StateTrie<HexBytes, Account>, private val poA: PoA) : AbstractValidator(accountTrie) {
    override fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult {
        val res = super.commonValidate(rd, block, dependency)
        if (!res.success) return res
        val fee = res.fee
        if (fee + poA.model.rewardAt(dependency.height + 1) != block.body[0].value
        ) {
            return fault(
                "reward of coin base transaction should be " + poA.model.rewardAt(
                    dependency.height + 1
                )
            )
        }

        val res0 = validateCoinBase(dependency, block.body[0])
        if (!res0.success) return res0
        if (poA.minerContract.getProposer(
                rd,
                dependency.hash,
                block.createdAt
            ).address != block.body[0].to
        ) return fault("invalid proposer " + block.body[0].to)

        // validate signature
        val v = BigEndian.decodeInt64(block.nonce.bytes, 0)
        val r = block.extraData.bytes.u256()
        val s = vrs.bytesAt(2)
        val signature = ECDSASignature.fromComponents(r, s, v)
        val rawHash = getRawHash(block.header)
        // validate signer
        val signer = ECKey.signatureToAddress(rawHash, signature).hex()
        if (signer != block.coinbase) {
            return fault("signer not equals to coinbase")
        }
        val key = ECKey.signatureToKey(rawHash, signature)
        // verify signature
        return if (!key.verify(rawHash, signature)) {
            fault("verify signature failed")
        } else res
    }

    // validate pre-pending transaction
    override fun validate(rd: RepositoryReader, dependency: Header, transaction: Transaction): ValidateResult {
        return success()
    }

    private fun validateCoinBase(parent: Block, coinBase: Transaction): ValidateResult {
        return if (coinBase.nonce != parent.height + 1) fault("nonce of coin base should be " + parent.height + 1) else success()
    }

    override val blockGasLimit: Long
        get() = poA.config.blockGasLimit

}