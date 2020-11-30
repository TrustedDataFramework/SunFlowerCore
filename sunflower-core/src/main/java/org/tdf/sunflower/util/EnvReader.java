package org.tdf.sunflower.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.env.Environment;
import org.tdf.sunflower.Start;

import java.io.InputStream;
import java.net.URI;

@RequiredArgsConstructor
public class EnvReader {
    private final Environment env;

    // 哈希算法
    public String getHash(){
        String hash = env.getProperty("sunflower.crypto.hash");
        hash = (hash == null || hash.isEmpty()) ? "sm3" : hash;
        hash = hash.toLowerCase();
        return hash;
    }

    // 签名算法
    public String getEC(){
        String ec = env.getProperty("sunflower.crypto.ec");
        ec = (ec == null || ec.isEmpty()) ? "sm2" : ec;
        ec = ec.toLowerCase();
        return ec;
    }

    // 对称加密算法
    public String getAE(){
        return "sm4";
    }

    @SneakyThrows
    public JsonNode getGenesis(){
        InputStream in = FileUtils.getInputStream(env.getProperty("sunflower.consensus.genesis"));
        return Start.MAPPER.readValue(in, JsonNode.class);
    }

    public int getBlockInterval(){
        return Integer.parseInt(env.getProperty("sunflower.consensus.block-interval"));
    }

    public int getP2PPort(){
        int p = URI.create(env.getProperty("sunflower.p2p.address")).getPort();
        return p < 0 ? 7000 : p;
    }
}
