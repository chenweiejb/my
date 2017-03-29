package org.os.common.zkregister.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import org.os.common.zkregister.ServiceLookup;
import org.os.common.zkregister.client.rpchandler.RpcClientHandler;
import org.os.common.zkregister.rpc.RpcRequest;
import org.os.common.zkregister.rpc.RpcResponse;

public class RpcLookup extends ServiceLookup {

	public RpcLookup(String servicePath) {
		super(servicePath);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T lookupService(String url, Class<T> clazz) {
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				RpcRequest request = new RpcRequest(); // 创建并初始化 RPC 请求
				request.setRequestId(UUID.randomUUID().toString());
				request.setClassName(method.getDeclaringClass().getName());
				request.setMethodName(method.getName());
				request.setParameterTypes(method.getParameterTypes());
				request.setParameters(args);

				String[] array = url.split(":");
				String host = array[0];
				int port = Integer.parseInt(array[1]);

				RpcClientHandler client = new RpcClientHandler(host, port); // 初始化 RPC 客户端
				RpcResponse response = client.send(request); // 通过 RPC 客户端发送 RPC 请求并获取 RPC 响应

				if (response.getError() != null) {
					throw response.getError();
				} else {
					return response.getResult();
				}
			}
		});
	}

}
