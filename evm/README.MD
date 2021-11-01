# Ethereum Virtual Machine(EVM)

1. 不支持回退函数的调用, 不支持直接向合约转移 value
2. 不支持 block hash, coinbase 调用
3. 所有 opcode 的 gas 消耗都等于 2 
4. 合约内无法获取当前 gas, gas = 0
