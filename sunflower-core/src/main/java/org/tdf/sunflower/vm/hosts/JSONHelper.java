package org.tdf.sunflower.vm.hosts;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
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
    private enum Type {
        JSON, STRING, I64, U64, BOOL, F64;
    }

    private static final Gson GSON = new Gson();
    // singleton builder target
    private JsonElement element;
    private String built;

    public List<HostFunction> getHelpers() {
        return Arrays.asList(
                new JSONBuilderPut(this),
                new JSONBuilderBuild(this),
                new JSONBuilderBuildLength(this),
                new JSONBuilderPut(this),
                new JSONBuilderSet(this),
                new JSONReaderGetByKey(),
                new JSONReaderGetByIndex()
        );
    }

    public void clearBuilder() {
        element = null;
        built = null;
    }

    public void ensureJSONObject() {
        if (element != null && element.isJsonArray()) throw new RuntimeException("cannot put element to json array");
        if (element == null) element = GSON.toJsonTree(new HashMap<>());
    }

    public void ensureJSONArray(int size) {
        if (element != null && element.isJsonObject()) throw new RuntimeException("cannot push element to json object");
        if (element == null) element = GSON.toJsonTree(new ArrayList<>());
        while (element.getAsJsonArray().size() <= size) {
            element.getAsJsonArray().add(JsonNull.INSTANCE);
        }
    }

    private static class JSONBuilderPut extends HostFunction {
        private JSONHelper jsonHelper;

        JSONBuilderPut(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_put");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I64, ValueType.I64
                            ),
                            Collections.emptyList()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            Type t = Type.values()[(int) parameters[0]];
            String key = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            jsonHelper.ensureJSONObject();

            switch (t) {
                case JSON: {
                    String value = loadStringFromMemory((int) parameters[3], (int) parameters[4]);
                    jsonHelper.element.getAsJsonObject().add(key, GSON.fromJson(value, JsonElement.class));
                    break;
                }
                case STRING: {
                    String value = loadStringFromMemory((int) parameters[3], (int) parameters[4]);
                    jsonHelper.element.getAsJsonObject().addProperty(key, value);
                    break;
                }
                case I64: {
                    jsonHelper.element.getAsJsonObject().addProperty(key, parameters[3]);
                    break;
                }
                case U64: {
                    jsonHelper.element.getAsJsonObject().addProperty(key, UnsignedLong.fromLongBits(parameters[3]));
                    break;
                }
                case BOOL: {
                    jsonHelper.element.getAsJsonObject().addProperty(key, parameters[3] != 0);
                    break;
                }
                case F64: {
                    jsonHelper.element.getAsJsonObject()
                            .addProperty(key, BigDecimal.valueOf(Double.longBitsToDouble(parameters[3])));
                    break;
                }
                default: {
                    throw new RuntimeException("unreachable");
                }
            }
            return new long[0];
        }
    }


    private static class JSONBuilderSet extends HostFunction {

        private JSONHelper jsonHelper;

        public JSONBuilderSet(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;
            setName("_json_builder_set");
            setType(
                    new FunctionType(
                            Arrays.asList(
                                    ValueType.I32,
                                    ValueType.I32, ValueType.I64, ValueType.I64
                            ),
                            Collections.emptyList()
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            Type t = Type.values()[(int) parameters[0]];
            int index = (int) parameters[1];
            jsonHelper.ensureJSONArray(index);

            switch (t) {
                case JSON: {
                    String value = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
                    jsonHelper.element.getAsJsonArray().set(index, GSON.fromJson(value, JsonElement.class));
                    break;
                }
                case STRING: {
                    String value = loadStringFromMemory((int) parameters[2], (int) parameters[3]);
                    jsonHelper.element.getAsJsonArray().set(index, new JsonPrimitive(value));
                    break;
                }
                case U64:
                case I64: {
                    jsonHelper.element.getAsJsonArray().set(index, new JsonPrimitive(parameters[2]));
                    break;
                }
                case F64: {
                    jsonHelper.element.getAsJsonArray()
                            .set(index, new JsonPrimitive(Double.longBitsToDouble(parameters[2])));
                    break;
                }
                case BOOL: {
                    jsonHelper.element.getAsJsonArray()
                            .set(index, new JsonPrimitive(parameters[2] != 0));
                    break;
                }
            }
            return new long[0];
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

    private static class JSONReaderGetByKey extends HostFunction {

        public JSONReaderGetByKey() {
            setName("_json_reader_get_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(
                                    ValueType.I32,
                                    ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32,
                                    ValueType.I64
                            ),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            String key = loadStringFromMemory((int) parameters[3], (int) parameters[4]);
            Type t = Type.values()[(int) parameters[0]];
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            switch (t) {
                case JSON: {
                    int ptr = (int) parameters[5];
                    String ret = GSON.toJson(element);
                    if(parameters[6] != 0) putStringIntoMemory(ptr, ret);
                    return new long[]{ret.length()};
                }
                case STRING: {
                    int ptr = (int) parameters[5];
                    if(parameters[6] != 0) putStringIntoMemory(ptr, element.getAsString());
                    return new long[]{element.getAsString().length()};
                }
                case BOOL: {
                    return new long[]{element.getAsBoolean() ? 1 : 0};
                }
                case U64:
                case I64: {
                    return new long[]{element.getAsLong()};
                }
                case F64: {
                    return new long[]{Double.doubleToLongBits(element.getAsDouble())};
                }
                default:
                    throw new RuntimeException("unreachable");
            }
        }
    }

    private static class JSONReaderGetByIndex extends HostFunction {

        public JSONReaderGetByIndex() {
            setName("_json_reader_get_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(
                                    ValueType.I32,
                                    ValueType.I32, ValueType.I32,
                                    ValueType.I32,
                                    ValueType.I32, ValueType.I64
                            ),
                            Collections.singletonList(ValueType.I64)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            Type t = Type.values()[(int) parameters[0]];
            String json = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            JsonElement element = GSON
                    .fromJson(json, JsonElement.class)
                    .getAsJsonArray().get((int) parameters[3]);
            int ptr = (int) parameters[4];
            switch (t) {
                case JSON: {
                    String ret = GSON.toJson(element);
                    if(parameters[5] != 0) putStringIntoMemory(ptr, ret);
                    return new long[]{ret.length()};
                }
                case STRING: {
                    if(parameters[5] != 0) putStringIntoMemory(ptr, element.getAsString());
                    return new long[]{element.getAsString().length()};
                }
                case BOOL: {
                    return new long[]{element.getAsBoolean() ? 1 : 0};
                }
                case U64:
                case I64:{
                    return new long[]{element.getAsLong()};
                }
                case F64:{
                    return new long[]{Double.doubleToLongBits(element.getAsDouble())};
                }
                default:
                    throw new RuntimeException("unreachable");
            }
        }
    }

    private static class JSONReaderGetLenByKey extends HostFunction {

        public JSONReaderGetLenByKey() {
            setName("_json_reader_get_len_by_key");
            setType(
                    new FunctionType(
                            Arrays.asList(
                                    ValueType.I32,
                                    ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            String key = loadStringFromMemory((int) parameters[3], (int) parameters[4]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonObject().get(key);
            Type t = Type.values()[(int) parameters[0]];
            switch (t) {
                case JSON:
                    return new long[]{GSON.toJson(element).getBytes(StandardCharsets.UTF_8).length};
                case STRING:
                    return new long[]{element.getAsString().getBytes(StandardCharsets.UTF_8).length};
            }
            throw new RuntimeException("unreachable");
        }
    }


    private static class JSONReaderGetLenByIndex extends HostFunction {

        public JSONReaderGetLenByIndex() {
            setName("_json_reader_get_len_by_index");
            setType(
                    new FunctionType(
                            Arrays.asList(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32),
                            Collections.singletonList(ValueType.I32)
                    )
            );
        }

        @Override
        public long[] execute(long... parameters) {
            String json = loadStringFromMemory((int) parameters[1], (int) parameters[2]);
            JsonElement element = GSON.fromJson(json, JsonElement.class).getAsJsonArray().get((int) parameters[3]);
            Type t = Type.values()[(int) parameters[0]];
            switch (t) {
                case JSON:
                    return new long[]{GSON.toJson(element).getBytes(StandardCharsets.UTF_8).length};
                case STRING:
                    return new long[]{element.getAsString().getBytes(StandardCharsets.UTF_8).length};
            }
            throw new RuntimeException("unreachable");
        }
    }
}
