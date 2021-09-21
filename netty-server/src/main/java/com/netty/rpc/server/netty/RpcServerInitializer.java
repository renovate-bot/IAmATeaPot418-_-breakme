package com.netty.rpc.server.netty;

import com.netty.rpc.codec.RpcDecoder;
import com.netty.rpc.codec.RpcEncoder;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import com.netty.rpc.serializer.Serializer;
import com.netty.rpc.serializer.kryo.KryoSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {
    private Map<String, Object> serviceKey2BeanMap;
    private ThreadPoolExecutor businessTaskThreadPool;

    public RpcServerInitializer(Map<String, Object> serviceKey2BeanMap, ThreadPoolExecutor businessTaskThreadPool) {
        this.serviceKey2BeanMap = serviceKey2BeanMap;
        this.businessTaskThreadPool = businessTaskThreadPool;
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        Serializer serializer = KryoSerializer.class.newInstance();
        ChannelPipeline cp = channel.pipeline();
        // cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));
        // cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        cp.addLast(new RpcDecoder(RpcRequest.class, serializer));
        cp.addLast(new RpcEncoder(RpcResponse.class, serializer));
        cp.addLast(new BusinessHandler(serviceKey2BeanMap, businessTaskThreadPool));
    }
}
