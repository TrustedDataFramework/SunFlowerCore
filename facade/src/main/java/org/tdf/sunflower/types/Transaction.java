package org.tdf.sunflower.types;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.tdf.common.crypto.ECDSASignature;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.*;
import org.tdf.rlp.*;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.*;

import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tdf.common.util.ByteUtil.ZERO_BYTE_ARRAY;

@Slf4j(topic = "tx")
@RLPEncoding(Transaction.TransactionEncoder.class)
@RLPDecoding(Transaction.TransactionDecoder.class)
public class Transaction {
    public static class TransactionEncoder implements RLPEncoder<Transaction> {

        @Override
        public RLPElement encode(@NonNull Transaction o) {
            return RLPElement.fromEncoded(o.getEncoded());
        }
    }

    public static class TransactionDecoder implements RLPDecoder<Transaction> {

        @Override
        public Transaction decode(@NonNull RLPElement element) {
            return new Transaction(element.getEncoded());
        }
    }

    public static HexBytes calcTxTrie(List<Transaction> transactions) {
        Trie<byte[], byte[]> txsState = Trie
                .<byte[], byte[]>builder()
                .keyCodec(Codec.identity())
                .valueCodec(Codec.identity())
                .store(new ByteArrayMapStore<>())
                .hashFunction(HashUtil::sha3).build();

        if (transactions == null || transactions.isEmpty())
            return HexBytes.fromBytes(HashUtil.EMPTY_TRIE_HASH);

        for (int i = 0; i < transactions.size(); i++) {
            txsState.put(RLPCodec.encodeInt(i), transactions.get(i).getEncoded());
        }

        return txsState.commit();
    }

    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;
    private static final BigInteger DEFAULT_GAS_PRICE = new BigInteger("10000000000000");
    private static final BigInteger DEFAULT_BALANCE_GAS = new BigInteger("21000");
    /**
     * Since EIP-155, we could encode chainId in V
     */
    private static final int CHAIN_ID_INC = 35;
    private static final int LOWER_REAL_V = 27;
    protected byte[] sendAddress;
    /* Tx in encoded form */
    protected byte[] rlpEncoded;
    /* Indicates if this transaction has been parsed
     * from the RLP-encoded data */
    protected boolean parsed = false;
    /* SHA3 hash of the RLP encoded transaction */
    private byte[] hash;
    /* a counter used to make sure each transaction can only be processed once */
    private byte[] nonce;
    /* the amount of ether to transfer (calculated as wei) */
    private byte[] value;
    /* the address of the destination account
     * In creation transaction the receive address is - 0 */
    private byte[] receiveAddress;
    /* the amount of ether to pay as a transaction fee
     * to the miner for each unit of gas */
    private byte[] gasPrice;
    /* the amount of "gas" to allow for the computation.
     * Gas is the fuel of the computational engine;
     * every computational step taken and every byte added
     * to the state or transaction list consumes some gas. */
    private byte[] gasLimit;
    /* An unlimited size byte array specifying
     * input [data] of the message call or
     * Initialization code for a new contract */
    private byte[] data;
    private Integer chainId = null;
    /* the elliptic curve signature
     * (including public key recovery bits) */
    private ECDSASignature signature;
    private byte[] rawHash;

    public Transaction(byte[] rawData) {
        this.rlpEncoded = rawData;
        parsed = false;
    }

    public Transaction(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data,
                       Integer chainId) {
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.receiveAddress = receiveAddress;
        if (ByteUtil.isSingleZero(value)) {
            this.value = EMPTY_BYTE_ARRAY;
        } else {
            this.value = value;
        }
        this.data = data;
        this.chainId = chainId;

        if (receiveAddress == null) {
            this.receiveAddress = EMPTY_BYTE_ARRAY;
        }

        parsed = true;
    }

    /**
     * Warning: this transaction would not be protected by replay-attack protection mechanism
     * Use {@link Transaction#Transaction(byte[], byte[], byte[], byte[], byte[], byte[], Integer)} constructor instead
     * and specify the desired chainID
     */
    @Builder
    public Transaction(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data) {
        this(nonce, gasPrice, gasLimit, receiveAddress, value, data, null);
    }

    public Transaction(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data,
                       byte[] r, byte[] s, byte v, Integer chainId) {
        this(nonce, gasPrice, gasLimit, receiveAddress, value, data, chainId);
        this.signature = ECDSASignature.fromComponents(r, s, v);
    }


    /**
     * Warning: this transaction would not be protected by replay-attack protection mechanism
     * Use {@link Transaction#Transaction(byte[], byte[], byte[], byte[], byte[], byte[], byte[], byte[], byte, Integer)}
     * constructor instead and specify the desired chainID
     */
    public Transaction(byte[] nonce, byte[] gasPrice, byte[] gasLimit, byte[] receiveAddress, byte[] value, byte[] data,
                       byte[] r, byte[] s, byte v) {
        this(nonce, gasPrice, gasLimit, receiveAddress, value, data, r, s, v, null);
    }

