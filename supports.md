# TDS 

## 1. 参数配置

联盟链采用 yml 文件作为参数配置文件，可以在启动时用环境变量SPRING_CONFIG_LOCATION 指定好自定义的配置文件。
例如:

```sh
SPRING_CONFIG_LOCATION=classpath:application.yml,C:\Users\You\Documents\local.yml java -jar sunflower*.jar
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

### 1.1 sunflower 配置

```yml
sunflower:
    assert: 'false' # 是否开启断言，默认是 false
    validate: 'false' # 启动时是否校验本地数据，如果设置为true，启动时会校验账本数据
    libs: 'local/jar' # jar包路径，用于加载外部的共识插件
    secret-store: "" # 证书的路径，若填写，节点启动后会打印出一个公钥并且进入阻塞，用户通过工具生成证书文件保存到这个路径后，节点会成功加载证书
```


### 1.2 共识参数 (POA)

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

### 1.3 POA的创世区块文件

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

### 1.4 共识参数 (POW)

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

### 1.5 POW的创世区块文件

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

### 1.6 共识参数 (POS)

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

## 2. 区块头

## 3. 事务

## 4. 普通账户

## 5. keystore

## 6. 证书

## 7. 合约账户

## 8. 共识机制

## 9. 身份鉴权

