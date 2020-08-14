# TDS 

[TOC]

## 参数配置

TDS 采用 yml 文件作为参数配置文件，可以在启动时用环境变量SPRING_CONFIG_LOCATION 指定好自定义的配置文件。
例如:

``` sh
SPRING_CONFIG_LOCATION=classpath:application.yml,$HOME/Documents/local.yml java -jar sunflower*.jar
```

文件路径之间以逗号分割，后面的配置会覆盖前面的配置。
除了环境变量配置，也可以用命令行参数指定配置文件，例如

``` sh
java -jar sunflower*.jar --spring.config.location=classpath:application.yml
```

配置文件中的参数都可以用相应的命令函参数覆盖，
例如配置文件中的配置项 spring.datasource.url 可以在启动时用命令行参数覆盖。

``` sh
java -jar app.jar --spring.datasource.url="jdbc:h2:mem:test"
```

### sunflower 配置

``` yml
sunflower:
    assert: 'false' # 是否开启断言，默认是 false
    validate: 'false' # 启动时是否校验本地数据，如果设置为true，启动时会校验账本数据
    libs: 'local/jar' # jar包路径，用于加载外部的共识插件
    secret-store: "" # http 认证的路径，若填写，节点启动后会打印出一个公钥并且进入阻塞，认证成功后，节点会成功加载路径
```

### 共识参数 (PoA)

``` yml
sunflower:
  consensus:
    name: 'poa' # 选择 poa 作为共识机制
    genesis: 'genesis/poa.jsonc' # 创世区块的 url，搜索优先级是 网路 > 文件系统 > classpath 
    block-interval: '1' # 出块间隔，最小值是一秒
    enable-mining: 'true' # 是否开启挖矿
    private-key: 'f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5' # 节点的私钥明文，建议使用证书的方式加载
    allow-empty-block: 'false' # 是否允许空块
    max-body-size: '2048' # 区块的最大事务数量限制
```

### PoA的创世区块文件

``` jsonc
{
  "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000", // 父区块的哈希值

  "timestamp": 1572511433, // 区块时间戳

  // 被允许出块的账户地址
  "miners": [{
      "addr": "9cbf30db111483e4b84e77ca0e39378fd7605e1b"
    },
    {
      "addr": "9cbf30db111483e4b84e77ca0e39378fd7605e1b"
    }

  ],

  // 预分配的账户余额
  "alloc":{
        "9cbf30db111483e4b84e77ca0e39378fd7605e1b": 1000000,
        "bf0aba026e5a0e1a69094c8a0d19d905367d64cf": 1000000
  }
}
```

### 共识参数 (PoW)

``` yml
sunflower:
  consensus:
    name: 'pow' # 选择 pow 作为共识机制
    genesis: 'genesis/pow.jsonc' # 创世区块的 url，搜索优先级是 网路 > 文件系统 > classpath 
    block-interval: '30' # 出块间隔，最小值是一秒，
    enable-mining: 'true' # 是否开启挖矿
    blocks-per-era: '100' # 每隔多少个区块调整一次难度值
    allow-empty-block: 'true' # 是否出空块
    max-body-size: '2048' # 区块的最大事务数量限制
    miner-coin-base: '9cbf30db111483e4b84e77ca0e39378fd7605e1b' # 矿工收益地址
```

### PoW的创世区块文件

``` jsonc
{
  "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000", // 父区块的哈希值

  "timestamp": 1572511433, // 区块时间戳

  "nbits": "0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", // 初始难度值

  // 预分配的账户余额
  "alloc":{
        "9cbf30db111483e4b84e77ca0e39378fd7605e1b": 1000000,
        "bf0aba026e5a0e1a69094c8a0d19d905367d64cf": 1000000
  }
}

```

### 共识参数 (PoS)

``` yml
sunflower:
  consensus:
    name: 'pos' # 选择 pos 作为共识机制
    genesis: 'genesis/pos.jsonc' # 创世区块的 url，搜索优先级是 网路 > 文件系统 > classpath 
    block-interval: '5' # 出块间隔，最小值是一秒
    enable-mining: 'true' # 是否开启挖矿
    allow-empty-block: 'true' # 是否出空块
    max-body-size: '2048' # 区块的最大事务数量限制
    max-miners: '10' #最大矿工数量
    miner-coin-base: '9cbf30db111483e4b84e77ca0e39378fd7605e1b' # 矿工收益地址
```

### PoS的创世区块文件

``` jsonc
{
  "parentHash": "0x0000000000000000000000000000000000000000000000000000000000000000", // 父区块的哈希值

  "timestamp": 1572511433, // 区块时间戳
  "miners": [{
      // 矿工 = 地址 + 投票数量
      "addr": "9cbf30db111483e4b84e77ca0e39378fd7605e1b",
      "vote": 100000000
    }
  ],

// 预分配的账户余额
  "alloc":{
        "9cbf30db111483e4b84e77ca0e39378fd7605e1b": 1000000,
        "bf0aba026e5a0e1a69094c8a0d19d905367d64cf": 1000000
  }
}

```

### P2P 参数配置

``` yml
sunflower:
  p2p:
    name: 'websocket' # P2P消息传输采用的协议，默认是websocket, gRPC 是可选项
    max-peers: '16' # 邻居节点的最大数量限制，默认是16个
    
    # 节点的地址，端口可以不填，默认监听端口是30569
    # 如果主机名填localhost或者127.0.0.1，程序首先会尝试获取节点的公网ip，若公网ip加上端口号可以ping通，程序会把公网ip+端口作为对外地址。
    # 如果公网ip获取失败或者公网ip+端口无法ping通，程序会获取本机在局域网中的ip，将局域网的ip作为自己的对外地址
    address: 'node://localhost:7000' 

    # 是否开启节点发现，若不开启节点发现，邻居节点固定为种子节点和信任节点
    # 若开启节点发现，邻居节点会动态变化，但信任节点不会被主动断开。    
    enable-discovery: 'true' 
    
    # 种子节点
    bootstraps:

      - 'node://192.168.1.117:9999'

    # 信任节点的地址
    trusted:

      - 'node://192.168.1.117:9999'

    # 白名单配置，可以填写其他节点的公钥，如果白名单中填写了至少一个公钥，黑名单将无效，只有白名单中的节点才可以被连接
    white-list:

    	- '02b507fe1afd0cc7a525488292beadbe9f143784de44f8bc1c991636509fd50936'

    # 黑名单配置，可以填写其他节点的公钥，如果某个节点的公钥在黑名单中，它的消息将不会被接收
    blocked-list:

    	- '02b507fe1afd0cc7a525488292beadbe9f143784de44f8bc1c991636509fd50936'

    # 用明文设置节点p2p的私钥，建议用证书文件加载私钥
    private-key: 'f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5'

    # 是否持久化邻居节点信息，默认是false 
    # 如果设置为 true 且 database.type 不是 memory 节点信息会被持久化到文件 database.directory/peers.json
    persist: false

    # 节点发现检查频率，默认是15秒一次
    discover-rate: 15

    # p2p 最大包大小，协议会对超过这个大小的包进行分包传输 详见 p2p 章节
    max-packet-size: 2097152

    # 每隔 300 秒对收到的分包数据进行清理，详见 p2p 章节
    cache-expired-after: 300

```

