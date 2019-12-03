package org.tdf.util;

import org.tdf.common.Hashed;

/**
 * The modified Merkle Patricia tree (trie) provides a persistent data structure to map between arbitrary-length binary
 * data (byte arrays). It is defined in terms of a mutable data structure to map between 256-bit binary fragments and
 * arbitrary-length binary data, typically implemented as a database. The core of the trie, and its sole requirement in terms
 * of the protocol specification is to provide a single value that identifies a given set of key-value pairs, which may be either
 * a 32-byte sequence or the empty byte sequence. It is left as an implementation consideration to store and maintain the
 * structure of the trie in a manner that allows effective and efficient realisation of the protocol.
 * @param <T>
 */
public class Trie<T extends Hashed> {

}
