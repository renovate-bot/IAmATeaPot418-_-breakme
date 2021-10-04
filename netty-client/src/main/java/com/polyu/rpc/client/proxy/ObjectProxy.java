package com.polyu.rpc.client.proxy;

import com.polyu.rpc.client.manager.HandlerManager;
import com.polyu.rpc.client.netty.handler.RpcClientHandler;
import com.polyu.rpc.client.result.future.RpcFuture;
import com.polyu.rpc.codec.RpcRequest;
import com.polyu.rpc.route.DefaultRpcLoadBalanceHolder;
import com.polyu.rpc.route.RpcLoadBalance;
import com.polyu.rpc.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

public class ObjectProxy<T, P> implements InvocationHandler, RpcService<T, P, SerializableFunction<T>> {
    private static final Logger logger = LoggerFactory.getLogger(ObjectProxy.class);
    private Class<T> clazz;
    private String version;
    private RpcLoadBalance loadBalance;

    public ObjectProxy(Class<T> clazz, String version, RpcLoadBalance loadBalance) {
        this.clazz = clazz;
        this.version = version;
        this.loadBalance = loadBalance;
    }

    public ObjectProxy(Class<T> clazz, String version) {
        this.clazz = clazz;
        this.version = version;
    }

    /**
     * 动态代理调用
     * @param proxy 代理
     * @param method 方法
     * @param args 参数
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setVersion(version);

        String serviceKey = ServiceUtil.makeServiceKey(method.getDeclaringClass().getName(), version);
        RpcClientHandler handler = HandlerManager.chooseHandler(serviceKey, loadBalance == null ? DefaultRpcLoadBalanceHolder.getInstance() : loadBalance);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture.get();
    }

    @Override
    public RpcFuture call(String funcName, Object... args) throws Exception {
        String serviceKey = ServiceUtil.makeServiceKey(this.clazz.getName(), version);
        RpcClientHandler handler = HandlerManager.chooseHandler(serviceKey, loadBalance == null ? DefaultRpcLoadBalanceHolder.getInstance() : loadBalance);
        RpcRequest request = createRequest(this.clazz.getName(), funcName, args);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    @Override
    public RpcFuture call(SerializableFunction<T> tSerializableFunction, Object... args) throws Exception {
        String serviceKey = ServiceUtil.makeServiceKey(this.clazz.getName(), version);
        RpcClientHandler handler = HandlerManager.chooseHandler(serviceKey, loadBalance == null ? DefaultRpcLoadBalanceHolder.getInstance() : loadBalance);
        RpcRequest request = createRequest(this.clazz.getName(), tSerializableFunction.getName(), args);
        return handler.sendRequest(request);
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);
        request.setVersion(version);
        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);

        // Debug
        if (logger.isDebugEnabled()) {
            logger.debug(className);
            logger.debug(methodName);
            for (Class parameterType : parameterTypes) {
                logger.debug(parameterType.getName());
            }
            for (Object arg : args) {
                logger.debug(arg.toString());
            }
        }

        return request;
    }

    private Class<?> getClassType(Object obj) {
        return obj.getClass();
    }

}