### 同步参数配置

``` yml
sunflower:
  sync:
    # 定时发送 Status 消息的间隔
    heart-rate: 5 

    # 区块写入队列的检查频率，默认1秒钟检查一次
    block-write-rate: 1

    # 待写入区块队列的最大容量，默认是2048
    max-pending-blocks: 2048

    # 同步过程中单个消息中区块最大传输数量，默认是1024
    max-blocks-transfer: 1024

    # 区块压缩的目标区块，此区块之前的过时数据会被清除
    prune-hash: ''

    # 快速同步的目标区块哈希
    fast-sync-hash: ''
 
    # 快速同步的目标区块高度
    fast-sync-height: '0'

    # 快速同步过程中，单个消息包含的账户最大数量
    max-accounts-transfer: 512

    # 互斥锁的超时时间，一般不用修改
    lock-timeout: 1

    # 消息限流相关参数，例如设置 status: 16 则限制接收到status消息的频率限制在16次每秒
    rate-limits:
      status: '16'
      get-blocks: '16'
```

## rlp 编码

递归前缀编码(RLP) https://github.com/ethereum/wiki/wiki/RLP
一种二进制序列化规范，优点是紧凑，缺点是最大只支持对4G以下的内容进行编解码。

java 可以采用注解的方式进行 rlp 编码和解码：
https://github.com/TrustedDataFramework/java-rlp

javascript 或 nodejs 中使用 rlp 编码可参考 https://github.com/ethereumjs/rlp

## 区块头

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| version | int | 区块版本号 PoA=1634693120, PoW=7368567, PoS=7368563 |
| hashPrev    |   bytes   | 父区块的哈希值 |
|    transactionsRoot  |   bytes   | 事务的梅克尔根 |
| stateRoot | bytes | 状态树树根 |
| height | long | 区块高度 |
| createdAt | long | 区块的构造时间，用 unix epoch 秒数表示 |
| payload | bytes | 载荷，根据不同的共识具有不同的含义|
| hash | bytes | 区块的哈希值 |

### payload 的具体含义

1. 对于 PoA 共识，节点在构造区块完成后会对区块整体做一个签名，然后把签名放到区块头的 payload 字段中
2. PoW 共识的区块头 payload 相当于比特币区块头的 nonce，用于工作量证明
3. PoS 的区块头 payload 字段为空

## 事务

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| version | int | 事务版本号 PoA=1634693120, PoW=7368567, PoS=7368563 |
| type    |   int   | 0=COINBASE, 1=转账, 2=合约部署, 3=合约调用|
|  createdAt  |   long   | 事务的构造时间，用 unix epoch 秒数表示 |
| nonce | long | 事务的序号，coinbase事务的 nonce 等于区块高度 |
| from| bytes | 事务发送者的公钥 |
| gasPrice | long | 事务的手续费价格 |
| amount | long | 转账、后者在合约调用时的转账金额|
| payload | bytes | 载荷 |
| to | bytes |  转账的接收者或者被调用的合约|
| signature | bytes | 签名 |
| hash | bytes | 事务哈希 |

### amount 的不同含义

1. 对于 coinbase 事务，amount 是经济奖励的数量
2. 对于转账事务，amount 是转账的数量
3. 对于合约部署的事务，amount 必须为 0 
4. 对于合约调用的事务，amount 的金额会被转到合约的创建者的账户下

### payload 的不同含义

1. coinbase 事务和 转账事务的 payload 一般为空，特别的是 PoA 共识的 payload 填写的是出块者的公钥
2. 对于合约部署事务，payload 是智能合约 wasm 字节码
3. 对于合约调用事务，payload 是调用智能合约的二进制参数，构造方法是把智能合约方法名长度放在第一个字节，后面跟方法名的 acii 编码，剩余的就是针对具体调用的方法的参数

## 账户

普通账户与合约账户都用以下结构表示

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| address | bytes | 账户地址，二十个字节 |
| nonce |   long   | 递增序号 |
|  balance  |   long   | 账户余额，对于合约账户此字段永远为0 |
| createdBy | bytes | 合约的创建者，普通账户此字段为空 |
| contractHash | bytes | 合约代码的哈希值，普通账户此字段为空 |
| storageRoot | bytes | 合约存储树树根，普通账户此字段为空 |

### 普通账户

普通账户的地址由公钥计算得到。
普通账号可以用椭圆曲线密钥对表示，包含公钥和私钥，默认采用的椭圆曲线是sm2。

普通账户的地址生成是对公钥作一次哈希值计算后，取公钥的后面20个字节，伪代码如下：

``` python
h = sm3(publicKey)
address = h[len(h) - 20:]
```

nonce 用于防止重放攻击，当用普通账户构造事务时，需要保证事务的 nonce 等于当前账户的nonce 加一，用伪代码可以这样表示：

``` python
nonce = getNonce(publicKey)
transaction['type'] = 1 # 这里用的是转账事务，也可以 2或者3
transaction['from'] = publicKey
transaction['nonce'] = nonce + 1
```

balance 是账户的余额

### 合约账户

合约账户是普通账户部署合约时生成的，合约地址的生成用伪代码表示:

``` python
b = sm3(rlp.encode([publicKey, nonce]))
address = h[len(b) - 20:]
```

合约地址是把 合约部署者的公钥和部署合约事务的 nonce 作了 rlp 编码后，计算哈希值，然后取最后 20 个字节

合约账户没有对应的私钥和公钥，合约账户的 nonce 值等于部署合约事务的 nonce，而且这个nonce不会再改变

每个合约账户都有自己独立的存储空间，这个存储空间实际上是一个梅克尔-帕特里夏树，合约账户的状态可以用梅克尔-帕特里夏树的树根表示，也就是 storageRoot 字段

## 共识机制

### PoA

权威证明共识（又称PoA共识）规定，节点只有被授权以后才能参与区块链共识，也只有被授权的节点能加入 p2p 网络

poa 共识对应的创世区块中，miners 包含了所有被授权节点的公钥，只有拥有对应私钥的节点才可以加入 p2p 网络并参与共识

在 PoA 共识中有两个特殊的内置合约地址，
一个是 ```0000000000000000000000000000000000000003``` 和 ```0000000000000000000000000000000000000004```。这两个内置合约分别保存了 p2p 许可的节点和被授权参与共识的节点。其他节点如果想加入 p2p 网络，需要构造请求事务发到相应的内置合约上面。

