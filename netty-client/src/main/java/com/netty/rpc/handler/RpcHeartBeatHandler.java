package com.netty.rpc.handler;

import com.netty.rpc.client.ConnectionManager;
import com.netty.rpc.codec.HeartBeat;
import com.netty.rpc.protocol.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class RpcHeartBeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RpcHeartBeatHandler.class);

    private SocketAddress remotePeer;
    private volatile Channel channel;
    private RpcProtocol rpcProtocol;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    /**
     * 心跳事件进行处理
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()) {
                case WRITER_IDLE:
                    logger.info("heart beat WRITER_IDLE event triggered.");
                    break;
                case READER_IDLE:
                    logger.info("heart beat READER_IDLE event triggered.");
                    break;
                case ALL_IDLE:
                    sendHeartBeatPackage();
                    logger.debug("Client send beat-ping to {}", remotePeer);
                    logger.info("heart beat ALL_IDLE event triggered.");
                    break;
            }
        }
    }

    /**
     * 发送心跳包
     */
    private void sendHeartBeatPackage() {
        try {
            channel.writeAndFlush(HeartBeat.BEAT_PING);
        } catch (Exception e) {
            logger.error("Send heartBeatPackage exception: {}", e.getMessage());
        }
    }

    /**
     * server端超时主动关闭
     * 触发client端重连 以此机制保持长链接
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        try {
            connectionManager.connectServerNode(rpcProtocol);
        } catch (Exception e) {
            connectionManager.removeHandler(rpcProtocol);
        }
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }
}
