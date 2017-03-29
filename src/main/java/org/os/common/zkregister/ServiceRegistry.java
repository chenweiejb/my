package org.os.common.zkregister;

import static org.os.common.zkregister.constant.Constant.ZK_ADDRESS;
import static org.os.common.zkregister.constant.Constant.ZK_SESSION_TIMEOUT;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.os.common.zkregister.constant.Constant;
import org.os.common.zkregister.utils.ConfigInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务注册
 * @author chenwei 20170301
 *
 */
public class ServiceRegistry {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);
	private String registryAddress = ConfigInfo.getConfig(ZK_ADDRESS.name());
	private int sessionTimeout = Integer.parseInt(ConfigInfo.getConfig(ZK_SESSION_TIMEOUT.name()));

	private CountDownLatch latch = new CountDownLatch(1);
	private static ZooKeeper zk = null;
	private static ServiceRegistry serviceRegistry = null;
	
	private ServiceRegistry() {
		connectServer();
	}
	
	/**
	 * 获取服务对象
	 * @return
	 */
	public static ServiceRegistry getServiceRegistry() {
		if (serviceRegistry == null) {
			synchronized (ServiceRegistry.class) {
				if (serviceRegistry == null) {
					serviceRegistry = new ServiceRegistry();
				}
			}
		}
		return serviceRegistry;
	}
	
	/**
	 * 注册服务（需要在配置文件中设置根目录 ZK_PATH_ROOT）
	 * @param url 服务地址
	 * @param servicePath zk目录（例：/webservice/order……）需要事先存在zk服务器中
	 * @throws Exception
	 */
	public void register(String url, String servicePath) throws Exception {
		if (zk == null) {
			throw new RuntimeException("zk服务器连接失败");
		}
		String rootPath = ConfigInfo.getConfig(Constant.ZK_PATH_ROOT.name());
		String zkDataPath = getZkDataPath(rootPath, servicePath);
		createNode(zk, zkDataPath, url);
	}
	
	/**连接服务*/
	private void connectServer() {
		try {
			zk = new ZooKeeper(registryAddress, sessionTimeout, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					// 判断是否已连接ZK,连接后计数器递减.
					if (event.getState() == Event.KeeperState.SyncConnected) {
						latch.countDown();
					}
				}
			});

			// 若计数器不为0,则等待.
			latch.await();
			LOGGER.info("zk服务连接成功");
		} catch (IOException | InterruptedException e) {
			LOGGER.error("", e);
		}
	}
	
	/**
	 * 创建节点，保存数据
	 * @param zk
	 * @param zkDataPath 节点路径
	 * @param data 数据
	 * @throws Exception 
	 */
	private void createNode(ZooKeeper zk, String zkDataPath, String data) throws Exception {
		byte[] bytes = data.getBytes();
		String path = zk.create(zkDataPath, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		LOGGER.debug("create zookeeper node ({} => {})", path, data);
	}
	
	/**
	 * 获取zk目录完整路径，最后增加“node”节点，该节点自增用
	 * @param nodes 多个node拼成字符串
	 * @return
	 */
	private String getZkDataPath(String... nodes) {
		StringBuffer path = new StringBuffer();
		for (String node : nodes) {
			path.append(node);
		}
		path.append("/node");
		return path.toString();
	}
}
