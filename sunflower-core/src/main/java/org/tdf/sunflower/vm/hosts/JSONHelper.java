package org.tdf.sunflower.vm.hosts;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.*;
import org.tdf.lotusvm.runtime.HostFunction;
import org.tdf.lotusvm.types.FunctionType;
import org.tdf.lotusvm.types.ValueType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Standard built-in json library for smart contract development
 */
public class JSONHelper {
    private static final Gson GSON = new Gson();
    // singleton builder target
    private JsonElement element;
    private String built;

    public List<HostFunction> getHelpers() {
        return Arrays.asList(
                new JSONBuilderPutJSON(this),
                new JSONBuilderBuild(this),
                new JSONBuilderBuildArraySize(this),
                new JSONBuilderBuildLength(this),
                new JSONBuilderPutBoolean(this),
                new JSONBuilderPutF64(this),
                new JSONBuilderPutI64(this),
                new JSONBuilderPutJSON(this),
                new JSONBuilderPutString(this),
                new JSONBuilderPutU64(this),
                new JSONBuilderSetBoolean(this),
                new JSONBuilderSetF64(this),
                new JSONBuilderSetString(this),
                new JSONBuilderSetI64(this),
                new JSONBuilderSetJSON(this),
                new JSONReaderGetJSONByKey(),
                new JSONReaderGetJSONLenByKey(),
                new JSONReaderGetBooleanByIndex(),
                new JSONReaderGetBooleanByKey(),
                new JSONReaderGetU64ByKey(),
                new JSONReaderGetStrLenByIndex(),
                new JSONReaderGetF64ByKey(),
                new JSONReaderGetF64ByIndex(),
                new JSONReaderGetU64ByIndex(),
                new JSONBuilderPutU64(this),
                new JSONBuilderSetU64(this),
                new JSONReaderGetStrByIndex(),
                new JSONReaderGetI64ByIndex(),
                new JSONReaderGetJsonByIndex(),
                new JSONReaderGetStrByKey(),
                new JSONReaderGetJsonLenByIndex(),
                new JSONReaderGetI64ByKey(),
                new JSONReaderGetStrLenByKey()
        );
    }

    public void clearBuilder() {
        element = null;
        built = null;
    }

    public void ensureJSONObject() {
        if (element != null && element.isJsonArray()) throw new RuntimeException("cannot put element to json array");
        if (element == null) element = GSON.toJsonTree(new HashMap());
    }

    public void ensureJSONArray(int size) {
        if (element != null && element.isJsonObject()) throw new RuntimeException("cannot push element to json object");
        if (element == null) element = GSON.toJsonTree(new ArrayList());
        while (element.getAsJsonArray().size() <= size){
            element.getAsJsonArray().add(JsonNull.INSTANCE);
        }
    }

    private static class JSONBuilderPutJSON extends HostFunction {
        private JSONHelper jsonHelper;

        JSONBuilderPutJSON(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_put_json");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONObject();
            String key = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String value = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            jsonHelper.element.getAsJsonObject().add(key, GSON.fromJson(value, JsonElement.class));

            return new long[0];
        }
    }

    private static class JSONBuilderPutString extends HostFunction {
        private JSONHelper jsonHelper;

        public JSONBuilderPutString(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_put_str");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONObject();
            String key = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String value = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            jsonHelper.element.getAsJsonObject().addProperty(key, value);
            return new long[0];
        }
    }

