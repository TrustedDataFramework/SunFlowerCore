#!/usr/bin/env node -r ts-node/register
import { utils } from 'ethers'
import { UnsignedTransaction, providers, Wallet, Contract} from 'ethers'
import { compileRust, inline } from './src'
const fs = require('fs')
const file = '/Users/sal/Documents/Github/smart-contract-rs-v2/src/lib.rs'
const wasm = '/Users/sal/Documents/Github/smart-contract-rs-v2/target/wasm32-unknown-unknown/debug/smart_contract_rs_v2.wasm'

let abi = compileRust(fs.readFileSync(file, 'utf8'))
let bin = '0x' + fs.readFileSync(wasm).toString('hex')

let code = inline(bin, abi, ['hello world'])


const provider = new providers.JsonRpcProvider('http://localhost:7010')
const wallet = new Wallet(process.env.PRIVATE_KEY, provider)

async function deploy() {
    
    let unsigned: UnsignedTransaction = {
        nonce: 0,
        gasLimit: 0,
        gasPrice: 0,
        data: code,
        value: 0,
    }

    unsigned.nonce = await wallet.getTransactionCount()
    console.log(unsigned.nonce)
    unsigned.gasLimit = await wallet.estimateGas(unsigned)
    console.log(unsigned.gasLimit.toString())
    let resp = await wallet.sendTransaction(unsigned)
    let r = await resp.wait()

    const c = new Contract(r.contractAddress, abi, wallet)
    // pub fn some(a: String, b: Address, c: U256, d: u64, e: Vec<u8>) -> &'static String{ // @payable
    let r1 = await c.some('string', '0xdFf6Eb3A1965789f9D1aB95243aC9B9cEB1Ad1D3', utils.parseEther('1'), '1', '0xff')
    let r2 = await c.empty()
    console.log(r2)
}

deploy()
