package org.tdf.consortium.state;

import org.tdf.consortium.account.PublicKeyHash;
import org.tdf.serialize.SerializeDeserializer;
import org.tdf.util.BufferUtil;

public class AccountSerializerDeserializer implements SerializeDeserializer<Account> {
    @Override
    public byte[] serialize(Account account) {
        BufferUtil util = BufferUtil.newWriteOnly();
        util.putBytes(account.getPublicKeyHash().getPublicKeyHash());
        util.putLong(account.getBalance());
        if (account.getBinaryContract() == null) return util.toByteArray();
        util.putBytes(account.getBinaryContract());
        if (account.getMemory() == null) return util.toByteArray();
        util.putBytes(account.getMemory());
        if (account.getGlobals() == null) return util.toByteArray();
        util.putLongs(account.getGlobals());
        return util.toByteArray();
    }

    @Override
    public Account deserialize(byte[] data) {
        BufferUtil util = BufferUtil.newReadOnly(data);
        PublicKeyHash publicKeyHash = new PublicKeyHash(util.getBytes());
        long balance = util.getLong();
        byte[] binaryContract = null;
        byte[] memory = null;
        long[] globals = null;
        if (util.remaining() > 0) {
            binaryContract = util.getBytes();
        }
        if (util.remaining() > 0) {
            memory = util.getBytes();
        }
        if (util.remaining() > 0) {
            globals = util.getLongs();
        }
        return new Account(publicKeyHash, balance, binaryContract, memory, globals);
    }
}