    /**
     * @deprecated Use {@link Transaction#createDefault(String, BigInteger, BigInteger, Integer)} instead
     */
    @Deprecated
    public static Transaction createDefault(String to, BigInteger amount, BigInteger nonce) {
        return create(to, amount, nonce, DEFAULT_GAS_PRICE, DEFAULT_BALANCE_GAS);
    }

    public static Transaction createDefault(String to, BigInteger amount, BigInteger nonce, Integer chainId) {
        return create(to, amount, nonce, DEFAULT_GAS_PRICE, DEFAULT_BALANCE_GAS, chainId);
    }

    /**
     * @deprecated use {@link Transaction#create(String, BigInteger, BigInteger, BigInteger, BigInteger, Integer)} instead
     */
    @Deprecated
    public static Transaction create(String to, BigInteger amount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit) {
        return new Transaction(BigIntegers.asUnsignedByteArray(nonce),
                BigIntegers.asUnsignedByteArray(gasPrice),
                BigIntegers.asUnsignedByteArray(gasLimit),
                HexBytes.decode(to),
                BigIntegers.asUnsignedByteArray(amount),
                null);
    }

    public static Transaction create(String to, BigInteger amount, BigInteger nonce, BigInteger gasPrice,
                                     BigInteger gasLimit, Integer chainId) {
        return new Transaction(BigIntegers.asUnsignedByteArray(nonce),
                BigIntegers.asUnsignedByteArray(gasPrice),
                BigIntegers.asUnsignedByteArray(gasLimit),
                HexBytes.decode(to),
                BigIntegers.asUnsignedByteArray(amount),
                null,
                chainId);
    }

    private Integer extractChainIdFromRawSignature(BigInteger bv, byte[] r, byte[] s) {
        if (r == null && s == null) return bv.intValue();  // EIP 86
        if (bv.bitLength() > 31)
            return Integer.MAX_VALUE; // chainId is limited to 31 bits, longer are not valid for now
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return null;
        return (int) ((v - CHAIN_ID_INC) / 2);
    }

    private byte getRealV(BigInteger bv) {
        if (bv.bitLength() > 31) return 0; // chainId is limited to 31 bits, longer are not valid for now
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return (byte) v;
        byte realV = LOWER_REAL_V;
        int inc = 0;
        if ((int) v % 2 == 0) inc = 1;
        return (byte) (realV + inc);
    }

    public long transactionCost(Block block) {
        rlpParse();
        return 0;
    }

    public synchronized void verify() {
        rlpParse();
        validate();
    }

    public synchronized void rlpParse() {
        if (parsed) return;
        try {
            RLPList transaction = RLPElement.fromEncoded(rlpEncoded).asRLPList();

            // Basic verification
            if (transaction.size() > 9) throw new RuntimeException("Too many RLP elements");
            for (RLPElement rlpElement : transaction) {
                if (!rlpElement.isRLPItem())
                    throw new RuntimeException("Transaction RLP elements shouldn't be lists");
            }

            this.nonce = transaction.get(0).asBytes();
            this.gasPrice = transaction.get(1).asBytes();
            this.gasLimit = transaction.get(2).asBytes();
            this.receiveAddress = transaction.get(3).asBytes();
            this.value = transaction.get(4).asBytes();
            this.data = transaction.get(5).asBytes();
            // only parse signature in case tx is signed
            if (transaction.size() >= 7 && !transaction.get(6).isNull()) {
                byte[] vData = transaction.get(6).asBytes();
                BigInteger v = ByteUtil.bytesToBigInteger(vData);
                byte[] r = transaction.get(7).asBytes();
                byte[] s = transaction.get(8).asBytes();
                this.chainId = extractChainIdFromRawSignature(v, r, s);
                if (r != null && s != null) {
                    this.signature = ECDSASignature.fromComponents(r, s, getRealV(v));
                }
            } else {
                log.debug("RLP encoded tx is not signed!");
            }
            this.hash = HashUtil.sha3(rlpEncoded);
            this.parsed = true;
        } catch (Exception e) {
            throw new RuntimeException("Error on parsing RLP", e);
        }
    }

