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

## 区块头

## 事务

## 普通账户

## keystore

## 证书

## 合约账户

## 共识机制

## 身份鉴权

## p2p



