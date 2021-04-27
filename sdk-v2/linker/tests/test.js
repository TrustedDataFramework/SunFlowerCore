const { link } = require('../pkg/linker')
const fs = require('fs')
const wasm = fs.readFileSync('target/wasm32-unknown-unknown/release/linker.wasm')

const linked = link(wasm.toString('hex'), `[
    {
        "name": "init",
        "type": "function",
        "inputs": [
            {
                "type": "bytes",
                "name": "chainId"
            }
        ],
        "outputs": [
            {
                "type": "address"
            }
        ],
        "stateMutability": "payable"
    },
    {
        "type": "constructor",
        "inputs": [
            {
                "type": "bytes",
                "name": "chainId"
            }
        ],
        "outputs": [
            {
                "type": "address"
            }
        ],
        "stateMutability": "payable"
    }    
]`.replace(/[\n\t\s]/g, ''))

fs.writeFileSync('bin', Buffer.from(linked, 'hex'))