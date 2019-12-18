package org.tdf.sunflower.exception;

public class ConsensusEngineInitException extends Exception{
    public ConsensusEngineInitException() {
    }

    public ConsensusEngineInitException(String message) {
        super(message);
    }

    public ConsensusEngineInitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsensusEngineInitException(Throwable cause) {
        super(cause);
    }
}
