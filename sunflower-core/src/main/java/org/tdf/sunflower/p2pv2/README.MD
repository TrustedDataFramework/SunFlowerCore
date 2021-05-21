# Rlpx

outbound

1. PeerClient.connectAsync 
2. Netty Channel Registered Event
3. Channel Initializer(1. create channel, 2. inetSocketAddress = null 
4. Channel.init, 1. set remote id, 2. add handshake handler into pipeline. 3. set mq.channel 
5. Netty Channel active event
6. Handshake handler: 1. set channel.inetSocketAddress 2. handle handshake 3. notify handshake finished
7. 