    private void validate() {
        if (getNonce().length > HASH_LENGTH) throw new RuntimeException("Nonce is not valid");
        if (receiveAddress != null && receiveAddress.length != 0 && receiveAddress.length != ADDRESS_LENGTH)
            throw new RuntimeException("Receive address is not valid");
        if (gasLimit.length > HASH_LENGTH)
            throw new RuntimeException("Gas Limit is not valid");
        if (gasPrice != null && gasPrice.length > HASH_LENGTH)
            throw new RuntimeException("Gas Price is not valid");
        if (value != null && value.length > HASH_LENGTH)
            throw new RuntimeException("Value is not valid");
        if (getSignature() != null) {
            if (BigIntegers.asUnsignedByteArray(signature.r).length > HASH_LENGTH)
                throw new RuntimeException("Signature R is not valid");
            if (BigIntegers.asUnsignedByteArray(signature.s).length > HASH_LENGTH)
                throw new RuntimeException("Signature S is not valid");
            if (getSender() != null && getSender().length != ADDRESS_LENGTH)
                throw new RuntimeException("Sender is not valid");
        }
    }

    public boolean isParsed() {
        return parsed;
    }

    public byte[] getHash() {
        if ( hash != null && hash.length != 0)
            return hash;
        rlpParse();
        getEncoded();
        return hash;
    }

    public HexBytes getHashHex() {
        return HexBytes.fromBytes(getHash());
    }

    public byte[] getRawHash() {
        rlpParse();
        if (rawHash != null) return rawHash;
        byte[] plainMsg = this.getEncodedRaw();
        return rawHash = HashUtil.sha3(plainMsg);
    }

    public byte[] getNonce() {
        rlpParse();
        return nonce;
    }

    public long getNonceAsLong() {
        return ByteUtil.bytesToBigInteger(getNonce()).longValueExact();
    }

    public boolean isValueTx() {
        rlpParse();
        return value != null;
    }

    public byte[] getValue() {
        rlpParse();
        return value;
    }

    public Uint256 getValueAsUint() {
        return Uint256.of(getValue());
    }

    public void setValue(byte[] value) {
        this.value = value;
        rlpEncoded = null;
        hash = null;
        parsed = true;
    }

    public void setValue(Uint256 value) {
        setValue(value.getNoLeadZeroesData());
    }

    public byte[] getReceiveAddress() {
        rlpParse();
        return receiveAddress;
    }

    public HexBytes getReceiveHex() {
        return HexBytes.fromBytes(getReceiveAddress());
    }


    public byte[] getGasPrice() {
        rlpParse();
        return gasPrice;
    }

    public Uint256 getGasPriceAsU256() {
        return Uint256.of(getGasPrice());
    }


    public byte[] getGasLimit() {
        rlpParse();
        return gasLimit;
    }

    public Uint256 getGasLimitAsU256() {
        return Uint256.of(getGasLimit());
    }


    public long nonZeroDataBytes() {
        if (data == null) return 0;
        int counter = 0;
        for (final byte aData : data) {
            if (aData != 0) ++counter;
        }
        return counter;
    }

    public long zeroDataBytes() {
        if (data == null) return 0;
        int counter = 0;
        for (final byte aData : data) {
            if (aData == 0) ++counter;
        }
        return counter;
    }

    /*
     * Crypto
     */

    public byte[] getData() {
        rlpParse();
        return data;
    }

    public HexBytes getDataHex() {
        return HexBytes.fromBytes(getData());
    }


    public ECDSASignature getSignature() {
        rlpParse();
        return signature;
    }

    public byte[] getContractAddress() {
        if (!isContractCreation()) return null;
        return HashUtil.calcNewAddr(getSender(), getNonce().length == 0 ? ZERO_BYTE_ARRAY : getNonce());
    }

    public boolean isContractCreation() {
        rlpParse();
        return this.receiveAddress == null || Arrays.equals(this.receiveAddress, EMPTY_BYTE_ARRAY);
    }

    public ECKey getKey() {
        byte[] hash = getRawHash();
        return ECKey.recoverFromSignature(signature.v, signature, hash);
    }

