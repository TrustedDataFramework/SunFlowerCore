#!/usr/bin/env node -r ts-node/register
import { providers, Contract} from 'ethers'
import fs = require('fs')
import path = require('path')

const biosAddress = '0x0000000000000000000000000000000000000002'
const provider = new providers.JsonRpcProvider('http://localhost:7011')

const biosContract = new Contract(
    biosAddress, 
    fs.readFileSync(path.join(__dirname, 'bios.abi.json'), 'utf-8'),
    provider
)

biosContract.nbits().then(console.log)
