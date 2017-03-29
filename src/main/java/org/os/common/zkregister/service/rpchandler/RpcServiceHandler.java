package org.os.common.zkregister.service.rpchandler;
import java.util.HashMap;
import java.util.Map;

import org.os.common.zkregister.rpc.RpcRequest;
import org.os.common.zkregister.rpc.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
 
/**
 * RPC服务端:请求处理过程
 */
public class RpcServiceHandler extends SimpleChannelInboundHandler<RpcRequest> {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceHandler.class);
 
    private final Map<String, Object> handlerMap = new HashMap<String, Object>();
 
    public void putServiceObject(String key, Object object) {
    	handlerMap.put(key, object);
    }
    
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            Object result = handle(request);
            response.setResult(result);
        } catch (Throwable t) {
            response.setError(t);
        }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
 
    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        Object serviceBean = handlerMap.get(className);
 
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
 
        // Method method = serviceClass.getMethod(methodName, parameterTypes);
        // method.setAccessible(true);
        // return method.invoke(serviceBean, parameters);
 
        FastClass serviceFastClass = FastClass.create(serviceClass);	
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
        return serviceFastMethod.invoke(serviceBean, parameters);
    }
 
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }
}