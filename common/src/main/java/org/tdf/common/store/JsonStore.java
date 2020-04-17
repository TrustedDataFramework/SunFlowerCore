package org.tdf.common.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class JsonStore implements Store<String, JsonNode> {
    private final ObjectMapper mapper;
    private final String jsonFile;

    public JsonStore(String jsonFile, ObjectMapper mapper) {
        this.mapper = mapper;
        this.jsonFile = jsonFile;
    }

    @Override
    @SneakyThrows
    public Optional<JsonNode> get(String s) {
        File f = new File(jsonFile);
        if (!f.exists())
            return Optional.empty();
        JsonNode n = mapper.readValue(f, JsonNode.class);
        return Optional.ofNullable(n.get(s));
    }

    @Override
    @SneakyThrows
    public void put(String s, JsonNode jsonNode) {
        File f = new File(jsonFile);
        Map<String, Object> m = new HashMap<>();
        if (f.exists())
            m = mapper.readValue(f, Map.class);
        m.put(s, jsonNode);
        try (
                OutputStream os = new FileOutputStream(f)
        ) {
            mapper.writeValue(os, m);
        }
    }

    @Override
    @SneakyThrows
    public void remove(String s) {
        File f = new File(jsonFile);
        Map<String, Object> m = new HashMap<>();
        if (f.exists())
            m = mapper.readValue(f, Map.class);

        m.remove(s);
        mapper.writeValue(f, m);
    }

    @Override
    public void flush() {

    }

    @Override
    public void clear() {
        File f = new File(jsonFile);
        if (f.exists())
            f.delete();
    }

    @Override
    @SneakyThrows
    public void traverse(BiFunction<? super String, ? super JsonNode, Boolean> traverser) {
        File f = new File(jsonFile);
        Map<String, JsonNode> m = new HashMap<>();
        if (f.exists())
            m = mapper.readValue(f, new TypeReference<Map<String, JsonNode>>() {
            });
        for (Map.Entry<String, JsonNode> entry : m.entrySet()) {
            boolean cont = traverser.apply(entry.getKey(), entry.getValue());
            if (!cont)
                return;
        }
    }
}
