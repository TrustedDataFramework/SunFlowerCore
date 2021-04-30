#!/usr/bin/env node -r ts-node/register
import { utils, UnsignedTransaction } from 'ethers'

let unsigned: UnsignedTransaction = {
    to: '0x4Ad950B0B049ac9bA9A1982758cb44C2A1e9C52D',
    nonce: 0,
    gasLimit: 0,
    gasPrice: 0,
    data: '0x',
    value: 0,
    chainId: 56,
}


console.log(utils.getContractAddress({ from: '0x4Ad950B0B049ac9bA9A1982758cb44C2A1e9C52D', nonce: 0}))
