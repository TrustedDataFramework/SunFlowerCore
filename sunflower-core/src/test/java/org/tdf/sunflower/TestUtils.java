package org.tdf.sunflower;

import com.google.common.io.ByteStreams;
import org.tdf.common.util.BigEndian;
import org.tdf.sunflower.dao.Mapping;
import org.tdf.sunflower.entity.HeaderEntity;
import org.tdf.sunflower.entity.TransactionEntity;
import org.tdf.sunflower.types.Block;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TestUtils {
    static final byte[] BYTES = new byte[32];

    static Block getBlock(long height) {
        HeaderEntity h = new HeaderEntity(
            BigEndian.encodeInt64(height), 1, height == 0 ? BYTES : BigEndian.encodeInt64(height - 1),
            BYTES, BYTES, height, System.currentTimeMillis() / 1000, BYTES
        );
        TransactionEntity.TransactionEntityBuilder builder =
            TransactionEntity.builder().blockHash(BigEndian.encodeInt64(height))
                .height(height)
                .from(BYTES).payload(BYTES).to(BYTES)
                .signature(BYTES);
        Block b = new Block(Mapping.getFromHeaderEntity(h));
        b.setBody(Arrays.asList(
            builder.position(0).hash((height + "" + 0).getBytes()).build(),
            builder.position(1).hash((height + "" + 1).getBytes()).build(),
            builder.position(2).hash((height + "" + 2).getBytes()).build()
        ).stream().map(Mapping::getFromTransactionEntity).collect(Collectors.toList()));
        return b;
    }

    public static void main(String[] args) {
        print(new ISUB());
    }

    static void print(IN in) {
        System.out.println("in");
    }

    static void print(INIM in) {
        System.out.println("inim");
    }

    public static File readClassPathFile(String name) {
        return new File(TestUtils.class.getClassLoader().getResource(name).getFile());
    }

    public static byte[] readClassPathFileAsByteArray(String name) throws IOException {
        return ByteStreams.toByteArray(new FileInputStream(readClassPathFile(name)));
    }

    public static interface IN {
    }

    public static interface INIM extends IN {
    }

    public static class I implements IN {
    }

    public static class ISUB implements INIM {
    }
}
