// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sunflower.proto

package org.tdf.sunflower.proto;

public interface MultiPartOrBuilder extends
    // @@protoc_insertion_point(interface_extends:MultiPart)
    com.google.protobuf.MessageOrBuilder {

    /**
     * <code>bytes hash = 1;</code>
     *
     * @return The hash.
     */
    com.google.protobuf.ByteString getHash();

    /**
     * <code>bytes packet = 2;</code>
     *
     * @return The packet.
     */
    com.google.protobuf.ByteString getPacket();

    /**
     * <code>uint32 index = 3;</code>
     *
     * @return The index.
     */
    int getIndex();

    /**
     * <code>uint32 total = 4;</code>
     *
     * @return The total.
     */
    int getTotal();
}
