#!/usr/bin/env node -r ts-node/register
import { providers, Contract, Wallet, BigNumber} from 'ethers'
import { FARMBASE_GATEWAY_ADDRESS } from './src'
import { AUTHENTICATION_ABI } from './src/abi'
import path = require('path')

const provider = new providers.JsonRpcProvider('http://localhost:7010')
const wallet = new Wallet(process.env['PRIVATE_KEY'], provider)

const auth = new Contract(
    FARMBASE_GATEWAY_ADDRESS,
    AUTHENTICATION_ABI,
    wallet
)

async function logAddress() {
    console.log(wallet.address)
}

async function logApproved() {
    console.log(await auth.approved())
}

async function approve() {
    console.log(await auth.approve('0x8bB3194c582A9F70bDC079e4c20CDe4A7fc3c807'))
}

async function transfer() {
    console.log('balance = ' + await wallet.getBalance())
    await wallet.sendTransaction({ to: '0x4Ad950B0B049ac9bA9A1982758cb44C2A1e9C52D', value: BigNumber.from('1000000') })
}

transfer()