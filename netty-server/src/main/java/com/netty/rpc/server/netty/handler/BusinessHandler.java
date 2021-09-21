package com.netty.rpc.server.netty.handler;

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
 * netty server business logic process handler
 */
public class BusinessHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(BusinessHandler.class);

    private final Map<String, Object> serviceKey2BeanMap;

    /**
     * 业务线程池
     */
    private final ThreadPoolExecutor businessTaskThreadPool;

    public BusinessHandler(Map<String, Object> serviceKey2BeanMap, final ThreadPoolExecutor businessTaskThreadPool) {
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

}
