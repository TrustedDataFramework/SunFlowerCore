#!/usr/bin/env node -r ts-node/register
import { Interface } from '@ethersproject/abi'
import { compileRust, inline } from './src'
const fs = require('fs')
const file = '/Users/sal/Documents/Github/smart-contract-rs-v2/src/lib.rs'
const wasm = '/Users/sal/Documents/Github/smart-contract-rs-v2/target/wasm32-unknown-unknown/debug/smart_contract_rs_v2.wasm'

let abi = compileRust(fs.readFileSync(file, 'utf8'))
let bin = '0x' + fs.readFileSync(wasm).toString('hex')

let code = inline(bin, abi, ['hello world'])
fs.writeFileSync('bin', Buffer.from(code.substr(2), 'hex'))