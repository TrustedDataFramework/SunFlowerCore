package org.tdf.common.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.function.BiFunction;

import static java.nio.file.StandardOpenOption.*;

public class JsonStore implements BatchStore<String, JsonNode> {
    private final ObjectMapper mapper;
    private final String jsonFile;
    private Map<String, JsonNode> node;

    static Set<PosixFilePermission> defaultPosixPermissions = null;

    static {
        defaultPosixPermissions = new HashSet<>();
        defaultPosixPermissions.add(PosixFilePermission.OWNER_READ);
        defaultPosixPermissions.add(PosixFilePermission.OWNER_WRITE);
    }


    public JsonStore(String jsonFile, ObjectMapper mapper) {
        this.mapper = mapper;
        this.jsonFile = jsonFile;
        load();
    }

    @SneakyThrows
    private void sync() {
        byte[] bin = mapper.writeValueAsBytes(node);
        Files.write(Paths.get(this.jsonFile), bin, CREATE, TRUNCATE_EXISTING, WRITE);
        Files.setPosixFilePermissions(Paths.get(this.jsonFile), defaultPosixPermissions);
    }

    @SneakyThrows
    private void load() {
        File f = new File(jsonFile);
        if (!f.exists()) {
            node = new HashMap<>();
            return;
        }
        ObjectNode n = null;
        try{
            n = (ObjectNode) mapper.readValue(f, JsonNode.class);
        }catch (Exception ignored){

        }
        if(n == null)
            return;
        Iterator<Map.Entry<String, JsonNode>> it = n.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            this.node.put(e.getKey(), e.getValue());
        }
    }

    @Override
    @SneakyThrows
    public Optional<JsonNode> get(String s) {
        return Optional.ofNullable(node.get(s));
    }

    @Override
    @SneakyThrows
    public void put(String s, JsonNode jsonNode) {
        node.put(s, jsonNode);
        sync();
    }

    @Override
    @SneakyThrows
    public void remove(String s) {
        node.remove(s);
        sync();
    }

    @Override
    public void flush() {
        sync();
    }

    @Override
    public void clear() {
        node = new HashMap<>();
        sync();
    }

    @Override
    @SneakyThrows
    public void traverse(BiFunction<? super String, ? super JsonNode, Boolean> traverser) {
        for (Map.Entry<String, JsonNode> entry : node.entrySet()) {
            boolean cont = traverser.apply(entry.getKey(), entry.getValue());
            if (!cont)
                return;
        }
    }

    @Override
    @SneakyThrows
    public void putAll(Collection<? extends Map.Entry<? extends String, ? extends JsonNode>> rows) {
        for (Map.Entry<? extends String, ? extends JsonNode> row : rows) {
            node.put(row.getKey(), row.getValue());
        }
        sync();
    }
}
