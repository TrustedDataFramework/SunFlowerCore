package org.tdf.common.util;

import com.google.common.primitives.Bytes;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Merkle {
    public static byte[] getMerkleRoot(@NonNull List<byte[]> hashes, Function<byte[], byte[]> hashFunction) {
        if (hashes.size() == 0) throw new RuntimeException("no hash found");
        if (hashes.size() == 1) return hashFunction.apply(hashes.get(0));
        return calculateMerkleRoot(hashes, hashFunction);
    }

    private static byte[] calculateMerkleRoot(List<byte[]> hashes, Function<byte[], byte[]> hashFunction) {
        if (hashes.size() == 1) return hashes.get(0);
        if (hashes.size() % 2 == 1) hashes.add(hashes.get(hashes.size() - 1));
        List<byte[]> res = new ArrayList<>(hashes.size() / 2);
        for (int i = 0; i < hashes.size(); i += 2){
            res.add(hashFunction.apply(Bytes.concat(hashes.get(i), hashes.get(i + 1))));
        }
        return calculateMerkleRoot(res, hashFunction);
    }
}