以加入 p2p 网络为例，流程如下：

1. 节点 B 试图加入 p2p 网络，于是构造了如下事务发到了已经受到许可的节点A上

``` jsonc
{
    "version": 1634693120,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "节点B的公钥",
    "gasPrice": 0,
    "amount": 0,
    "payload": "00", 
    "to": "0000000000000000000000000000000000000003",
    "signature": "****",
    "hash": "**",
}
```

2. 节点A通过查看合约状态，收到了节点B的请求，并且同意了节点B的加入，构造了以下事务发到链上

``` jsonc
{
    "version": 1634693120,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "节点A的公钥",
    "gasPrice": 0,
    "amount": 0,
    "payload": "01拼接上节点A的地址" , 
    "to": "0000000000000000000000000000000000000003",
    "signature": "****",
    "hash": "**",
}
```

3. 节点A通过了节点B的请求后，因为原先只有节点A处在授权节点中，所以满足了超过2/3同意的条件，节点B加入了 p2p 许可节点。若节点C再要加入 p2p 网络，则必须节点A和节点B都同意才可以，因为此时若只有节点A同意，没有满足 2/3 以上的同意。

4. 节点 B 如果想退出共识，可以构造以下事务退出

``` jsonc
{
    "version": 1634693120,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "节点A的公钥",
    "gasPrice": 0,
    "amount": 0,
    "payload": "02" , 
    "to": "0000000000000000000000000000000000000003",
    "signature": "****",
    "hash": "**",
}
```

5. 私钥加载

由于 poa 需要在节点中填写私钥，我们建议通过 http 认证的方式加载私钥，而不是将私钥以明文的形式保存在节点上。

例如我现在想把私钥 ```f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5```，传递给节点，我可以先在节点上配置一个 http 认证路径

```yml
sunflower:
  secret-store: http://localhost:3000
```

等待节点启动后，节点会打印出如下的日志

please generate secret store for your private key, public key = 035db35c97cfa7b691e3d171b5e93bc3660c47f5912122aed008d2a99b457830af，这个 public key 是用来进行非对称加密的，我们可以把敏感信息用这个公钥加密后发给节点，节点再解密，实现安全可信的通信。

接下来节点会尝试每秒钟发送一次 get 请求到 

```http://localhost:3000?publicKey=035db35c97cfa7b691e3d171b5e93bc3660c47f5912122aed008d2a99b457830af```

我们需要启动一个服务，并对这个请求作出正确的响应，响应的格式为

```json
{
  "publicKey": "",
  "cipherText": ""
}
```

其中 cipherText 是用公钥 
```035db35c97cfa7b691e3d171b5e93bc3660c47f5912122aed008d2a99b457830af```
加密后的数据，而我们要加密的内容则是长度为 32 字节的数组。

下面以 nodejs 为例，展示如何对节点的认证请求作出正确响应，当节点成功加载私钥后，认证服务应当停止。注意要引入依赖 @salaku/sm-crypto 和 @salaku/js-sdk

```js
const tool = require('@salaku/js-sdk')
const sm2 = require('@salaku/sm-crypto').sm2

const sk = 'f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5'
const URL  = require('url');

var http = require('http')

var server = http.createServer()

server.on('request', function (request, response) {
query)
    let otherPublicKey = URL.parse(request.url,true).query['publicKey']
    if(otherPublicKey !== '035db35c97cfa7b691e3d171b5e93bc3660c47f5912122aed008d2a99b457830af')
      response.end() // 如果不是正确的公钥，拒绝认证请求

    // 对公钥进行解压缩  
    if(otherPublicKey.substr(0, 2) !== '04'){
        otherPublicKey = sm2.deCompress(otherPublicKey)
    }

    // 生成密文
    const ret = {
        publicKey: tool.privateKey2PublicKey(sk),
        cipherText: sm2.doEncrypt(Buffer.from(sk, 'hex'), otherPublicKey, sm2.C1C2C3)
    }
    // response 对象有一个方法：write 可以用来给客户端发送响应数据
    // write 可以使用多次，但是最后一定要使用 end 来结束响应，否则客户端会一直等待
    response.write(JSON.stringify(ret))
    response.end()
    // 认证成功，拒绝后续的认证请求
})

server.listen(3000, function () {
    console.log('服务器启动成功了，可以通过 http://127.0.0.1:3000/ 来进行访问')
})

```

### PoS

PoS 即 Proof of Stake。在 PoS 共识中，股权较大的节点拥有区块打包权。这里的 PoS 使用内置合约保存每个账号收到的投票数量，地址是 ```0000000000000000000000000000000000000005```。

在这个内置合约中每个账号被按照投票数量排序，假设 `sunflower.consensus.max-miners=10` ，则投票排名前10的账户会获得区块打包权。

与 PoS 相关的事务有投票事务和取消投票的事务

1. 投票事务

假设节点A给节点B投 1000 票，则节点A需要构造如下事务

``` jsonc
{
    "version": 7368563,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "节点A的公钥",
    "gasPrice": 0,
    "amount": 1000,
    "payload": "00拼接上节点B的地址" , 
    "to": "0000000000000000000000000000000000000005",
    "signature": "****",
    "hash": "**",
}
```

2. 撤回投票事务

假设节点A要撤回给节点B的投票，需要构造如下事务

``` jsonc
{
    "version": 7368563,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "节点A的公钥",
    "gasPrice": 0,
    "amount": 0,
    "payload": "01拼接上投票事务的哈希值" ,
    "to": "0000000000000000000000000000000000000005",
    "signature": "****",
    "hash": "**",
}
```

节点A只有撤回给B的投票后才能重新投票

### PoW

PoW 即工作量证明，优先解决哈希值难题的矿工可获得区块打包权，工作量证明用伪代码表示如下：

``` python
while True:
  r = randbytes(32) # 生成随机 payload
  block['payload'] = r
  digest = hash(block)
  if digest < nbits: 
    # 完成了工作量证明
    break
```

这里的 PoW 使用内置合约保存难度值，该内置合约的地址为 ```0000000000000000000000000000000000000002` ``，难度值会随时调整，不像比特币一样把难度值保存在区块头中。

PoW没有对 p2p 作出限制，也没有限制矿工的数量，只要能优先解决哈希值难题的矿工就可以获得区块打包权。

## keystore

keystore 用于保存用户的私钥

