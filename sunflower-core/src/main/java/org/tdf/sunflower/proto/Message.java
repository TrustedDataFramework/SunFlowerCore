// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sunflower.proto

package org.tdf.sunflower.proto;

/**
 * Protobuf type {@code Message}
 */
public final class Message extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:Message)
    MessageOrBuilder {
    public static final int CODE_FIELD_NUMBER = 1;
    public static final int CREATED_AT_FIELD_NUMBER = 2;
    public static final int REMOTE_PEER_FIELD_NUMBER = 3;
    public static final int TTL_FIELD_NUMBER = 4;
    public static final int NONCE_FIELD_NUMBER = 5;
    public static final int SIGNATURE_FIELD_NUMBER = 6;
    public static final int BODY_FIELD_NUMBER = 7;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:Message)
    private static final org.tdf.sunflower.proto.Message DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<Message>
        PARSER = new com.google.protobuf.AbstractParser<Message>() {
        @java.lang.Override
        public Message parsePartialFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
            return new Message(input, extensionRegistry);
        }
    };

    static {
        DEFAULT_INSTANCE = new org.tdf.sunflower.proto.Message();
    }

    private int code_;
    private com.google.protobuf.Timestamp createdAt_;
    private volatile java.lang.Object remotePeer_;
    private long ttl_;
    private long nonce_;
    private com.google.protobuf.ByteString signature_;
    private com.google.protobuf.ByteString body_;
    private byte memoizedIsInitialized = -1;

    // Use Message.newBuilder() to construct.
    private Message(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private Message() {
        code_ = 0;
        remotePeer_ = "";
        signature_ = com.google.protobuf.ByteString.EMPTY;
        body_ = com.google.protobuf.ByteString.EMPTY;
    }

    private Message(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        this();
        if (extensionRegistry == null) {
            throw new java.lang.NullPointerException();
        }
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
            com.google.protobuf.UnknownFieldSet.newBuilder();
        try {
            boolean done = false;
            while (!done) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        done = true;
                        break;
                    case 8: {
                        int rawValue = input.readEnum();

                        code_ = rawValue;
                        break;
                    }
                    case 18: {
                        com.google.protobuf.Timestamp.Builder subBuilder = null;
                        if (createdAt_ != null) {
                            subBuilder = createdAt_.toBuilder();
                        }
                        createdAt_ = input.readMessage(com.google.protobuf.Timestamp.parser(), extensionRegistry);
                        if (subBuilder != null) {
                            subBuilder.mergeFrom(createdAt_);
                            createdAt_ = subBuilder.buildPartial();
                        }

                        break;
                    }
                    case 26: {
                        java.lang.String s = input.readStringRequireUtf8();

                        remotePeer_ = s;
                        break;
                    }
                    case 32: {

                        ttl_ = input.readUInt64();
                        break;
                    }
                    case 40: {

                        nonce_ = input.readUInt64();
                        break;
                    }
                    case 50: {

                        signature_ = input.readBytes();
                        break;
                    }
                    case 58: {

                        body_ = input.readBytes();
                        break;
                    }
                    default: {
                        if (!parseUnknownField(
                            input, unknownFields, extensionRegistry, tag)) {
                            done = true;
                        }
                        break;
                    }
                }
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e.setUnfinishedMessage(this);
        } catch (java.io.IOException e) {
            throw new com.google.protobuf.InvalidProtocolBufferException(
                e).setUnfinishedMessage(this);
        } finally {
            this.unknownFields = unknownFields.build();
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
        return org.tdf.sunflower.proto.Sunflower.internal_static_Message_descriptor;
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(java.io.InputStream input)
        throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
            .parseWithIOException(PARSER, input);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
            .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static org.tdf.sunflower.proto.Message parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
            .parseDelimitedWithIOException(PARSER, input);
    }

    public static org.tdf.sunflower.proto.Message parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
            .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
            .parseWithIOException(PARSER, input);
    }

    public static org.tdf.sunflower.proto.Message parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
            .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(org.tdf.sunflower.proto.Message prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static org.tdf.sunflower.proto.Message getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<Message> parser() {
        return PARSER;
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
        return new Message();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
        return this.unknownFields;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
        return org.tdf.sunflower.proto.Sunflower.internal_static_Message_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                org.tdf.sunflower.proto.Message.class, org.tdf.sunflower.proto.Message.Builder.class);
    }

    /**
     * <pre>
     * protocol header
     * </pre>
     *
     * <code>.Code code = 1;</code>
     *
     * @return The enum numeric value on the wire for code.
     */
    public int getCodeValue() {
        return code_;
    }

    /**
     * <pre>
     * protocol header
     * </pre>
     *
     * <code>.Code code = 1;</code>
     *
     * @return The code.
     */
    public org.tdf.sunflower.proto.Code getCode() {
        @SuppressWarnings("deprecation")
        org.tdf.sunflower.proto.Code result = org.tdf.sunflower.proto.Code.valueOf(code_);
        return result == null ? org.tdf.sunflower.proto.Code.UNRECOGNIZED : result;
    }

    /**
     * <code>.google.protobuf.Timestamp created_at = 2;</code>
     *
     * @return Whether the createdAt field is set.
     */
    public boolean hasCreatedAt() {
        return createdAt_ != null;
    }

    /**
     * <code>.google.protobuf.Timestamp created_at = 2;</code>
     *
     * @return The createdAt.
     */
    public com.google.protobuf.Timestamp getCreatedAt() {
        return createdAt_ == null ? com.google.protobuf.Timestamp.getDefaultInstance() : createdAt_;
    }

    /**
     * <code>.google.protobuf.Timestamp created_at = 2;</code>
     */
    public com.google.protobuf.TimestampOrBuilder getCreatedAtOrBuilder() {
        return getCreatedAt();
    }

    /**
     * <code>string remote_peer = 3;</code>
     *
     * @return The remotePeer.
     */
    public java.lang.String getRemotePeer() {
        java.lang.Object ref = remotePeer_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            remotePeer_ = s;
            return s;
        }
    }

    /**
     * <code>string remote_peer = 3;</code>
     *
     * @return The bytes for remotePeer.
     */
    public com.google.protobuf.ByteString
    getRemotePeerBytes() {
        java.lang.Object ref = remotePeer_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b =
                com.google.protobuf.ByteString.copyFromUtf8(
                    (java.lang.String) ref);
            remotePeer_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    /**
     * <pre>
     * avoid flood attack
     * </pre>
     *
     * <code>uint64 ttl = 4;</code>
     *
     * @return The ttl.
     */
    public long getTtl() {
        return ttl_;
    }

    /**
     * <pre>
     * avoid collision
     * </pre>
     *
     * <code>uint64 nonce = 5;</code>
     *
     * @return The nonce.
     */
    public long getNonce() {
        return nonce_;
    }

    /**
     * <code>bytes signature = 6;</code>
     *
     * @return The signature.
     */
    public com.google.protobuf.ByteString getSignature() {
        return signature_;
    }

    /**
     * <pre>
     * protocol body
     * </pre>
     *
     * <code>bytes body = 7;</code>
     *
     * @return The body.
     */
    public com.google.protobuf.ByteString getBody() {
        return body_;
    }

    @java.lang.Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) return true;
        if (isInitialized == 0) return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
        throws java.io.IOException {
        if (code_ != org.tdf.sunflower.proto.Code.NOTHING.getNumber()) {
            output.writeEnum(1, code_);
        }
        if (createdAt_ != null) {
            output.writeMessage(2, getCreatedAt());
        }
        if (!getRemotePeerBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 3, remotePeer_);
        }
        if (ttl_ != 0L) {
            output.writeUInt64(4, ttl_);
        }
        if (nonce_ != 0L) {
            output.writeUInt64(5, nonce_);
        }
        if (!signature_.isEmpty()) {
            output.writeBytes(6, signature_);
        }
        if (!body_.isEmpty()) {
            output.writeBytes(7, body_);
        }
        unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (code_ != org.tdf.sunflower.proto.Code.NOTHING.getNumber()) {
            size += com.google.protobuf.CodedOutputStream
                .computeEnumSize(1, code_);
        }
        if (createdAt_ != null) {
            size += com.google.protobuf.CodedOutputStream
                .computeMessageSize(2, getCreatedAt());
        }
        if (!getRemotePeerBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, remotePeer_);
        }
        if (ttl_ != 0L) {
            size += com.google.protobuf.CodedOutputStream
                .computeUInt64Size(4, ttl_);
        }
        if (nonce_ != 0L) {
            size += com.google.protobuf.CodedOutputStream
                .computeUInt64Size(5, nonce_);
        }
        if (!signature_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
                .computeBytesSize(6, signature_);
        }
        if (!body_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
                .computeBytesSize(7, body_);
        }
        size += unknownFields.getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof org.tdf.sunflower.proto.Message)) {
            return super.equals(obj);
        }
        org.tdf.sunflower.proto.Message other = (org.tdf.sunflower.proto.Message) obj;

        if (code_ != other.code_) return false;
        if (hasCreatedAt() != other.hasCreatedAt()) return false;
        if (hasCreatedAt()) {
            if (!getCreatedAt()
                .equals(other.getCreatedAt())) return false;
        }
        if (!getRemotePeer()
            .equals(other.getRemotePeer())) return false;
        if (getTtl()
            != other.getTtl()) return false;
        if (getNonce()
            != other.getNonce()) return false;
        if (!getSignature()
            .equals(other.getSignature())) return false;
        if (!getBody()
            .equals(other.getBody())) return false;
        return unknownFields.equals(other.unknownFields);
    }

    @java.lang.Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + CODE_FIELD_NUMBER;
        hash = (53 * hash) + code_;
        if (hasCreatedAt()) {
            hash = (37 * hash) + CREATED_AT_FIELD_NUMBER;
            hash = (53 * hash) + getCreatedAt().hashCode();
        }
        hash = (37 * hash) + REMOTE_PEER_FIELD_NUMBER;
        hash = (53 * hash) + getRemotePeer().hashCode();
        hash = (37 * hash) + TTL_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
            getTtl());
        hash = (37 * hash) + NONCE_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
            getNonce());
        hash = (37 * hash) + SIGNATURE_FIELD_NUMBER;
        hash = (53 * hash) + getSignature().hashCode();
        hash = (37 * hash) + BODY_FIELD_NUMBER;
        hash = (53 * hash) + getBody().hashCode();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    @java.lang.Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    @java.lang.Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE
            ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Message> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public org.tdf.sunflower.proto.Message getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Protobuf type {@code Message}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:Message)
        org.tdf.sunflower.proto.MessageOrBuilder {
        private int code_ = 0;
        private com.google.protobuf.Timestamp createdAt_;
        private com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder> createdAtBuilder_;
        private java.lang.Object remotePeer_ = "";
        private long ttl_;
        private long nonce_;
        private com.google.protobuf.ByteString signature_ = com.google.protobuf.ByteString.EMPTY;
        private com.google.protobuf.ByteString body_ = com.google.protobuf.ByteString.EMPTY;

        // Construct using org.tdf.sunflower.proto.Message.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(
            com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return org.tdf.sunflower.proto.Sunflower.internal_static_Message_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return org.tdf.sunflower.proto.Sunflower.internal_static_Message_fieldAccessorTable
                .ensureFieldAccessorsInitialized(
                    org.tdf.sunflower.proto.Message.class, org.tdf.sunflower.proto.Message.Builder.class);
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
            }
        }

        @java.lang.Override
        public Builder clear() {
            super.clear();
            code_ = 0;

            if (createdAtBuilder_ == null) {
                createdAt_ = null;
            } else {
                createdAt_ = null;
                createdAtBuilder_ = null;
            }
            remotePeer_ = "";

            ttl_ = 0L;

            nonce_ = 0L;

            signature_ = com.google.protobuf.ByteString.EMPTY;

            body_ = com.google.protobuf.ByteString.EMPTY;

            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
            return org.tdf.sunflower.proto.Sunflower.internal_static_Message_descriptor;
        }

        @java.lang.Override
        public org.tdf.sunflower.proto.Message getDefaultInstanceForType() {
            return org.tdf.sunflower.proto.Message.getDefaultInstance();
        }

        @java.lang.Override
        public org.tdf.sunflower.proto.Message build() {
            org.tdf.sunflower.proto.Message result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public org.tdf.sunflower.proto.Message buildPartial() {
            org.tdf.sunflower.proto.Message result = new org.tdf.sunflower.proto.Message(this);
            result.code_ = code_;
            if (createdAtBuilder_ == null) {
                result.createdAt_ = createdAt_;
            } else {
                result.createdAt_ = createdAtBuilder_.build();
            }
            result.remotePeer_ = remotePeer_;
            result.ttl_ = ttl_;
            result.nonce_ = nonce_;
            result.signature_ = signature_;
            result.body_ = body_;
            onBuilt();
            return result;
        }

        @java.lang.Override
        public Builder clone() {
            return super.clone();
        }

        @java.lang.Override
        public Builder setField(
            com.google.protobuf.Descriptors.FieldDescriptor field,
            java.lang.Object value) {
            return super.setField(field, value);
        }

        @java.lang.Override
        public Builder clearField(
            com.google.protobuf.Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        @java.lang.Override
        public Builder clearOneof(
            com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return super.clearOneof(oneof);
        }

        @java.lang.Override
        public Builder setRepeatedField(
            com.google.protobuf.Descriptors.FieldDescriptor field,
            int index, java.lang.Object value) {
            return super.setRepeatedField(field, index, value);
        }

        @java.lang.Override
        public Builder addRepeatedField(
            com.google.protobuf.Descriptors.FieldDescriptor field,
            java.lang.Object value) {
            return super.addRepeatedField(field, value);
        }

        @java.lang.Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof org.tdf.sunflower.proto.Message) {
                return mergeFrom((org.tdf.sunflower.proto.Message) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(org.tdf.sunflower.proto.Message other) {
            if (other == org.tdf.sunflower.proto.Message.getDefaultInstance()) return this;
            if (other.code_ != 0) {
                setCodeValue(other.getCodeValue());
            }
            if (other.hasCreatedAt()) {
                mergeCreatedAt(other.getCreatedAt());
            }
            if (!other.getRemotePeer().isEmpty()) {
                remotePeer_ = other.remotePeer_;
                onChanged();
            }
            if (other.getTtl() != 0L) {
                setTtl(other.getTtl());
            }
            if (other.getNonce() != 0L) {
                setNonce(other.getNonce());
            }
            if (other.getSignature() != com.google.protobuf.ByteString.EMPTY) {
                setSignature(other.getSignature());
            }
            if (other.getBody() != com.google.protobuf.ByteString.EMPTY) {
                setBody(other.getBody());
            }
            this.mergeUnknownFields(other.unknownFields);
            onChanged();
            return this;
        }

        @java.lang.Override
        public final boolean isInitialized() {
            return true;
        }

        @java.lang.Override
        public Builder mergeFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
            org.tdf.sunflower.proto.Message parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (org.tdf.sunflower.proto.Message) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        /**
         * <pre>
         * protocol header
         * </pre>
         *
         * <code>.Code code = 1;</code>
         *
         * @return The enum numeric value on the wire for code.
         */
        public int getCodeValue() {
            return code_;
        }

        /**
         * <pre>
         * protocol header
         * </pre>
         *
         * <code>.Code code = 1;</code>
         *
         * @param value The enum numeric value on the wire for code to set.
         * @return This builder for chaining.
         */
        public Builder setCodeValue(int value) {
            code_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * protocol header
         * </pre>
         *
         * <code>.Code code = 1;</code>
         *
         * @return The code.
         */
        public org.tdf.sunflower.proto.Code getCode() {
            @SuppressWarnings("deprecation")
            org.tdf.sunflower.proto.Code result = org.tdf.sunflower.proto.Code.valueOf(code_);
            return result == null ? org.tdf.sunflower.proto.Code.UNRECOGNIZED : result;
        }

        /**
         * <pre>
         * protocol header
         * </pre>
         *
         * <code>.Code code = 1;</code>
         *
         * @param value The code to set.
         * @return This builder for chaining.
         */
        public Builder setCode(org.tdf.sunflower.proto.Code value) {
            if (value == null) {
                throw new NullPointerException();
            }

            code_ = value.getNumber();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * protocol header
         * </pre>
         *
         * <code>.Code code = 1;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearCode() {

            code_ = 0;
            onChanged();
            return this;
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         *
         * @return Whether the createdAt field is set.
         */
        public boolean hasCreatedAt() {
            return createdAtBuilder_ != null || createdAt_ != null;
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         *
         * @return The createdAt.
         */
        public com.google.protobuf.Timestamp getCreatedAt() {
            if (createdAtBuilder_ == null) {
                return createdAt_ == null ? com.google.protobuf.Timestamp.getDefaultInstance() : createdAt_;
            } else {
                return createdAtBuilder_.getMessage();
            }
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        public Builder setCreatedAt(com.google.protobuf.Timestamp value) {
            if (createdAtBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                createdAt_ = value;
                onChanged();
            } else {
                createdAtBuilder_.setMessage(value);
            }

            return this;
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        public Builder setCreatedAt(
            com.google.protobuf.Timestamp.Builder builderForValue) {
            if (createdAtBuilder_ == null) {
                createdAt_ = builderForValue.build();
                onChanged();
            } else {
                createdAtBuilder_.setMessage(builderForValue.build());
            }

            return this;
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        public Builder mergeCreatedAt(com.google.protobuf.Timestamp value) {
            if (createdAtBuilder_ == null) {
                if (createdAt_ != null) {
                    createdAt_ =
                        com.google.protobuf.Timestamp.newBuilder(createdAt_).mergeFrom(value).buildPartial();
                } else {
                    createdAt_ = value;
                }
                onChanged();
            } else {
                createdAtBuilder_.mergeFrom(value);
            }

            return this;
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        public Builder clearCreatedAt() {
            if (createdAtBuilder_ == null) {
                createdAt_ = null;
                onChanged();
            } else {
                createdAt_ = null;
                createdAtBuilder_ = null;
            }

            return this;
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        public com.google.protobuf.Timestamp.Builder getCreatedAtBuilder() {

            onChanged();
            return getCreatedAtFieldBuilder().getBuilder();
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        public com.google.protobuf.TimestampOrBuilder getCreatedAtOrBuilder() {
            if (createdAtBuilder_ != null) {
                return createdAtBuilder_.getMessageOrBuilder();
            } else {
                return createdAt_ == null ?
                    com.google.protobuf.Timestamp.getDefaultInstance() : createdAt_;
            }
        }

        /**
         * <code>.google.protobuf.Timestamp created_at = 2;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder>
        getCreatedAtFieldBuilder() {
            if (createdAtBuilder_ == null) {
                createdAtBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
                    com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder>(
                    getCreatedAt(),
                    getParentForChildren(),
                    isClean());
                createdAt_ = null;
            }
            return createdAtBuilder_;
        }

        /**
         * <code>string remote_peer = 3;</code>
         *
         * @return The remotePeer.
         */
        public java.lang.String getRemotePeer() {
            java.lang.Object ref = remotePeer_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                remotePeer_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <code>string remote_peer = 3;</code>
         *
         * @param value The remotePeer to set.
         * @return This builder for chaining.
         */
        public Builder setRemotePeer(
            java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }

            remotePeer_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string remote_peer = 3;</code>
         *
         * @return The bytes for remotePeer.
         */
        public com.google.protobuf.ByteString
        getRemotePeerBytes() {
            java.lang.Object ref = remotePeer_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                        (java.lang.String) ref);
                remotePeer_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string remote_peer = 3;</code>
         *
         * @param value The bytes for remotePeer to set.
         * @return This builder for chaining.
         */
        public Builder setRemotePeerBytes(
            com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);

            remotePeer_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>string remote_peer = 3;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearRemotePeer() {

            remotePeer_ = getDefaultInstance().getRemotePeer();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * avoid flood attack
         * </pre>
         *
         * <code>uint64 ttl = 4;</code>
         *
         * @return The ttl.
         */
        public long getTtl() {
            return ttl_;
        }

        /**
         * <pre>
         * avoid flood attack
         * </pre>
         *
         * <code>uint64 ttl = 4;</code>
         *
         * @param value The ttl to set.
         * @return This builder for chaining.
         */
        public Builder setTtl(long value) {

            ttl_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * avoid flood attack
         * </pre>
         *
         * <code>uint64 ttl = 4;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearTtl() {

            ttl_ = 0L;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * avoid collision
         * </pre>
         *
         * <code>uint64 nonce = 5;</code>
         *
         * @return The nonce.
         */
        public long getNonce() {
            return nonce_;
        }

        /**
         * <pre>
         * avoid collision
         * </pre>
         *
         * <code>uint64 nonce = 5;</code>
         *
         * @param value The nonce to set.
         * @return This builder for chaining.
         */
        public Builder setNonce(long value) {

            nonce_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * avoid collision
         * </pre>
         *
         * <code>uint64 nonce = 5;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearNonce() {

            nonce_ = 0L;
            onChanged();
            return this;
        }

        /**
         * <code>bytes signature = 6;</code>
         *
         * @return The signature.
         */
        public com.google.protobuf.ByteString getSignature() {
            return signature_;
        }

        /**
         * <code>bytes signature = 6;</code>
         *
         * @param value The signature to set.
         * @return This builder for chaining.
         */
        public Builder setSignature(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }

            signature_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>bytes signature = 6;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearSignature() {

            signature_ = getDefaultInstance().getSignature();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * protocol body
         * </pre>
         *
         * <code>bytes body = 7;</code>
         *
         * @return The body.
         */
        public com.google.protobuf.ByteString getBody() {
            return body_;
        }

        /**
         * <pre>
         * protocol body
         * </pre>
         *
         * <code>bytes body = 7;</code>
         *
         * @param value The body to set.
         * @return This builder for chaining.
         */
        public Builder setBody(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }

            body_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * protocol body
         * </pre>
         *
         * <code>bytes body = 7;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearBody() {

            body_ = getDefaultInstance().getBody();
            onChanged();
            return this;
        }

        @java.lang.Override
        public final Builder setUnknownFields(
            final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @java.lang.Override
        public final Builder mergeUnknownFields(
            final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }


        // @@protoc_insertion_point(builder_scope:Message)
    }

}

