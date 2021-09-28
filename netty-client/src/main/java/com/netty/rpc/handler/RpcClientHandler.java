package com.netty.rpc.handler;

import com.netty.rpc.connection.ConnectionManager;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import com.netty.rpc.future.RpcFuture;
import com.netty.rpc.protocol.RpcProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;


public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();
    private volatile Channel channel;
    private RpcProtocol rpcProtocol;

    private volatile boolean intentionalClose;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) {
        String requestId = response.getRequestId();
        logger.debug("Receive response: {}.", requestId);
        RpcFuture rpcFuture = pendingRPC.get(requestId);
        if (rpcFuture != null) {
            pendingRPC.remove(requestId);
            rpcFuture.done(response);
        } else {
            logger.warn("Can not get pending response for request id: {}.", requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client caught exception: {}.", cause.getMessage());
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 发送请求
     * @param request RpcRequest
     * @return result future
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        pendingRPC.put(request.getRequestId(), rpcFuture);
        try {
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()) {
                logger.error("Send request {} error.", request.getRequestId());
            }
        } catch (InterruptedException e) {
            logger.error("Send request exception: {}.", e.getMessage());
        }
        return rpcFuture;
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    /**
     * server端超时主动关闭
     * 触发client端重连 以此机制保持长链接
     * 主动关闭则不进行重连接
     * @param ctx
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (isIntentionalClose()) {
            super.channelInactive(ctx);
            ConnectionManager.getInstance().removeHandler(rpcProtocol);
            return;
        }
        logger.info("Connection to server lose, active reconnect mechanism.");
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        try {
            connectionManager.connectServerNode(rpcProtocol);
        } catch (Exception e) {
            connectionManager.removeHandler(rpcProtocol);
        }
    }


    private boolean isIntentionalClose() {
        return intentionalClose;
    }

    /**
     * 主动关闭时设置
     * @param intentionalClose boolean 是否主动关闭
     */
    public void setIntentionalClose(boolean intentionalClose) {
        this.intentionalClose = intentionalClose;
    }
}
