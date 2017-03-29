package org.os.common.zkregister.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.os.common.zkregister.ServiceRegistry;
import org.os.common.zkregister.rpc.RpcDecoder;
import org.os.common.zkregister.rpc.RpcEncoder;
import org.os.common.zkregister.rpc.RpcRequest;
import org.os.common.zkregister.rpc.RpcResponse;
import org.os.common.zkregister.service.rpchandler.RpcServiceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RpcRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcRegistry.class);
	
	static EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	static EventLoopGroup workerGroup = new NioEventLoopGroup();
	static Map<String, RpcServiceHandler> rpcHandlerMap = new HashMap<String, RpcServiceHandler>();
	static Set<String> serverSet = new HashSet<String>();

	private RpcRegistry(){
	}

//	public static String publishService(Object serviceObject, String host, int port) throws Exception {
//		verifyStartServer(host, port);
//		String canonicalName = serviceObject.getClass().getInterfaces()[0].getCanonicalName();
//		rpcHandlerMap.get(host + port).putServiceObject(canonicalName, serviceObject);
//		String url = String.format("%s:%d", host, port);
//		return url;
//	}
	
	public static void publishService(Object serviceObject, String host, int port, ServiceRegistry serviceRegistry, String servicePath) throws Exception {
		try{
			RpcServiceHandler rpcHandler = new RpcServiceHandler();
			String canonicalName = serviceObject.getClass().getInterfaces()[0].getCanonicalName();
			rpcHandler.putServiceObject(canonicalName, serviceObject);
			
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline()
					.addLast(new RpcDecoder(RpcRequest.class))
					.addLast(new RpcEncoder(RpcResponse.class))
					.addLast(rpcHandler);
				}
			});

			ChannelFuture future = bootstrap.bind(host, port).sync();
			LOGGER.debug("server started on port {}", port);
			
			serviceRegistry.register(host+":"+port, servicePath);

			future.channel().closeFuture().sync();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	private static void verifyStartServer(String host, int port) throws Exception {
		if (!serverSet.contains(host + port)) {
			synchronized (RpcRegistry.class) {
				if (!serverSet.contains(host + port)) {
					CountDownLatch latch = new CountDownLatch(1);
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try{
								RpcServiceHandler rpcHandler = new RpcServiceHandler();
								
								ServerBootstrap bootstrap = new ServerBootstrap();
								bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
								.childHandler(new ChannelInitializer<SocketChannel>() {

									@Override
									protected void initChannel(SocketChannel ch) throws Exception {
										ch.pipeline()
										.addLast(new RpcDecoder(RpcRequest.class))
										.addLast(new RpcEncoder(RpcResponse.class))
										.addLast(rpcHandler);
									}
								});

								ChannelFuture future = bootstrap.bind(host, port).sync();
								LOGGER.debug("server started on port {}", port);

								serverSet.add(host + port);
								rpcHandlerMap.put(host + port, rpcHandler);
								latch.countDown();
								
								future.channel().closeFuture().sync();
								
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								workerGroup.shutdownGracefully();
								bossGroup.shutdownGracefully();
								latch.countDown();
							}
						}
					}).start();
					latch.await();
				}
			}
		}
	}

}