``` jsonc
{
  "publicKey" : "02ef5b1a65b7f2afbc20ecd0f1400892ea8c4e2c86fd0491abcb8be8af7f1f6a41", // 用户的公钥

  "crypto" : {
    "cipher" : "sm4-128-ctr", // 使用的对称加密算法 
    "cipherText" : "f078aefe661bc4498d686216e263c77a9026dcb4249d935c0b26b0b289cab098", // 加密过后的私钥

    "iv" : "ce69a3f220a7d6e6eea04eb927e9f4d1", 
    "salt" : "cfa496f673a4eeb508d301822b35b867aa05533225774fd47509908e6357e0cd"
  },
  "id" : "39453f15-b72d-4599-a86e-6d7459fa19d7",
  "version" : "1",
  "mac" : "345a86d380a043d20f36712ebb4bed37bdab412e73a290cab5aee6203c2ef83a",
  "kdf" : "sm2-kdf",
  "address" : "c86d486ac528ff14c17b9a9190b4d3c79a0291a8"
}
```

keystore 的生成过程用伪代码表示:

``` python
keystore = {}

sk = b'********' # 私钥
password = '********' # 用户输入的密码
salt = randbytes(32) # 生成随机盐
iv = randbytes(16) # 生成随机向量
key = sm3(salt + password.encode('ascii'))[:16] # 推导出key
keystore['salt'] = salt.hex()
keystore['iv'] = iv.hex()
keystore['ciphertext'] = sm4.encrypt_ecb_nopadding(key,sk) # 对私钥进行加密保存
keystore['mac'] = sm3(key + ciphertext).hex() # 生成 mac
keystore['id'] == uuid() 
keystore['version'] = '1'
```

keystore 的读取过程用伪代码表示：

``` python
password = '********' # 用户输入的密码
salt = bytes.fromhex(keystore['salt']) # 盐
iv = bytes.fromhex(keystore['iv']) # iv
key = sm3(salt + password.encode('ascii'))[:16]
cipher = bytes.fromhex(keystore['ciphertext'])
sk = sm4.decrypt_ecb_nopadding(key,cipher) # 读取到私钥
```

nodejs keystore 生成可解析可以使用 sdk ```https://github.com/TrustedDataFramework/js-sdk```

## P2P

P2P网络基于 grpc 或者 websocket, 两者都是二进制协议，都支持长连接。

### 消息结构

p2p 消息的序列化和反序列化基于 protobuf

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | NOTHING=0, PING=1, PONG=2, LOOKUP=3, PEERS=4, ANOTHER=5, DISCONNECT=6, MULTI_PART=7 |
| created_at    |   int   | 消息发送的时间戳 |
|    remote_peer  |   string   | 消息的发送者的uri |
| ttl | int | 每当消息被转发一次，ttl会减1，防止消息被无限次转发 |
| nonce| int | 递增随机数，防止出现哈希值相同的消息 |
|signature| bytes | 消息发送者对消息整体的签名，用于防止消息内容被篡改 |
| data | bytes | 消息体，具体内容取决于 code|

备注：
NOTHING 是空消息，不作处理
PING和PONG主要用于节点发现、保活还有密钥协商。LOOKUP和PEERS用于节点发现。
DISCONNECT表示节点即将断开，MULTI_PART用于将消息分包发送。

当 code=ANOTHER 时，data 中的内容一般是 rlp 编码后的结构化数据

### 密钥协商

在节点A和节点B之间进行加密通信，需要协商出一个共同的且只有A和B知道的密钥。对于节点A，这个密钥可以用节点B的公钥和节点A的私钥计算得出，而对于节点B这个密钥可以用节点A的公钥和节点B的私钥计算得出，这样在经过一次PING和PONG后，节点A和B之间的ANOTHER类型的消息都可以用这个密钥加密，默认的对称加密算法是sm4

### 节点发现

每个节点都拥有自己的椭圆曲线密钥对，以sm2为例, sm2的公钥长度是33字节私钥长度是32字节。
节点在P2P网络中的唯一标识符就是自己的公钥，这个公钥信息以十六进制编码在节点的uri中。

例如

``` 
node://03a5acb1faa4dfe70f8e038e297de499cb258cc00afda2822e27291ed180013bd8@192.168.1.3:9999
```

当节点发现关闭时，节点不处理 LOOKUP 和 PEERS 消息，只会和种子节点或信任节点保持连接。

当节点发现开启时，若想在启动时连接种子节点，可以在种子节点列表里把这个uri加入其中，如果uri中不包含公钥，例如node://192.168.1.3:9999，节点会在建立连接时获取对方的公钥。

节点发现基于kademlia协议，在此协议中，每个节点都有自己的唯一标识，节点之间可以通过这个标识计算出距离，这个距离和ip地址物理地址无关，只和彼此的公钥有关。根据不同的距离，节点会将其他节点分到不同的桶当中，例如sm2的公钥长度是33个字节，264个bit，那么桶的数量就有264个。kademlia协议可以保证：当节点的邻居节点均匀分布到不同的桶当中时，可以最大化网络的连通性。

节点会对自己的邻居节点根据活跃程度进行记分，每隔 `sunflower.p2p.discover-rate` 秒会向其他节点发送一个PING消息和一个Lookup消息，其他节点在收到PING消息后会回复PONG消息。

节点B收到 节点A的Lookup 消息会返回一个 PEERS 消息，PEERS 消息中包含了节点B的邻居节点信息。节点A收到来自节点B的PEERS消息，会尝试连接PEERS消息中的邻居节点。

节点A收到节点B的任何类型的消息都会给节点B加32分，这个分值的半衰期是同样是 `sunflower.p2p.discover-rate` 秒，在A和B节点持续保持连接的状态下，各自的分值始终都是大于0的，当节点B主动断开与A的连接后，B的分值会快速衰减至0，当B的分值为0后，节点A认为节点B已长时间处于离线状态，将节点B从邻居节点列表中删除。

### 消息分包

websocket 和 grpc 都对单个消息的大小作了限制，为了实现发送较大的单个消息，需要对大消息进行分包发送，当消息大小超过 `sunflower.p2p.max-packet-size` 时，大消息会被拆成多个 code 为 MULTI_PART 的消息逐次发送，接收方收到 MULTI_PART 的消息后会将消息暂时保存在内存中，直到所有的分包消息都已收到，接收方再将收到的分包消息合并再做后续处理。

### 区块同步和事务广播

区块同步协议基于五种不同的消息类型，传输时均采用RLP编码。

在区块同步过程中，节点会维持一个优先级队列，这个队列包含了所有等待验证和写入的区块，队头的区块高度最低，而且包含了较多的事务。

以下数据结构中的 code 和 P2P 消息中的 code 没有任何关联，最终整体会经过 rlp 编码填充到 P2P 消息中的 data 字段中

1. 状态消息(Status)

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | Status = 4 |
| bestBlockHeight    |   long   | 最高区块高度 |
| bestBlockHash  |   bytes   | 最高区块的哈希值 |
| genesisBlockHash | bytes | 创世区块的哈希值 |
| prunedHeight| long | 压缩目标区块的高度 |
|prunedHash| bytes | 压缩目标区块的哈希 |

