package org.tdf.sunflower;

public class ApplicationConstants {
    public static final String CONSENSUS_NAME_PROPERTY = "sunflower.consensus.name";

    public static final String CONSENSUS_POA = "poa";

    public static final String CONSENSUS_NONE = "none";

    public static final String CONSENSUS_POW = "pow";

    public static final String CONSENSUS_POS = "pos";
    public static final String CONSENSUS_VRF = "vrf";
    public static final int SHUTDOWN_SIGNAL = 0;
    public static final long MAX_SHUTDOWN_WAITING = 5;
    // cache read-only trie for quick search
    public static int TRIE_CACHE_SIZE = 32;
    // cache transactions to avoid message flood
    public static int P2P_TRANSACTION_CACHE_SIZE = 128;
    // cache proposals to avoid message flood
    public static int P2P_PROPOSAL_CACHE_SIZE = 128;
    public static long VM_STEP_LIMIT = 0;

    public static long VM_GAS_PRICE = 0;

    public static long MAX_FRAMES = 0;

    public static long MAX_CONTRACT_CALL_DEPTH = 0;

    public static boolean VALIDATE;

    public static long TRANSACTION_CONFIRMS = 0;
}
