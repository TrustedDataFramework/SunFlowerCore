# TDS 

[TOC]

## 参数配置

TDS 采用 yml 文件作为参数配置文件，可以在启动时用环境变量SPRING_CONFIG_LOCATION 指定好自定义的配置文件。
例如:

```sh
SPRING_CONFIG_LOCATION=classpath:application.yml,$HOME/Documents/local.yml java -jar sunflower*.jar
```
文件路径之间以逗号分割，后面的配置会覆盖前面的配置。
除了环境变量配置，也可以用命令行参数指定配置文件，例如

```sh
java -jar sunflower*.jar --spring.config.location=classpath:application.yml
```

配置文件中的参数都可以用相应的命令函参数覆盖，
例如配置文件中的配置项 spring.datasource.url 可以在启动时用命令行参数覆盖。

```sh
java -jar app.jar --spring.datasource.url="jdbc:h2:mem:test"
```

### sunflower 配置

```yml
sunflower:
    assert: 'false' # 是否开启断言，默认是 false
    validate: 'false' # 启动时是否校验本地数据，如果设置为true，启动时会校验账本数据
    libs: 'local/jar' # jar包路径，用于加载外部的共识插件
    secret-store: "" # 证书的路径，若填写，节点启动后会打印出一个公钥并且进入阻塞，用户通过工具生成证书文件保存到这个路径后，节点会成功加载证书
```


### 共识参数 (POA)

```yml
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

### POA的创世区块文件

```jsonc
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

### 共识参数 (POW)

```yml
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

### POW的创世区块文件

```jsonc
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

### 共识参数 (POS)

```yml
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


### POS的创世区块文件

```jsonc
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

```yml
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

```yml
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

## 区块头

## 事务

## 普通账户

## keystore

## 证书

## 合约账户

## 共识机制

## 身份鉴权

## p2p

P2P网络基于 grpc 或者 websocket,两者都是二进制协议，都支持长连接。

### 消息结构

p2p 消息的序列化和反序列化基于 protobuf

| 字段名     |   类型   | 说明 |
| ---- | ---- | ---- |
| code | int | NOTHING=0,PING=1,PONG=2,LOOKUP=3,PEERS=4,ANOTHER=5,DISCONNECT=6,MULTI_PART=7 |
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

每个节点都拥有自己的椭圆曲线密钥对，以sm2为例,sm2的公钥长度是33字节私钥长度是32字节。
节点在P2P网络中的唯一标识符就是自己的公钥，这个公钥信息以十六进制编码在节点的uri中。

例如
```
node://03a5acb1faa4dfe70f8e038e297de499cb258cc00afda2822e27291ed180013bd8@192.168.1.3:9999
```

当节点发现关闭时，节点不处理 LOOKUP 和 PEERS 消息，只会和种子节点或信任节点保持连接。

当节点发现开启时，若想在启动时连接种子节点，可以在种子节点列表里把这个uri加入其中，如果uri中不包含公钥，例如node://192.168.1.3:9999，节点会在建立连接时获取对方的公钥。

节点发现基于kademlia协议，在此协议中，每个节点都有自己的唯一标识，节点之间可以通过这个标识计算出距离，这个距离和ip地址物理地址无关，只和彼此的公钥有关。根据不同的距离，节点会将其他节点分到不同的桶当中，例如sm2的公钥长度是33个字节，264个bit，那么桶的数量就有264个。kademlia协议可以保证：当节点的邻居节点均匀分布到不同的桶当中时，可以最大化网络的连通性。

节点会对自己的邻居节点根据活跃程度进行记分，每隔`sunflower.p2p.discover-rate`秒会向其他节点发送一个PING消息和一个Lookup消息，其他节点在收到PING消息后会回复PONG消息。

节点B收到 节点A的Lookup 消息会返回一个 PEERS 消息，PEERS 消息中包含了节点B的邻居节点信息。节点A收到来自节点B的PEERS消息，会尝试连接PEERS消息中的邻居节点。

节点A收到节点B的任何类型的消息都会给节点B加32分，这个分值的半衰期是同样是`sunflower.p2p.discover-rate`秒，在A和B节点持续保持连接的状态下，各自的分值始终都是大于0的，当节点B主动断开与A的连接后，B的分值会快速衰减至0，当B的分值为0后，节点A认为节点B已长时间处于离线状态，将节点B从邻居节点列表中删除。

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


当节点收到Status消息后，首先会尝试同步孤块，若对方的最高区块的高度大于孤块的高度，节点发出区块请求：startHeight=本地孤块高度 stopHeight=对方最高区块的高度，descend=true,limit取决于本地的区块传输数量限制

其次节点会比较自己的本地的最高区块高度和对方的最高区块高度，如果本地最高的区块高度小于等于对方最高区块的高度，或者本地最高区块的哈希值和对方最高区块的哈希值不相等，节点会向对方发送区块请求：startHeight=本地最高区块高度 stopHeight=对方最高区块的高度，descend=false,limit取决于本地的区块传输数量限制

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
| total   |  bytes | 目标区块的状态树树根 |
| accounts | 数组 | 账户 |
| traversed | bool | 对方是否已将所有账户传输完成 |





