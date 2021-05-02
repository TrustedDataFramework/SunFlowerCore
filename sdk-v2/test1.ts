#!/usr/bin/env node -r ts-node/register
import { providers, Contract} from 'ethers'
import { POW_BIOS_ADDRESS} from './src'
import { POW_BIOS_ABI } from './src/abi'
import path = require('path')

const provider = new providers.JsonRpcProvider('http://localhost:7011')

const biosContract = new Contract(
    POW_BIOS_ADDRESS,
    POW_BIOS_ABI,
    provider
)

biosContract.nbits().then(console.log)
