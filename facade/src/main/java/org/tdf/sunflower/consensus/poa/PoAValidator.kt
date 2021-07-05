package org.tdf.sunflower.consensus.poa


import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.crypto.ECDSASignature
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.consensus.AbstractValidator
import org.tdf.sunflower.consensus.poa.PoaUtils.getRawHash
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.types.ValidateResult
import org.tdf.sunflower.types.ValidateResult.Companion.fault
import org.tdf.sunflower.types.ValidateResult.Companion.success
import java.util.*

class PoAValidator(accountTrie: StateTrie<HexBytes, Account>, private val poA: PoA) : AbstractValidator(accountTrie) {
    override fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult {
        val res = super.commonValidate(rd, block, dependency)
        if (!res.success) return res
        val fee = res.fee
        if (fee + poA.model.rewardAt(dependency.height + 1) != block.body[0].valueAsUint
        ) {
            return fault(
                "reward of coin base transaction should be " + poA.model.rewardAt(
                    dependency.height + 1
                )
            )
        }
        if (block.coinbase != block.body[0].receiveHex) return fault("block coinbase not equals to coinbase transaction receiver")
        val res0 = validateCoinBase(dependency, block.body[0])
        if (!res0.success) return res0
        if (poA.minerContract.getProposer(
                rd,
                dependency.hash,
                block.createdAt
            ).address != block.body[0].receiveHex
        ) return fault("invalid proposer " + block.body[0].senderHex)

        // validate signature
        val vrs = Rlp.decodeList(block.extraData.bytes)
        val v = Rlp.decodeByte(vrs.rawAt(0))
        val r = vrs.bytesAt(1)
        val s = vrs.bytesAt(2)
        val signature = ECDSASignature.fromComponents(r, s, v)
        val rawHash = getRawHash(block.header)
        // validate signer
        val signer = HexBytes.fromBytes(ECKey.signatureToAddress(rawHash, signature))
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
        if (!poA.config.controlled) return success()
        val farmBaseAdmin = poA.config.farmBaseAdmin
        if (poA.config.threadId == PoA.GATEWAY_ID) { // for gateway node, only accept transaction from farm-base admin
            if (Objects.requireNonNull(transaction.chainId) != PoA.GATEWAY_ID || transaction.senderHex != farmBaseAdmin) {
                return fault(
                    String.format(
                        "farmbase only accept admin transaction with network id = %s, while from = %s, network id = %s",
                        PoA.GATEWAY_ID,
                        transaction.senderHex,
                        transaction.chainId
                    )
                )
            }

            // for thread node, only accept transaction with thread id or gateway id
        } else {
            if (transaction.chainId != PoA.GATEWAY_ID && transaction.chainId != poA.config.threadId) {
                return fault(
                    String.format(
                        "this thread only accept transaction with thread id = %s, while id = %s received",
                        poA.config.threadId,
                        transaction.chainId
                    )
                )
            }
            if (transaction.chainId == PoA.GATEWAY_ID && transaction.senderHex != farmBaseAdmin) {
                return fault("transaction with zero version should received from farmbase")
            }
        }
        return if (transaction.senderHex != farmBaseAdmin
            && !poA.validatorContract
                .getApproved(
                    rd,
                    dependency.hash
                )
                .contains(
                    transaction.senderHex
                )
        ) fault("from address is not approved") else success()
    }

    private fun validateCoinBase(parent: Block, coinBase: Transaction): ValidateResult {
        return if (coinBase.nonceAsLong != parent.height + 1) fault("nonce of coin base should be " + parent.height + 1) else success()
    }

    override val blockGasLimit: Long
        get() = poA.config.blockGasLimit

}