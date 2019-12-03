# Consortium Development Guide

1. git subtree usage

```shell script
git remote add someorigin https://github.com/someproject # add remote
git subtree add --prefix=foldername someorigin master # add folder MonadJ as subtree code directory
git subtree push --prefix=foldername someorigin master # push subtree local change to remote
git subtree pull --prefix=foldername someorigin master # pull from remote 
```

2. spring data jpa usage

https://docs.spring.io/spring-data/jpa/docs/current/reference/html/

3. lombok usage

https://jingyan.baidu.com/article/0a52e3f4e53ca1bf63ed725c.html

4. configurations override

-Dspring.config.location=classpath:\application.yml,some-path\custom-config.yml

5. web assembly text format compiler

https://github.com/WebAssembly/wabt/releases

6. smart contract development

- install rust toolchain https://www.rust-lang.org/tools/install
- install rust wasm build target ```rustup target add wasm32-unknown-unknown``` 
- build to wasm ```cargo build ```

7. RLP Encoding/Decoding

- declaring your pojo

```java
    public static class Node{  
        // RLP annotation specify the order of field in encoded list
        @RLP(0)
        public String name;
    
        // list field require an ElementType Annotation to specify its element type
        @RLP(1)
        @ElementType(Node.class)
        public List<Node> children;

        public Node() {
        }

        public Node(String name) {
            this.name = name;
        }

        public void addChildren(Collection<Node> nodes){
            if(children == null){
                children = new ArrayList<>();
            }
            if (!nodes.stream().allMatch(x -> x != this)){
                throw new RuntimeException("tree like object cannot add self as children");
            }
            children.addAll(nodes);
        }
        
        public static void main(String[] args){
            Node root = new Node("1");
            root.addChildren(Arrays.asList(new Node("2"), new Node("3")));
            Node node2 = root.children.get(0);
            node2.addChildren(Arrays.asList(new Node("4"), new Node("5")));
            root.children.get(1).addChildren(Arrays.asList(new Node("6"), new Node("7")));
    
            // encode to byte array
            byte[] encoded = RLPSerializer.SERIALIZER.serialize(root);
            // encode to rlp element
            RLPElement el = RLPElement.encode(root);
            // decode from byte array
            Node root2 = RLPDeserializer.deserialize(encoded, Node.class);
            assert root2.children.get(0).children.get(0).name.equals("4");
            assert root2.children.get(0).children.get(1).name.equals("5");
            assert root2.children.get(1).children.get(0).name.equals("6");
            assert root2.children.get(1).children.get(1).name.equals("7");            
        }       
    }
```

- custom encoding/decoding

```java
public class Main{
    public static class MapEncoderDecoder implements RLPEncoder<Map<String, String>>, RLPDecoder<Map<String, String>> {
        @Override
        public Map<String, String> decode(RLPElement element) {
            RLPList list = element.getAsList();
            Map<String, String> map = new HashMap<>(list.size() / 2);
            for (int i = 0; i < list.size(); i += 2) {
                map.put(list.get(i).getAsItem().getString(), list.get(i+1).getAsItem().getString());
            }
            return map;
        }

        @Override
        public RLPElement encode(Map<String, String> o) {
            RLPList list = RLPList.createEmpty(o.size() * 2);
            o.keySet().stream().sorted(String::compareTo).forEach(x -> {
                list.add(RLPItem.fromString(x));
                list.add(RLPItem.fromString(o.get(x)));
            });
            return list;
        }
    }

    public static class MapWrapper{
        @RLP
        @RLPEncoding(MapEncoderDecoder.class)
        @RLPDecoding(MapEncoderDecoder.class)
        public Map<String, String> map;

        public MapWrapper(Map<String, String> map) {
            this.map = map;
        }

        public MapWrapper() {
        }
    }

    public public static void main(String[] args){
              Map<String, String> m = new HashMap<>();
              m.put("a", "1");
              m.put("b", "2");
              byte[] encoded = RLPSerializer.SERIALIZER.serialize(new MapWrapper(m));
              MapWrapper decoded = RLPDeserializer.deserialize(encoded, MapWrapper.class);
              assert decoded.map.get("a").equals("1");
    }
}
```

Benchmark compare to EthereumJ:

decoding list 10000000 times: 

ethereumJ: 3698ms 
our: 2515ms

8. github package

    1. set GITHUB_USERNAME environment as your github login name
    2. generate a token for github package: https://github.com/settings/tokens
    3. set GITHUB_TOKEN environment as the token generated above. 

## Commands

1. start application: (Windows) 

```.\gradlew consortium:bootRun```

2. clear builds (Windows) 

```.\gradlew consortium:clean```

3. build and run fat jar (Windows)

```shell script
.\gradlew consortium:bootJar       

# override default spring config with your custom config                     
java -jar consortium\build\libs\consortium-0.0.1-SNAPSHOT.jar -Dspring.config.location=classpath:\application.yml,some-path\custom-config.yml

# you can also load your config by environment
set SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml 
java -jar consortium\build\libs\consortium-0.0.1-SNAPSHOT.jar
```  

4. rest apis

- /account/{address} display account 
- /config display application configuration
