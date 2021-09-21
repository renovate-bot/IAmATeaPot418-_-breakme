package com.netty.rpc.client;

import com.netty.rpc.handler.RpcClientHandler;
import com.netty.rpc.handler.RpcClientInitializer;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.proxy.ObjectProxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConnectionWithOutRegistry {
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8, 600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));
    private static final Logger logger = LoggerFactory.getLogger(ConnectionWithOutRegistry.class);

    public static RpcClientHandler thisHandler;
    public static boolean first = true;



    public static Semaphore s = new Semaphore(0);

    public void connectServerNode(RpcProtocol rpcProtocol) throws InterruptedException {
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }

        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
//                            connectedServerNodes.put(rpcProtocol, handler);
                            handler.setRpcProtocol(rpcProtocol);
//                            signalAvailableHandler();


                            if (first) {
                                thisHandler = handler;
                                s.release(1);
                            }
                        } else {
                            logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                        }
                    }
                });
            }
        });
    }

}