2. 区块请求(Get Blocks)

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | GET_BLOCKS = 5 |
| startHeight    |   long   | 请求的起始区块高度 |
| stopHeight  |   long   | 请求的结束区块高度 |
| descend | bool | true为降序, false 为升序 |
| limit| long | 单次请求的最大区块数量限制 |

当节点收到Status消息后，首先会尝试同步孤块，若对方的最高区块的高度大于孤块的高度，节点发出区块请求：startHeight=本地孤块高度 stopHeight=对方最高区块的高度，descend=true, limit取决于本地的区块传输数量限制

其次节点会比较自己的本地的最高区块高度和对方的最高区块高度，如果本地最高的区块高度小于等于对方最高区块的高度，或者本地最高区块的哈希值和对方最高区块的哈希值不相等，节点会向对方发送区块请求：startHeight=本地最高区块高度 stopHeight=对方最高区块的高度，descend=false, limit取决于本地的区块传输数量限制

3. 区块(Blocks)

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | BLOCKS = 6 |
| blocks    |  数组  | 区块 |

节点在收到区块请求后，需要对请求做出回应，回应的内容是rlp编码格式的区块体，收到区块的节点会把区块暂存到待写入队列中

4. 提案(Proposal)

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | PROPOSAL = 7 |
| block    |  BLOCK  | 区块 |

矿工节点在区块生成成功后会向邻居节点广播生产出的区块，收到区块的节点会把区块暂存到待写入队列中，同时会把这个消息中继广播给其他邻居节点

5. 事务(Transaction)

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | TRANSACTION = 8 |
| transaction    |  Transaction  | 事务 |

当有用户通过rpc发送事务后，节点会把这个事务通过p2p广播给其他节点，其他节点收到后会把事务放入事务内存池，同时中继广播这个事务。

### 消息限流

为了防止过于频繁的请求，在同步时会对 Status 和 Get Blocks 消息作限流，默认限制在 16hz。

### 快速同步和区块压缩

当新节点从零开始同步时，可以在配置文件中指定目标区块进行快速同步，节点启动后会进入快速同步状态。在快速同步状态的节点会优先连接能够提供快速同步区块和账户状态的节点，当所有账户状态下载好后，节点对状态树根进行校验，若状态树校验成功，快速同步完成，节点可以正常出块和同步。

当本地区块高度较高，存在大量过时数据时可以进行区块压缩，删除过时的区块头、事务和账户状态。区块压缩不会影响同步和出块，但删除的历史账本数据无法再被查询。快速同步用到的消息类型有：

1. 获取账户

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | GET_ACCOUNTS = 11 |
| stateRoot    |  bytes | 目标区块的状态树树根 |
| maxAccounts | int | 单次传输的账户最大数量 |

2. 账户

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | ACCOUNTS = 12 |
| total   |  long | 账户总数，只有当 traversed 为 true 时，此字段才有意义 |
| accounts | 数组 | 账户 |
| traversed | bool | 对方是否已将所有账户传输完成 |

## 智能合约

WebAssembly是由W3C开发的一个标准组的有效率, 轻量级的指令集。这意味着我们可以编译不同类型的编程语言, 从C++, Rust 到 go 等, 可以在浏览器中运行几乎代替JavaScript。WebAssembly，简称WASM，是内存安全的、平台独立的，并且可以有效地映射到所有类型的CPU架构。

TDS 内部实现了一个 WebAssembly 执行引擎，不同编程语言编写的智能合约代码在经过编译后，都可以部署到 TDS 链上。下面以 AssemblyScript 为例，简单介绍下智能合约的编写、部署和调用。

### 入门

以下是一个智能合约 hello world 示例：

``` typescript
import {DB, Result, log, RLP, Context} from "../lib";

const KEY = Uint8Array.wrap(String.UTF8.encode('key'));

// every contract should had a function named by init
// which will be called at most once when contract deployed
export function init(): void{
    log("contract deployed successfully by index.ts")
}

export function increment(): void {
    let i = DB.has(KEY) ?  RLP.decodeU64(DB.get(KEY)) : 0;
    i++;
    log("call contract successful counter = " + i.toString());
    DB.set(KEY, RLP.encodeU64(i));
}

export function get(): void{
    let i = DB.has(KEY) ?  RLP.decodeU64(DB.get(KEY)) : 0;
    Result.write(RLP.encodeU64(i))
}

export function getN(): void{
    let i = DB.has(KEY) ?  RLP.decodeU64(DB.get(KEY)) : 0;
    const args = Context.args();
    assert(args.method === 'getN', 'method is getN');
    const j = RLP.decodeU64(args.parameters);
    Result.write(RLP.encodeU64(i + j))
}

export function addN(): void {
    const args = Context.args();
    let i = DB.has(KEY) ?  RLP.decodeU64(DB.get(KEY)) : 0;
    i+= RLP.decodeU64(args);
    DB.set(KEY, RLP.encodeU64(i));
}
```

在这个智能合约中，第一个方法是 init 方法，这个方法只有在合约部署时会被调用。

在智能合约的第一行引入了 DB 这个依赖，在智能合约中可以通过 DB 操作合约存储，例如 increament 方法在每次触发时会把 DB 中读取一个整数，再把整数加一，然后再保存到 DB 中。

该合约部署成功后，如果想调用 `increment` 方法，需要构造事务，构造事务的伪代码如下:

``` js
// 构造 payload 需要把方法长度作为第一个字节拼在方法名的 ascii 编码之前
const method = Buffer.from('increment', 'ascii')
const prefix = Buffer.of([method.length])
const tx = {
    "version": 1634693120,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "你的公钥",
    "gasPrice": 0,
    "amount": 0,
    "payload": Buffer.concat([prefix, method]).toString('hex'),
    "to": "合约的地址",
    "signature": "****",
    "hash": "**",
}
```

如果想查看这个 i 的最新数值，可以调用如下的伪代码

``` js
const rlp = require('rlp') // https://www.npmjs.com/package/rlp
const BigInteger = require('bigi') // https://www.npmjs.com/package/bigi

// 构造 parameters 同样需要把方法长度作为第一个字节拼在方法名的 ascii 编码之前
const contractAddress = '****'
const method = Buffer.from('get', 'ascii')
const prefix = Buffer.of([method.length])
const parameters = Buffer.concat([prefix, method]).toString('hex')
axios.get( `localhost:8888/rpc/contract/${contractAddress}?parameters=${parameters}` )
    .then(r => {
        const d = r.data.data
        // 因为 合约中 Result.write 的是 i 的 rlp 编码，所以需要再解码一次
        const i = BigIneger.fromBuffer(rlp.decode(Buffer.from(d, 'hex')))
        console.log( `i = ${i.intValue()}` )
    })
```

### 方法调用

在外界与合约交互有两种方式：

1. 发起请求

