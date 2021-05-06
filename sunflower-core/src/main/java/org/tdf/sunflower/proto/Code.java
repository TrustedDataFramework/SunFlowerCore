// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sunflower.proto

package org.tdf.sunflower.proto;

/**
 * Protobuf enum {@code Code}
 */
public enum Code
    implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <pre>
     * empty message
     * </pre>
     *
     * <code>NOTHING = 0;</code>
     */
    NOTHING(0),
    /**
     * <pre>
     * ping message, open a channel
     * </pre>
     *
     * <code>PING = 1;</code>
     */
    PING(1),
    /**
     * <pre>
     * pong message, response to a channel opened event
     * </pre>
     *
     * <code>PONG = 2;</code>
     */
    PONG(2),
    /**
     * <pre>
     * query for peers
     * </pre>
     *
     * <code>LOOK_UP = 3;</code>
     */
    LOOK_UP(3),
    /**
     * <pre>
     * response to peers query
     * </pre>
     *
     * <code>PEERS = 4;</code>
     */
    PEERS(4),
    /**
     * <pre>
     * used for application handlers
     * </pre>
     *
     * <code>ANOTHER = 5;</code>
     */
    ANOTHER(5),
    /**
     * <pre>
     * disconnect message, prompt peers to delete yourself in cache
     * </pre>
     *
     * <code>DISCONNECT = 6;</code>
     */
    DISCONNECT(6),
    /**
     * <pre>
     * multi part
     * </pre>
     *
     * <code>MULTI_PART = 7;</code>
     */
    MULTI_PART(7),
    UNRECOGNIZED(-1),
    ;

    /**
     * <pre>
     * empty message
     * </pre>
     *
     * <code>NOTHING = 0;</code>
     */
    public static final int NOTHING_VALUE = 0;
    /**
     * <pre>
     * ping message, open a channel
     * </pre>
     *
     * <code>PING = 1;</code>
     */
    public static final int PING_VALUE = 1;
    /**
     * <pre>
     * pong message, response to a channel opened event
     * </pre>
     *
     * <code>PONG = 2;</code>
     */
    public static final int PONG_VALUE = 2;
    /**
     * <pre>
     * query for peers
     * </pre>
     *
     * <code>LOOK_UP = 3;</code>
     */
    public static final int LOOK_UP_VALUE = 3;
    /**
     * <pre>
     * response to peers query
     * </pre>
     *
     * <code>PEERS = 4;</code>
     */
    public static final int PEERS_VALUE = 4;
    /**
     * <pre>
     * used for application handlers
     * </pre>
     *
     * <code>ANOTHER = 5;</code>
     */
    public static final int ANOTHER_VALUE = 5;
    /**
     * <pre>
     * disconnect message, prompt peers to delete yourself in cache
     * </pre>
     *
     * <code>DISCONNECT = 6;</code>
     */
    public static final int DISCONNECT_VALUE = 6;
    /**
     * <pre>
     * multi part
     * </pre>
     *
     * <code>MULTI_PART = 7;</code>
     */
    public static final int MULTI_PART_VALUE = 7;
    private static final com.google.protobuf.Internal.EnumLiteMap<
        Code> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<Code>() {
            public Code findValueByNumber(int number) {
                return Code.forNumber(number);
            }
        };
    private static final Code[] VALUES = values();
    private final int value;

    Code(int value) {
        this.value = value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static Code valueOf(int value) {
        return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static Code forNumber(int value) {
        switch (value) {
            case 0:
                return NOTHING;
            case 1:
                return PING;
            case 2:
                return PONG;
            case 3:
                return LOOK_UP;
            case 4:
                return PEERS;
            case 5:
                return ANOTHER;
            case 6:
                return DISCONNECT;
            case 7:
                return MULTI_PART;
            default:
                return null;
        }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<Code>
    internalGetValueMap() {
        return internalValueMap;
    }

    public static final com.google.protobuf.Descriptors.EnumDescriptor
    getDescriptor() {
        return org.tdf.sunflower.proto.Sunflower.getDescriptor().getEnumTypes().get(0);
    }

    public static Code valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
            throw new java.lang.IllegalArgumentException(
                "EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
            return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
    }

    public final int getNumber() {
        if (this == UNRECOGNIZED) {
            throw new java.lang.IllegalArgumentException(
                "Can't get the number of an unknown enum value.");
        }
        return value;
    }

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
    getValueDescriptor() {
        return getDescriptor().getValues().get(ordinal());
    }

    public final com.google.protobuf.Descriptors.EnumDescriptor
    getDescriptorForType() {
        return getDescriptor();
    }

    // @@protoc_insertion_point(enum_scope:Code)
}