    public synchronized byte[] getSender() {
        try {
            if (sendAddress == null && getSignature() != null) {
                sendAddress = ECKey.signatureToAddress(getRawHash(), getSignature());
            }
            return sendAddress;
        } catch (SignatureException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public HexBytes getSenderHex() {
        return HexBytes.fromBytes(getSender());
    }

    public Integer getChainId() {
        rlpParse();
        return chainId == null ? null : chainId;
    }

    /**
     * @deprecated should prefer #sign(ECKey) over this method
     */
    @Deprecated
    public void sign(byte[] privKeyBytes) throws ECKey.MissingPrivateKeyException {
        sign(ECKey.fromPrivate(privKeyBytes));
    }

    public void sign(ECKey key) throws ECKey.MissingPrivateKeyException {
        this.signature = key.sign(this.getRawHash());
        this.rlpEncoded = null;
    }

    @Override
    public String toString() {
        return toString(Integer.MAX_VALUE);
    }

    public String toString(int maxDataSize) {
        rlpParse();
        String dataS;
        if (data == null) {
            dataS = "";
        } else if (data.length < maxDataSize) {
            dataS = ByteUtil.toHexString(data);
        } else {
            dataS = ByteUtil.toHexString(Arrays.copyOfRange(data, 0, maxDataSize)) +
                    "... (" + data.length + " bytes)";
        }
        return "TransactionData [" + "hash=" + ByteUtil.toHexString(hash) +
                "  nonce=" + ByteUtil.toHexString(nonce) +
                ", gasPrice=" + ByteUtil.toHexString(gasPrice) +
                ", gas=" + ByteUtil.toHexString(gasLimit) +
                ", receiveAddress=" + ByteUtil.toHexString(receiveAddress) +
                ", sendAddress=" + ByteUtil.toHexString(getSender()) +
                ", value=" + ByteUtil.toHexString(value) +
                ", data=" + dataS +
                ", signatureV=" + (signature == null ? "" : signature.v) +
                ", signatureR=" + (signature == null ? "" : ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(signature.r))) +
                ", signatureS=" + (signature == null ? "" : ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(signature.s))) +
                "]";
    }

    /**
     * For signatures you have to keep also
     * RLP of the transaction without any signature data
     */
    public byte[] getEncodedRaw() {

        rlpParse();
        byte[] rlpRaw;

        // parse null as 0 for nonce
        byte[] nonce = null;
        if (this.nonce == null || this.nonce.length == 1 && this.nonce[0] == 0) {
            nonce = RLPCodec.encodeBytes(null);
        } else {
            nonce = RLPCodec.encodeBytes(this.nonce);
        }
        byte[] gasPrice = RLPCodec.encodeBytes(this.gasPrice);
        byte[] gasLimit = RLPCodec.encodeBytes(this.gasLimit);
        byte[] receiveAddress = RLPCodec.encodeBytes(this.receiveAddress);
        byte[] value = RLPCodec.encodeBytes(this.value);
        byte[] data = RLPCodec.encodeBytes(this.data);

        // Since EIP-155 use chainId for v
        if (chainId == null) {
            rlpRaw = RLPCodec.encodeElements(Arrays.asList(nonce, gasPrice, gasLimit, receiveAddress, value, data));
        } else {
            byte[] v, r, s;
            v = RLPCodec.encodeInt(chainId);
            r = RLPCodec.encodeBytes(EMPTY_BYTE_ARRAY);
            s = RLPCodec.encodeBytes(EMPTY_BYTE_ARRAY);
            rlpRaw = RLPCodec.encodeElements(Arrays.asList(nonce, gasPrice, gasLimit, receiveAddress, value, data, v, r, s));
        }
        return rlpRaw;
    }

    public synchronized byte[] getEncoded() {

        if (rlpEncoded != null) return rlpEncoded;

        byte[] nonce  = RLPCodec.encodeBytes(this.nonce);
        byte[] gasPrice = RLPCodec.encodeBytes(this.gasPrice);
        byte[] gasLimit = RLPCodec.encodeBytes(this.gasLimit);
        byte[] receiveAddress = RLPCodec.encodeBytes(this.receiveAddress);
        byte[] value = RLPCodec.encodeBytes(this.value);
        byte[] data = RLPCodec.encodeBytes(this.data);

        byte[] v, r, s;

        if (signature != null) {
            int encodeV;
            if (chainId == null) {
                encodeV = signature.v;
            } else {
                encodeV = signature.v - LOWER_REAL_V;
                encodeV += chainId * 2 + CHAIN_ID_INC;
            }
            v = RLPCodec.encodeInt(encodeV);
            r = RLPCodec.encodeBytes(BigIntegers.asUnsignedByteArray(signature.r));
            s = RLPCodec.encodeBytes(BigIntegers.asUnsignedByteArray(signature.s));
        } else {
            // Since EIP-155 use chainId for v
            v = chainId == null ? RLPCodec.encodeBytes(EMPTY_BYTE_ARRAY) : RLPCodec.encodeInt(chainId);
            r = RLPCodec.encodeBytes(EMPTY_BYTE_ARRAY);
            s = RLPCodec.encodeBytes(EMPTY_BYTE_ARRAY);
        }

        this.rlpEncoded = RLPCodec.encodeElements(
                Arrays.asList(nonce, gasPrice, gasLimit,
                receiveAddress, value, data, v, r, s)
        );

        this.hash = HashUtil.sha3(rlpEncoded);

        return rlpEncoded;
    }

    @Override
    public int hashCode() {

        byte[] hash = this.getHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Transaction)) return false;
        Transaction tx = (Transaction) obj;

        return tx.hashCode() == this.hashCode();
    }

    @Override
    protected Transaction clone() {
        return new Transaction(getEncoded());
    }

    public static void main(String[] args) {

    }
}
