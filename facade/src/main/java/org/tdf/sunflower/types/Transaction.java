package org.tdf.sunflower.types;

import com.github.salpadding.rlpstream.*;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import lombok.Builder;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.tdf.common.crypto.ECDSASignature;
import org.tdf.common.crypto.ECKey;
import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;
import org.tdf.common.trie.TrieImpl;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.BigIntegers;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tdf.common.util.ByteUtil.ZERO_BYTE_ARRAY;

public class Transaction implements RlpWritable {
    @Override
    public int writeToBuf(RlpBuffer rlpBuffer) {
        return rlpBuffer.writeRaw(getEncoded());
    }

    public static final class TransactionCodec implements Codec<Transaction> {
        public static Function<? super Transaction, byte[]> ENCODER = Transaction::getEncoded;
        public static Function<byte[], ? extends Transaction> DECODER = Transaction::new;

        @Override
        public Function<? super Transaction, byte[]> getEncoder() {
            return ENCODER;
        }

        @Override
        public Function<byte[], ? extends Transaction> getDecoder() {
            return DECODER;
        }
    }

    private static final Logger log = org.slf4j.LoggerFactory.getLogger("tx");

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static HexBytes calcTxTrie(List<Transaction> transactions) {
        Trie<byte[], byte[]> txsState = new TrieImpl<>();

        if (transactions == null || transactions.isEmpty())
            return HexBytes.fromBytes(HashUtil.EMPTY_TRIE_HASH);

        for (int i = 0; i < transactions.size(); i++) {
            txsState.set(Rlp.encodeInt(i), transactions.get(i).getEncoded());
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

    @RlpCreator
    public static Transaction fromRlpStream(byte[] bin, long streamId) {
        return new Transaction(StreamId.rawOf(bin, streamId));
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


    public synchronized void verify() {
        rlpParse();
        validate();
    }

    public synchronized void rlpParse() {
        if (parsed) return;
        try {
            RlpList li = Rlp.decodeList(rlpEncoded);

            // Basic verification
            if (li.size() > 9) throw new RuntimeException("Too many RLP elements");

            this.nonce = li.bytesAt(0);
            this.gasPrice = li.bytesAt(1);
            this.gasLimit = li.bytesAt(2);
            if(gasLimit.length > 8 || (gasLimit.length == 8 && gasLimit[0] < 0))
                throw new RuntimeException("gas limit exceeds Long.MAX_VALUE");

            this.receiveAddress = li.bytesAt(3);
            this.value = li.bytesAt(4);
            this.data = li.bytesAt(5);
            // only parse signature in case tx is signed
            if (li.size() >= 7 && li.bytesAt(6).length != 0) {
                byte[] vData = li.bytesAt(6);
                BigInteger v = ByteUtil.bytesToBigInteger(vData);
                byte[] r = li.bytesAt(7);
                byte[] s = li.bytesAt(8);
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

    @SneakyThrows
    public boolean verifySig() {
        ECKey key = ECKey.signatureToKey(getRawHash(), getSignature());
        // verify signature
        return key.verify(getRawHash(), getSignature());
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
        if (hash != null && hash.length != 0)
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

    @SneakyThrows
    public synchronized byte[] getSender() {
        if (sendAddress == null && getSignature() != null) {
            sendAddress = ECKey.signatureToAddress(getRawHash(), getSignature());
        }
        return sendAddress;
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
            nonce = Rlp.encodeBytes(null);
        } else {
            nonce = Rlp.encodeBytes(this.nonce);
        }
        byte[] gasPrice = Rlp.encodeBytes(this.gasPrice);
        byte[] gasLimit = Rlp.encodeBytes(this.gasLimit);
        byte[] receiveAddress = Rlp.encodeBytes(this.receiveAddress);
        byte[] value = Rlp.encodeBytes(this.value);
        byte[] data = Rlp.encodeBytes(this.data);

        // Since EIP-155 use chainId for v
        if (chainId == null) {
            rlpRaw = Rlp.encodeElements(nonce, gasPrice, gasLimit, receiveAddress, value, data);
        } else {
            byte[] v, r, s;
            v = Rlp.encodeInt(chainId);
            r = Rlp.encodeBytes(EMPTY_BYTE_ARRAY);
            s = Rlp.encodeBytes(EMPTY_BYTE_ARRAY);
            rlpRaw = Rlp.encodeElements(nonce, gasPrice, gasLimit, receiveAddress, value, data, v, r, s);
        }
        return rlpRaw;
    }

    public synchronized byte[] getEncoded() {

        if (rlpEncoded != null) return rlpEncoded;

        byte[] nonce = Rlp.encodeBytes(this.nonce);
        byte[] gasPrice = Rlp.encodeBytes(this.gasPrice);
        byte[] gasLimit = Rlp.encodeBytes(this.gasLimit);
        byte[] receiveAddress = Rlp.encodeBytes(this.receiveAddress);
        byte[] value = Rlp.encodeBytes(this.value);
        byte[] data = Rlp.encodeBytes(this.data);

        byte[] v, r, s;

        if (signature != null) {
            int encodeV;
            if (chainId == null) {
                encodeV = signature.v;
            } else {
                encodeV = signature.v - LOWER_REAL_V;
                encodeV += chainId * 2 + CHAIN_ID_INC;
            }
            v = Rlp.encodeInt(encodeV);
            r = Rlp.encodeBytes(BigIntegers.asUnsignedByteArray(signature.r));
            s = Rlp.encodeBytes(BigIntegers.asUnsignedByteArray(signature.s));
        } else {
            // Since EIP-155 use chainId for v
            v = chainId == null ? Rlp.encodeBytes(EMPTY_BYTE_ARRAY) : Rlp.encodeInt(chainId);
            r = Rlp.encodeBytes(EMPTY_BYTE_ARRAY);
            s = Rlp.encodeBytes(EMPTY_BYTE_ARRAY);
        }

        this.rlpEncoded = Rlp.encodeElements(
                nonce, gasPrice, gasLimit,
                receiveAddress, value, data, v, r, s
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

    public static class TransactionBuilder {
        private byte[] nonce;
        private byte[] gasPrice;
        private byte[] gasLimit;
        private byte[] receiveAddress;
        private byte[] value;
        private byte[] data;

        TransactionBuilder() {
        }

        public Transaction.TransactionBuilder nonce(byte[] nonce) {
            this.nonce = nonce;
            return this;
        }

        public Transaction.TransactionBuilder gasPrice(byte[] gasPrice) {
            this.gasPrice = gasPrice;
            return this;
        }

        public Transaction.TransactionBuilder gasLimit(byte[] gasLimit) {
            this.gasLimit = gasLimit;
            return this;
        }

        public Transaction.TransactionBuilder receiveAddress(byte[] receiveAddress) {
            this.receiveAddress = receiveAddress;
            return this;
        }

        public Transaction.TransactionBuilder value(byte[] value) {
            this.value = value;
            return this;
        }

        public Transaction.TransactionBuilder data(byte[] data) {
            this.data = data;
            return this;
        }

        public Transaction build() {
            return new Transaction(nonce, gasPrice, gasLimit, receiveAddress, value, data);
        }

        public String toString() {
            return "Transaction.TransactionBuilder(nonce=" + Arrays.toString(this.nonce) + ", gasPrice=" + Arrays.toString(this.gasPrice) + ", gasLimit=" + Arrays.toString(this.gasLimit) + ", receiveAddress=" + Arrays.toString(this.receiveAddress) + ", value=" + Arrays.toString(this.value) + ", data=" + Arrays.toString(this.data) + ")";
        }
    }
}