    private static class JSONBuilderPutI64 extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderPutI64(JSONHelper jsonHelper) {
            setName("_json_builder_put_i64");
            this.jsonHelper = jsonHelper;
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONObject();
            String key = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            jsonHelper.element.getAsJsonObject().addProperty(key, parameters[2]);
            return new long[0];
        }
    }

    private static class JSONBuilderPutU64 extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderPutU64(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_put_u64");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONObject();
            String key = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            jsonHelper.element.getAsJsonObject().addProperty(key, UnsignedLong.fromLongBits(parameters[2]));
            return new long[0];
        }
    }

    private static class JSONBuilderPutBoolean extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderPutBoolean(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_put_bool");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONObject();
            String key = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            jsonHelper.element.getAsJsonObject().addProperty(key, parameters[2] != 0);
            return new long[0];
        }
    }

    private static class JSONBuilderPutF64 extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderPutF64(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_put_f64");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.F64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONObject();
            String key = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            jsonHelper.element.getAsJsonObject().addProperty(key, new BigDecimal(Double.longBitsToDouble(parameters[2])));
            return new long[0];
        }
    }

    private static class JSONBuilderSetJSON extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSetJSON(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set_json");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONArray((int) parameters[0]);
            String value = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            jsonHelper.element.getAsJsonArray().set((int) parameters[0], GSON.fromJson(value, JsonElement.class));
            return new long[0];
        }
    }

    private static class JSONBuilderSetString extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSetString(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set_str");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONArray((int) parameters[0]);
            String value = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            jsonHelper.element.getAsJsonArray().set((int) parameters[0], new JsonPrimitive(value));
            return new long[0];
        }
    }

    private static class JSONBuilderSetI64 extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSetI64(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set_i64");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONArray((int) parameters[0]);
            jsonHelper.element.getAsJsonArray().set((int) parameters[0], new JsonPrimitive(parameters[1]));
            return new long[0];
        }
    }

    private static class JSONBuilderSetU64 extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSetU64(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set_u64");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONArray((int) parameters[0]);
            jsonHelper.element.getAsJsonArray().set((int) parameters[0], new JsonPrimitive(parameters[1]));
            return new long[0];
        }
    }

    private static class JSONBuilderSetBoolean extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSetBoolean(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set_bool");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONArray((int) parameters[0]);
            JsonArray array = jsonHelper.element.getAsJsonArray();
            jsonHelper.element.getAsJsonArray().set((int) parameters[0], new JsonPrimitive(parameters[1] != 0));
            return new long[0];
        }
    }

    private static class JSONBuilderSetF64 extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSetF64(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set_f64");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.F64),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            jsonHelper.ensureJSONArray((int) parameters[0]);
            jsonHelper.element.getAsJsonArray().set((int) parameters[0], new JsonPrimitive(Double.longBitsToDouble(parameters[1])));
            return new long[0];
        }
    }

    private static class JSONBuilderBuildArraySize extends HostFunction {
        private JSONHelper jsonHelper;

        JSONBuilderBuildArraySize(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_array_size");
            setType(
                    new FunctionType(
                            new ArrayList<>(),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            if (jsonHelper.element == null) throw new RuntimeException("cannot build json without any put or set");
            jsonHelper.built = GSON.toJson(jsonHelper.element);
            return new long[]{jsonHelper.element.getAsJsonArray().size()};
        }
    }


    private static class JSONBuilderBuild extends HostFunction {
        private JSONHelper jsonHelper;

        JSONBuilderBuild(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_build");
            setType(
                    new FunctionType(
                            Collections.singletonList(ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            if (jsonHelper.built == null)
                throw new RuntimeException("call _json_builder_build before _json_builder_build_len");
            int ptr = (int) parameters[0];
            putStringIntoMemory(ptr, jsonHelper.built);
            jsonHelper.clearBuilder();
            return new long[0];
        }
    }

    private static class JSONBuilderBuildLength extends HostFunction {
        private JSONHelper jsonHelper;

        JSONBuilderBuildLength(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_build_len");
            setType(
                    new FunctionType(
                            new ArrayList<>(),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            if (jsonHelper.element == null) throw new RuntimeException("cannot build json without any put or set");
            String json = GSON.toJson(jsonHelper.element);
            jsonHelper.built = json;
            return new long[]{json.getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class JSONReaderGetJSONByKey extends HostFunction {

        public JSONReaderGetJSONByKey() {
            setName("_json_reader_get_json_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            int ptr = (int) parameters[4];
            putStringIntoMemory(ptr, GSON.toJson(element));
            return new long[0];
        }
    }

    private static class JSONReaderGetJSONLenByKey extends HostFunction {

        public JSONReaderGetJSONLenByKey() {
            setName("_json_reader_get_json_len_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            return new long[]{GSON.toJson(element).getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class JSONReaderGetStrByKey extends HostFunction {

        public JSONReaderGetStrByKey() {
            setName("_json_reader_get_str_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            int ptr = (int) parameters[4];
            putStringIntoMemory(ptr, element.getAsString());
            return new long[0];
        }
    }

    private static class JSONReaderGetStrLenByKey extends HostFunction {

        public JSONReaderGetStrLenByKey() {
            setName("_json_reader_get_str_len_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            return new long[]{element.getAsString().getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class JSONReaderGetI64ByKey extends HostFunction {

        public JSONReaderGetI64ByKey() {
            setName("_json_reader_get_i64_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            return new long[]{element.getAsLong()};
        }
    }

    private static class JSONReaderGetU64ByKey extends HostFunction {

        public JSONReaderGetU64ByKey() {
            setName("_json_reader_get_u64_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            return new long[]{element.getAsLong()};
        }
    }

    private static class JSONReaderGetBooleanByKey extends HostFunction {

        public JSONReaderGetBooleanByKey() {
            setName("_json_reader_get_bool_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            return new long[]{element.getAsBoolean() ? 1 : 0};
        }
    }

    private static class JSONReaderGetF64ByKey extends HostFunction {

        public JSONReaderGetF64ByKey() {
            setName("_json_reader_get_f64_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.F64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            String key = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            return new long[]{Double.doubleToLongBits(element.getAsDouble())};
        }
    }

    private static class JSONReaderGetJsonByIndex extends HostFunction {

        public JSONReaderGetJsonByIndex() {
            setName("_json_reader_get_json_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            int ptr = (int) parameters[3];
            putStringIntoMemory(ptr, element.getAsJsonObject().getAsString());
            return new long[0];
        }
    }

    private static class JSONReaderGetJsonLenByIndex extends HostFunction {

        public JSONReaderGetJsonLenByIndex() {
            setName("_json_reader_get_json_len_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            return new long[element.getAsJsonObject().getAsString().getBytes(StandardCharsets.UTF_8).length];
        }
    }

    private static class JSONReaderGetStrByIndex extends HostFunction {

        public JSONReaderGetStrByIndex() {
            setName("_json_reader_get_str_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            new ArrayList<>()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            int ptr = (int) parameters[3];
            putStringIntoMemory(ptr, element.getAsString());
            return new long[0];
        }
    }

    private static class JSONReaderGetStrLenByIndex extends HostFunction {

        public JSONReaderGetStrLenByIndex() {
            setName("_json_reader_get_str_len_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            return new long[]{element.getAsString().getBytes(StandardCharsets.UTF_8).length};
        }
    }

    private static class JSONReaderGetI64ByIndex extends HostFunction {

        public JSONReaderGetI64ByIndex() {
            setName("_json_reader_get_i64_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            return new long[]{element.getAsLong()};
        }
    }

    private static class JSONReaderGetU64ByIndex extends HostFunction {

        public JSONReaderGetU64ByIndex() {
            setName("_json_reader_get_u64_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            return new long[]{element.getAsLong()};
        }
    }

    private static class JSONReaderGetBooleanByIndex extends HostFunction {

        public JSONReaderGetBooleanByIndex() {
            setName("_json_reader_get_bool_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            return new long[]{element.getAsBoolean() ? 1 : 0};
        }
    }

    private static class JSONReaderGetF64ByIndex extends HostFunction {

        public JSONReaderGetF64ByIndex() {
            setName("_json_reader_get_f64_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.F64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[0], (int) parameters[1]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[2]);
            return new long[]{Double.doubleToLongBits(element.getAsDouble())};
        }
    }

}
