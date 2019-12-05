import {Context, log} from "../assembly";
import {Hash} from "../assembly/hash";
import {Parameters} from "../assembly/payload";

let dnaDigits: u32 = 16;
let dnaModulus: f64;

class Zombie {
    name: string;
    dna: u32;

    constructor(name: string, dna: u32) {
        this.name = name;
        this.dna = dna;
    }
}

let zombies: Zombie[];

let zombieToOwner: Map<u32, Uint8Array>;

let ownerZombieCount: Map<Uint8Array, u32>;

export function init(): void {
    dnaModulus = 10 ** dnaDigits;
    zombieToOwner = new Map<u32, Uint8Array>();
    ownerZombieCount = new Map<Uint8Array, u32>();
    zombies = [];
}

function _createZombie(_name: string, _dna: u32): void {
    let id = zombies.push(new Zombie(_name, _dna));
    let sender = Context.load().sender;
    zombieToOwner.set(id, sender);
    let count = ownerZombieCount.has(sender) ? ownerZombieCount.get(sender) : 0;
    ownerZombieCount.set(sender, ++count);
    log("create zombie name is " + _name);
    log("create zombie _dna is " + _dna.toString());
    log("create zombie id is " + id.toString());
}

function _generateRandomDna(name: string): u32 {
    let buffer: ArrayBuffer = String.UTF8.encode(name);
    let _name: Uint8Array = Uint8Array.wrap(buffer, 0, buffer.byteLength);
    let rand: Uint8Array = Hash.keccak256(_name);
    let _rand: u32 = u32(String.UTF8.decode(rand.buffer));
    return u32(_rand % dnaModulus);
}

export function createRandomZombie(): void {
    let _name = Parameters.load().string();
    let randDna = _generateRandomDna(_name);
    randDna = randDna - randDna % 100;
    _createZombie(_name, randDna);
}

export function feedOnKitty(): void {
    let param = Parameters.load();
    let _zombieId = param.u32();
    let _kittyId = param.u32();
    let name = param.string();
    if (!zombieToOwner.has(_kittyId)) {
        log("invalid kittyId");
        return;
    }
    let kittyDna = zombies[_kittyId - 1].dna;
    feedAndMultiply(_zombieId, kittyDna, name);
}

function feedAndMultiply(_zombieId: u32, _targetDna: u32, name: string): void {
    let myZombie = zombies[_zombieId];
    _targetDna = u32(_targetDna % dnaModulus);
    let newDna = (myZombie.dna + _targetDna) / 2;
    newDna = newDna - newDna % 100 + 99;
    _createZombie(name, newDna);
}
