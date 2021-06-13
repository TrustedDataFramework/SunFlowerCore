package org.tdf.evm

val EMPTY_RESULT = Result()

data class Result(
    val status: StatusCode? = null,
    val newCodePosition: Int? = null,
    val output: ByteArray? = null,
    val validationStatus: StatusCode? = null,
)

enum class StatusCode(val number: Int) {
    SUCCESS(0),
    FAILURE(1),
    REVERT(2),
    OUT_OF_GAS(3),
    INVALID_INSTRUCTION(4),
    UNDEFINED_INSTRUCTION(5),
    STACK_OVERFLOW(6),
    STACK_UNDERFLOW(7),
    BAD_JUMP_DESTINATION(8),
    INVALID_MEMORY_ACCESS(9),
    CALL_DEPTH_EXCEEDED(10),
    STATIC_MODE_VIOLATION(11),
    PRECOMPILE_FAILURE(12),
    CONTRACT_VALIDATION_FAILURE(13),
    ARGUMENT_OUT_OF_RANGE(14),
    WASM_UNREACHABLE_INSTRUCTION(15),
    WASM_TRAP(16),
    INTERNAL_ERROR(-1),
    REJECTED(-2),
    OUT_OF_MEMORY(-3);
}
