import {Context, log} from "../assembly";
import {Parameters} from "../assembly/payload";

let sender: Uint8Array;
let balances: Map<Uint8Array, u64>;


export function init(): void {
    balances = new Map<Uint8Array, u64>();
    let c = Context.load();
    sender = c.sender;
    balances.set(sender, 100_0000);
    log("contract deployed successfully");
}

export function transfer(): void {
    let balance = balances.get(sender);
    const params = Parameters.load();
    let len_to:i32 = params.i8();
    let recipient: Uint8Array = params.bytes(len_to);
    let money = params.u64();
    if (balance < money) {
        log("balance is not enough");
        return
    }
    balance -= money;
    balances.set(sender, balance);
    if (balances.has(recipient)) {
        balances.set(recipient, balances.get(recipient) + money)
    } else {
        balances.set(recipient, money)
    }
    log("transfer succeed, balance is " + balance.toString())
}

export function getBalance(): u64 {
    return balances.has(sender) ? balances.get(sender) : 0;
}
