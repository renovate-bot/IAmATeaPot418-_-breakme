package com.netty.rpc.server.netty;

import com.netty.rpc.codec.Beat;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.server.task.BusinessTask;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * netty server handler
 */
public class BusinessHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(BusinessHandler.class);

    private final Map<String, Object> serviceKey2BeanMap;

    /**
     * 业务线程池
     */
    private final ThreadPoolExecutor businessTaskThreadPool;

    BusinessHandler(Map<String, Object> serviceKey2BeanMap, final ThreadPoolExecutor businessTaskThreadPool) {
        this.serviceKey2BeanMap = serviceKey2BeanMap;
        this.businessTaskThreadPool = businessTaskThreadPool;
    }

    /**
     * 获取rpcRequest进行业务处理
     * @param ctx
     * @param request
     */
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) {
        businessTaskThreadPool.execute(new BusinessTask(request, serviceKey2BeanMap, ctx));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Server caught exception: " + cause.getMessage());
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            logger.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
