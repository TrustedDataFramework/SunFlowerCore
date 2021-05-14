package org.tdf.common.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

public class JsonStore implements BatchStore<String, JsonNode>, IterableStore<String, JsonNode> {
    static Set<PosixFilePermission> defaultPosixPermissions = null;

    static {
        defaultPosixPermissions = new HashSet<>();
        defaultPosixPermissions.add(PosixFilePermission.OWNER_READ);
        defaultPosixPermissions.add(PosixFilePermission.OWNER_WRITE);
    }

    private final ObjectMapper mapper;
    private final String jsonFile;
    private Map<String, JsonNode> node;


    public JsonStore(String jsonFile, ObjectMapper mapper) {
        this.mapper = mapper;
        this.jsonFile = jsonFile;
        load();
    }

    @SneakyThrows
    private void sync() {
        if (jsonFile.equals("$memory"))
            return;
        byte[] bin = mapper.writeValueAsBytes(node);
        Files.write(Paths.get(this.jsonFile), bin, CREATE, TRUNCATE_EXISTING, WRITE);
        Files.setPosixFilePermissions(Paths.get(this.jsonFile), defaultPosixPermissions);
    }

    @SneakyThrows
    private void load() {
        this.node = new HashMap<>();
        if (jsonFile.equals("$memory"))
            return;
        File f = new File(jsonFile);
        if (!f.exists()) {
            return;
        }
        ObjectNode n = null;
        try {
            n = (ObjectNode) mapper.readValue(f, JsonNode.class);
        } catch (Exception ignored) {

        }
        if (n == null) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = n.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            this.node.put(e.getKey(), e.getValue());
        }
    }

    @Override
    @SneakyThrows
    public JsonNode get(@NonNull String s) {
        return node.get(s);
    }

    @Override
    @SneakyThrows
    public void set(@NonNull String s, @NonNull JsonNode jsonNode) {
        node.put(s, jsonNode);
        sync();
    }

    @Override
    @SneakyThrows
    public void remove(@NonNull String s) {
        node.remove(s);
        sync();
    }

    @Override
    public void flush() {
        sync();
    }


    @Override
    @SneakyThrows
    public void putAll(@NonNull Collection<? extends Map.Entry<? extends String, ? extends JsonNode>> rows) {
        for (Map.Entry<? extends String, ? extends JsonNode> row : rows) {
            node.put(Objects.requireNonNull(row.getKey()), Objects.requireNonNull(row.getValue()));
        }
        sync();
    }


    @Override
    public Iterator<Map.Entry<String, JsonNode>> iterator() {
        return node.entrySet().iterator();
    }
}