若想在查看合约的同时加入参数，可以用二进制的方式传入，例如要调用以上合约的 ```getN``` 方法，并且把 j 作为参数传递：

``` js
const contractAddress = '****' // 合约的地址
const method = Buffer.from('getN', 'ascii')
const prefix = Buffer.of([method.length])
const j = rlp.encode(123456) // 使用 rlp 专成 Uint8 Array
const parameters = Buffer.concat([prefix, method, j]).toString('hex')
axios.get( `localhost:8888/rpc/contract/${contractAddress}?parameters=${parameters}` )
    .then(r => {
        const d = r.data.data
        // 因为 合约中 Result.write 的是 i 的 rlp 编码，所以需要再解码一次
        const i = BigIneger.fromBuffer(rlp.decode(Buffer.from(d, 'hex')))
        console.log( `i = ${i.intValue()}` )
    })
```

通过发起请求智能查看合约的状态，无法改变合约的存储，如果在查看合约状态时试图改写 ```DB```，接口会报错

2. 构造事务

只有通过构造事务采可以改变合约的存储状态，例如方法 ```addN()` ``，调用该方法的事务如下：

``` js
const rlp = require('rlp') // https://www.npmjs.com/package/rlp
const method = Buffer.from('addN', 'ascii')
const prefix = Buffer.of([method.length])
const args = rlp.encode(123456)

const tx = {
    "version": 1634693120,
    "type": 3,
    "createdAt": "2020-07-29T07:16:40Z",
    "nonce": 1,
    "from": "你的公钥",
    "gasPrice": 0,
    "amount": 0,
    "payload": Buffer.concat([prefix, method, args]).toString('hex'),
    "to": "合约的地址",
    "signature": "****",
    "hash": "**",
}
```

如果通过构造事务调用合约，可以通过在payload后面拼接上参数，其他和区块链有关的参数也可以通过 ```Context` `` 类获得

``` ts
const header = context.header(); // 获得区块头
const tx = context.transaction(); // 获取当前的事务
const contract = context.contract(); // 获取合约自身
```

## Assembly Script

