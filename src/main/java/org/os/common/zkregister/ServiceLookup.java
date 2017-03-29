package org.os.common.zkregister;

import static org.os.common.zkregister.constant.Constant.ZK_ADDRESS;
import static org.os.common.zkregister.constant.Constant.ZK_SESSION_TIMEOUT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.os.common.zkregister.constant.Constant;
import org.os.common.zkregister.utils.ConfigInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务获取
 * @author chenwei 20170301
 *
 */
public abstract class ServiceLookup {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLookup.class);
	private String registryAddress = ConfigInfo.getConfig(ZK_ADDRESS.name());
	private int sessionTimeout = Integer.parseInt(ConfigInfo.getConfig(ZK_SESSION_TIMEOUT.name()));

	private CountDownLatch latch = new CountDownLatch(1);
	private volatile List<String> urlList = new ArrayList<String>();
	private String servicePath;

	/**
	 * 构造器
	 * @param serviceName zk目录（例：/webservice/order……）
	 */
	public ServiceLookup(String servicePath) {
		this.servicePath = servicePath;
		ZooKeeper zk = connectServer(); // 连接 ZooKeeper 服务器并获取 ZooKeeper 对象
		if (zk != null) {
			watchNode(zk); // 观察 /registry 节点的所有子节点并更新 urlList 成员变量
		}
	}

	/**
	 * 设置 zk 服务器地址
	 * @param registryAddress （例：127.0.0.1:2181）
	 */
	public void setRegistryAddress(String registryAddress) {
		this.registryAddress = registryAddress;
	}

	/**
	 * 设置超时时间毫秒
	 * @param sessionTimeout
	 */
	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	protected abstract <T> T lookupService(String url, Class<T> clazz);

	/**
	 * 获取服务地址
	 * @return
	 */
	public String lookup() {
		int size = urlList.size();
		String url = null;
		if (size > 0) {
			if (size == 1) {
				url = urlList.get(0);
			} else {
				url = urlList.get(new Random().nextInt(size));
			}
		}
		return url;
	}
	
	/**
	 * 获取服务对象
	 * @param clazz 对象类
	 * @return
	 */
	public <T> T lookup(Class<T> clazz) {
		T service = null;
		String url = lookup();
		System.out.println(url);
		if (url != null) {
			service = lookupService(url, clazz);
		}
		return service;
	}
	
	/**连接服务*/
	private ZooKeeper connectServer() {
		ZooKeeper zk = null;
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
		} catch (IOException | InterruptedException e) {
			LOGGER.error("", e);
		}
		return zk;
	}

	/**
	 * 启动时关注节点
	 * @param zk
	 */
	private void watchNode(ZooKeeper zk) {
		String rootPath = ConfigInfo.getConfig(Constant.ZK_PATH_ROOT.name());
		String pathAll = getZkDataPath(rootPath, servicePath);
		try {
			List<String> nodeList = zk.getChildren(pathAll, new Watcher() {

				@Override
				public void process(WatchedEvent arg0) {
					if (arg0.getType() == Event.EventType.NodeChildrenChanged) {
						watchNode(zk);
					}
				}
			});

			List<String> dataList = new ArrayList<>();
			for (String node : nodeList) {
				byte[] data = zk.getData(pathAll + "/" + node, false, null);
				dataList.add(new String(data));
			}
			urlList = dataList;
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
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
		return path.toString();
	}
}