[Assemblyscript](https://www.assemblyscript.org/) 是 Typescript 的一个更严格的子集，它的语法较为简单，可以用 [Assemblyscript编译器](https://github.com/AssemblyScript/assemblyscript) 编译成符合 web assembly 标准的二进制字节码。

Assemblyscript 的语法可以参考[官方文档](https://www.assemblyscript.org/)。

###  下载智能合约编写模版

``` sh
git clone https://github.com/TrustedDataFramework/assembly-script-template
cd assembly-script-template
npm install
```

在 ```package.json``` 中有两个重要的依赖项：

``` json
{
  "@salaku/js-sdk": "^0.2.10",
  "@salaku/sm-crypto": "^0.1.8"
}
```

其中 [```@salaku/sm-crypto```](https://github.com/TrustedDataFramework/sm-crypto) 包含了国密 sm2、sm3 和 sm4 的 javascript 实现，我们需要它来作哈希值计算和事务的签名等，[` ` `@salaku/js-sdk` ``](https://github.com/TrustedDataFramework/js-sdk) 封装了事务构造和rpc调用的方法，可以简化智能合约的开发。

### 编译和部署合约

1. 新建 local 目录

``` sh
mkdir local # local 文件夹在 git 中被忽略了
```

2. 编写合约源代码

然后新建一个 hello-world.ts 文件，复制以下内容到 hello-world.ts 中

``` sh
touch local/hello-world.ts
```

``` typescript
import {log} from "../lib";

// every contract should contains a function named 'init'
// which will be called when contract deployed
export function init(): void{
  log('hello world');
}

export function invoke(): void{
    log("hello world");
}
```

3. 编写配置文件 

再新建 config.json 文件，复制以下内容到 config.json

``` sh
touch local/config.json
```

``` json
{
  "version": "1634693120",
  "host": "192.168.1.171",
  "port": "7010",
  "source": "local/hello-world.ts",
  "private-key": "**",
  "asc-path": "node_modules/.bin/asc",
  "gas-price": 0
}
```

  + 各个字段说明如下：

    - version 表示事务的版本
    - host 表示合约部署的节点的主机
    - port 表示节点的 rpc 端口号
    - source 表示合约的源代码文件
    - private-key 需要填写私钥，用于对事务作签名
    - asc-path 是编译器的路径，对于 linux 和 mac 一般是 ```node_modules/.bin/asc```，对于 windows 一般是 ```node_modules/.bin/asc.cmd```
    - gas-price 表示手续费的单价，对于私链或者联盟链一般填0即可

4. 读取配置文件

利用 nodejs 提供的 require 函数可以轻松地读取 json 文件，因为我们新建的文件 ```config.json``` 位于 local 目录下，所以我们需要通过环境变量的方式将配置文件的路径传递过去

```js
const path = require('path')

function getConfigPath() {
  // 如果环境变量CONFIG=没有值，则选择当前目录下的 config.json 文件
    if (!process.env['CONFIG'])
        return path.join(process.cwd(), 'config.json')
      
    const c = process.env['CONFIG']
    // 判断环境变量的CONFIG=的值是否是绝对路径，如果是绝对路径直接 return 
    if (path.isAbsolute(c))
        return c
    // 如果是相对路径就把工作目录和这个相对路径拼接得到绝对路径    
    return path.join(process.cwd(), c)
}

// 读取配置
// 现在我们可以通过环境变量 CONFIG 传递配置文件的路径
const conf = require(getConfigPath());
```

5. 编译 

可以使用 ```@salaku/js-sdk``` 提供的方法编译合约：

``` js
const tool = require('@salaku/js-sdk')
// getConfigPath 的代码参考 5.配置
const conf = require(getConfigPath());

async function main() {
    // 编译合约源代码得到wasm二进制字节码，注意 wasm 的类型是 buffer
    const wasm = await tool.compileContract(conf['asc-path'], conf.source)
}
```

6. 构造事务

``` js
// 事务构造工具
const builder = new tool.TransactionBuilder(conf.version, conf['private-key'], conf['gas-price'] || 0)
```

有了事务构造工具和wasm字节码，就可以构造合约部署的事务了

``` js
const tx = builder.buildDeploy(wasm, 0)
```

7. 创建 rpc 工具，签发事务

``` js
const rpc = new tool.RPC(conf.host, conf.port)
```

有了 rpc 工具后，可以获取实时的 nonce，新的事务需要填充 nonce 后再作签名

``` js
const pk = tool.privateKey2PublicKey(conf['private-key']) // 把私钥转公钥
tx.nonce = (await rpc.getNonce(pk)) + 1 // 用公钥获取 nonce
builder.sign(tx) // 对事务作签名
const resp = await rpc.sendTransaction(tx) // 发送事务得到节点的返回值
```

8. 完整代码(deploy.js):

``` js
/**
 * 智能合约部署示例
 */

function getConfigPath() {
    if (!process.env['CONFIG'])
        return path.join(process.cwd(), 'config.json')
    const c = process.env['CONFIG']
    if (path.isAbsolute(c))
        return c
    return path.join(process.cwd(), c)
}

const tool = require('@salaku/js-sdk')
const path = require('path')

// 读取配置
const conf = require(getConfigPath());
const sk = conf['private-key']
const pk = tool.privateKey2PublicKey(sk)

// 事务构造工具
const builder = new tool.TransactionBuilder(conf.version, sk, conf['gas-price'] || 0)
// rpc 工具
const rpc = new tool.RPC(conf.host, conf.port)

async function main() {
    // 编译合约得到二进制内容
    const o = await tool.compileContract(conf['asc-path'], conf.source)
    // 构造合约部署的事务
    const tx = builder.buildDeploy(o, 0)

    if (!tx.nonce) {
        tx.nonce = (await rpc.getNonce(pk)) + 1
    }

    tool.sign(tx, sk)
    console.log( `contract address = ${tool.getContractAddress(pk, tx.nonce)}` )
    const resp = await tool.sendTransaction(conf.host, conf.port, tx)
    console.log(resp)
}

main().catch(console.error)
```

调用方式 

``` sh
CONFIG=local/config.json node deploy.js
```

### 合约代码结构

1. 函数声明

一份智能合约代码可以由一个或者多个源代码文件组成，只有合约中被声明为 export 的函数才可以被触发

``` typescript
import {log} from "../lib";
export function init(): void{ 
  log('hello world');
}

export function invoke(): void{ 
  execute();
}

function execute(): void{ // init 没有被 export
  log('hello world');
}
```

在这份合约中，invoke 函数可以通过构造事务或者通过rpc触发，而 execute 则不能被触发。

2. init 函数

每份合约都至少要包含一个名为 init 的函数，而且这个函数必须要被导出，这个 init 函数中的代码会在合约被部署时调用。

错误的合约(init 没有被 export)：

``` typescript
import {log} from "../lib";
function init(): void{ // init 没有被 export
  log('hello world');
}
```

错误的合约(缺少 init 函数)

``` typescript
import {log} from "../lib";
export function main(): void{ //  缺少 init 函数
  log('hello world');
}
```

### 状态存储 <span id="anchor1"></span>

1. 临时存储

和 solidity 不同，TDS 合约代码不通过声明全局变量的方式实现持久化存储，例如在以下代码中：

``` typescript
let c: u64;

export function init(): void{
  c = 0;
}

export function inc(): void{
  c++;
}
```

在这份合约中，c 被声明为全局变量，而且在外部可以通过构造事务触发 inc 函数实现 c 的自增，看似只要每次调用 inc 函数 c 都会加一。实际上在这里 c 存储的位置是 wasm 引擎的内存，而 wasm 引擎的内存不会被持久化到区块链中去，c本质上是一个临时存储。因此 inc 函数无论触发了多少次，c 的数值依然都是 0。

2. 永久存储

TDS 智能合约提供了实现永久存储的方法，由内置对象 ```DB``` 实现，```DB``` 本质上是一个 key value 存储，存储的 key 和 value 都是二进制格式，Assemblyscript 使用 Uint8Array 表示二进制的数据。

3. DB 类基本操作

```typescript
import {DB, DBIterator} from './lib'

// 在保存字符串键值对前，要先把字符串转成二进制数据
function str2bin(str: string): Uint8Array{
  return Uint8Array.wrap(String.UTF8.encode(str));
}

// 把从 DB 中读取的二进制数据转成字符串
function bin2str(bin: Uint8Array): string{
  return String.UTF8.encode(bin.buffer);
}

export function init(): void{
  // 保存一个字符串键值对 （增、改）
  DB.set(str2bin('key'), str2bin('value'));

  // 删除一个字符串键值对 （删）
  DB.remove(str2bin('key'));

  // 判断 key 是否存在 （查）
  const exists = DB.has(str2bin('key'));

  if(!exists){
    DB.set(str2bin('key'), str2bin('value'));
  } else{
      // 打印 value 的值 （查）
      // 因为 Assemblyscript 没有 null 类型，如果 exists 为 false 的情况下调用 DB.get(str2bin('key')) 会报异常，合约执行会终止
    log(bin2str(DB.get(str2bin('key')));
  }
}
```

4. 迭代器 DBIterator

使用迭代器可以遍历整个合约状态存储

```typescript
import {DB, DBIterator, log, Context} from './lib'

// 在保存字符串键值对前，要先把字符串转成二进制数据
function str2bin(str: string): Uint8Array{
  return Uint8Array.wrap(String.UTF8.encode(str));
}

// 把从 DB 中读取的二进制数据转成字符串
function bin2str(bin: Uint8Array): string{
  return String.UTF8.encode(bin.buffer);
}

export function iterate(): void{
  DBIterator.reset();
  while(DBIterator.hasNext()){
    const entry = DBIterator.next();
    log('key = ' + bin2str(entry.key) + ' value = ' + bin2str(entry.value));
  }
}
```

### 触发 <span id="anchor0"></span>

触发合约中的方法有两种方式，一种是通过 rpc 触发，另一种是通过事务触发。

1. rpc 触发

通过 rpc 触发的限制在于，触发的方法对合约状态存储必须是只读的，而且无法获得区块链的上下文对象，例如当前的事务、父区块的哈希值，在以下合约中：

```typescript
import {DB, DBIterator, log, Result} from './lib'

// 在保存字符串键值对前，要先把字符串转成二进制数据
function str2bin(str: string): Uint8Array{
  return Uint8Array.wrap(String.UTF8.encode(str));
}

// 把从 DB 中读取的二进制数据转成字符串
function bin2str(bin: Uint8Array): string{
  return String.UTF8.encode(bin.buffer);
}

// 把 key 设置为 0
export function init(): void{
  DB.set(str2bin('key'), str2bin('0'));
}

// 把 key 自增
export function inc(): void{
  const val = DB.get(str2bin('key'));
  const v = parseInt(bin2str(val));
  v++;
  DB.set(str2bin('key'), str2bin(v.toString()));
}

// 打印 key
export function logKey(): void{
  const val = DB.get(str2bin('key'));
  log(bin2str(val));
}
```

在这份合约中，```inc``` 函数对合约状态作了修改，因为无法通过 rpc 触发 ```inc``` 函数，而 ```logKey``` 函数没有对合约状态作修改，属于只读函数，所以可以用 rpc 触发 ```logKey``` 函数。

我们可以通过 rpc 触发合约中的只读函数来获取合约状态，而内置对象 ```Result``` 是 rpc 的返回值与合约函数传递接口，我们需要通过 ```Result``` 提供的 ```write``` api 来获取合约中的数据。例如我们在上文的合约的基础上增加一个函数 ```getKey```。

```typescript
export function getKey(): void{
  const val = DB.get(str2bin('key'));
  Result.write(val);
}
```

```Result.write``` 接受二进制格式的数据，并在外部应用中以十六进制编码的方式呈现，在外部展示合约中的值还需要转码，

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
async function main(){
  const data = await rpc.viewContract('***合约地址***', 'getKey')
  const val = Buffer.from(data, 'hex').toString('utf8')
  console.log(`val = ${val}`)
}

main()
```

2. rpc 触发时传参

rpc 触发合约中的函数时，可以用内置对象 ```Context``` 获取参数，例如我把 ```getKey``` 稍作修改，改成 ```getKeyAddN``` 函数

```typescript
export function getKeyAddN(): void{
  // 读取额外参数
  const p = Context.args().parameters;
  // 转码为 int 类型
  const j = parseInt(bin2str(p));
  // 读取 db 中的数值
  const val = parseInt(bin2str(DB.get(str2bin('key'))));
  // 作加法后返回
  Result.write(str2bin((j + val).toString()));
}
```

rpc 触发代码：

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
async function main(){
  const data = await rpc.viewContract('***合约地址***', 'getKeyAddN', Buffer.from('128', 'ascii'))
  const val = Buffer.from(data, 'hex').toString('utf8')
  console.log(`val = ${val}`)
}

main()
```

3. 事务触发

通过事务触发可以对合约状态作写入、删改等操作，也可以在触发的函数中获取到区块链的上下文对象。


例如要通过事务触发以上合约中的 ```inc``` 函数可以执行 nodejs 代码：

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
const builder = new tool.TransactionBuilder(conf.version, conf['private-key'], conf['gas-price'] || 0)
const pk = tool.privateKey2PublicKey(conf['private-key']) // 把私钥转公钥

async function main(){
  const tx = builder.buildContractCall('**合约地址**', tool.buildPayload('inc'), 0)
  tx.nonce = (await rpc.getNonce(pk)) + 1

  builder.sign(tx)
  rpc.sendTransaction(tx)
}

main()
```

4. 事务触发时带参

事务触发合约中的函数时，也可以用内置对象 ```Context``` 获取参数，例如我把 ```inc``` 稍作修改，改成 ```incN``` 函数

```typescript
export function incN(): void{
  // 读取额外参数
  const p = Context.args().parameters;
  // 转码为 int 类型
  const j = parseInt(bin2str(p));
  // 读取 db 中的数值
  const val = parseInt(bin2str(DB.get(str2bin('key'))));
  // 作加法后存储
  DB.set(str2bin('key'), str2bin((p + j).toString()));
}
```

例如要通过事务触发以上合约中的 ```incN``` 函数，把 ```key``` 对应的值加上 128 可以执行 nodejs 代码：

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
const builder = new tool.TransactionBuilder(conf.version, conf['private-key'], conf['gas-price'] || 0)
const pk = tool.privateKey2PublicKey(conf['private-key']) // 把私钥转公钥

async function main(){
  const payload = tool.buildPayload('incN', Buffer.from('128', 'ascii'))
  const tx = builder.buildContractCall('**合约地址**', payload, 0)
  tx.nonce = (await rpc.getNonce(pk)) + 1
  builder.sign(tx)
  rpc.sendTransaction(tx)
}

main()
```

### 内置对象

我们利用了 Assemblyscript 丰富的扩展性，内建了一些对象和类，使得智能合约的编写更加简单，以下列举了内置对象的使用示例

1. log 函数

区块链节点在执行 log 函数时，会把 log 的参数打印到标准输出，可以用于调试智能合约

```typescript
import {log} from './lib'

export function init(): void{
  log('hello world');
}
```

2. Context 类

对于 rpc 触发的函数，可以通过 Context 拿到 rpc 调用的参数，对于事务触发的函数，可以通过 Context 拿到事务中payload 的参数，具体可以参考 [触发](#anchor0) 章节。

除此之外，事务触发的函数或者 init 函数可以通过 Context 拿到区块链上下文对象，示例如下：

```typescript
import {Context} from './lib'

export function init(): void{
  // 获得事务所在的区块的区块头
  // 包含了父区块的哈希，区块的创建时间和区块的高度
  const header = Context.header();

  // 获得当前的事务，包含了事务的所有字段
  const tx = Context.transaction();

  // 当前的合约的地址，部署时的nonce和合约的创建者的地址
  const contract = Context.contract();
}
```

3. DB 

DB 的使用可以参考[状态存储](#anchor1) 章节。

4. Decimal

Decimal 类用于实现精确的十进制有限小数的四则运算


```typescript
import {Decimal} from './lib'

export function init(): void{
  // 加法
  let d = Decimal.add('0.01', '0.02');

  // 减法
  d = Decimal.sub(d, '0.02');

  // 乘法
  d = Decimal.mul(d, 10);

  // 除法，限制小数点后10位，如果除不尽则抛出异常
  d = Decimal.div(d, 2, 10);

  // 比较
  let c = Decimal.compare('0.01', '0.02');
}
```

5. Event 类

Event 用于从合约内部向外界异步通知，区块链节点在启动时会创建一个 socketio 服务，默认的端口是 10004，外部程序可以在连接 socketio 服务后监听智能合约的事件。

例如以下合约部署后，在前端可以监听 emit 函数触发时发送的 ```event-name``` 事件

```typescript
import  from './lib'

export function init(): void{

}

export function emit(): void{
  const arr = new Uint8Array(1);
  arr[0] = 255;
  Event.emit('event-name', arr);
}
```

前端代码：

```html
<html>
  <body>
    <script type="text/javascript">
      function connectSocket() {
          let socket = window.socket
          if(socket != null){
              socket.disconnect()
          }
          window.socket = io.connect('http://localhost:10004')
          socket = window.socket
          socket.on('***合约地址***:event-name', console.log)
      }
      connectSocket()          
    </script>
  </body>
</html>
```

因为 [255] 的十六进制编码是 "ff" 当 emit 被触发时，前端将会打印出对象:

```json
{
  "data": "ff"
}
```

6. Hex 类

Hex 包含了十六进制编码和解码

```typescript
import {Hex} from './lib'

export function init(): void{
  // 十六进制编码转二进制数组
  const arr = Hex.decode('ff');

  // 二进制数组转十六进制编码
  log(Hex.encode(arr));
}
```

7. Hash 类

Hash 包含了常用的哈希值算法

```typescript
import {Hash} from './lib'

export function init(): void{
  // keccak256
  let digest = Hash.keccak256(Hex.decode('ff'));

  // sm3
  digest = Hash.sm3(Hex.decode('ff'));
}
```

### RLP 编码/解码

### 安全设计

1. 异常处理


2. 溢出