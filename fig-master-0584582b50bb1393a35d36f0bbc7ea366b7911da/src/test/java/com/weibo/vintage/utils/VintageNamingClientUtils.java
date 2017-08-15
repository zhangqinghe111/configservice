package com.weibo.vintage.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import redis.clients.jedis.Jedis;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.listener.NamingServiceChangeListener;
import com.weibo.vintage.model.EndpointAddress;
import com.weibo.vintage.model.NamingServiceCluster;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.model.NotifyAction;

/**
 * 此类为测试用例的工具类
 * 
 * @author lingling6
 * 
 */
public class VintageNamingClientUtils {
	public static String[] Invalid_String = { ";", "&", "%", "#", "$", "@",
			",", "*", "^", "~", "(", ")", "\\", "|", "+", "[", "]", "{", "}",
			"-", "<", ">", "?", "a", "10.10.10.a", "a.a.a.a", "10.a.a.10",
			"10.10.10"};

	// for smart naming service
	public static String[] InvalidPrirotyStrings = new String[] { "high_yf",
			"test_test", "test_test_", "test_yf_", "test_test_high_yf",
			"test_yf_high", "test_test_test" };
	public static String[] InvalidBusinessStrings = new String[] { "_high_yf",
			"_test_high", "_test", "_high" };

	private static NamingServiceChangeListenerTest listener = new NamingServiceChangeListenerTest();

	// 此变量为心跳线程获取节点变更时的节点信息。
	public static Set<NamingServiceNode> currentNodes = new HashSet<NamingServiceNode>();
	public static Set<NamingServiceNode> currentWorkingNodes = new HashSet<NamingServiceNode>();
	public static Set<NamingServiceNode> currentUnreachableNodes = new HashSet<NamingServiceNode>();

	public static HashMap<String, Set<NamingServiceNode>> nodeMap = new HashMap<String, Set<NamingServiceNode>>();
	public static HashMap<String, Set<NamingServiceNode>> nodeWorkingMap = new HashMap<String, Set<NamingServiceNode>>();
	public static HashMap<String, Set<NamingServiceNode>> nodeUnreachableMap = new HashMap<String, Set<NamingServiceNode>>();

	public static Jedis redis = new Jedis(VintageConstantsTest.REDIS_IP, VintageConstantsTest.REDIS_PORT);

	static class NamingServiceChangeListenerTest implements
			NamingServiceChangeListener {
		/**
		 * client监听事件 
		 * notifyAction：变更通知行为 
		 * immediately：即时的推送行为,比如zk有变更时即时通知给订阅方
		 * heartbeat：通过HeartBeat校验到的更新行为
		 */
		public void handleNodeChange(String service, String cluster,
				Set<NamingServiceNode> nodes, NotifyAction notifyAction)
				throws Exception {
			Set<NamingServiceNode> workingNodes = new HashSet<NamingServiceNode>();
			Set<NamingServiceNode> unreachableNodes = new HashSet<NamingServiceNode>();

			// 打印节点变化的service、cluster信息
			VintageTestLogger.info("\n" + "serviceId: " + service + "  clusterId: "
					+ cluster + "   Node Change" + "\n");

			// currentNodes = nodes;

			for (NamingServiceNode node : nodes) {
				// 每一个变化节点的详细信息
//				VintageTestLogger.info(node.getNodeStatus() + "  "
//						+ node.getAddress() + "   " + node.getExtInfo());
				switch (node.getNodeStatus()) {
				case working:
					workingNodes.add(node);
					break;
				case unreachable:
					unreachableNodes.add(node);
					break;
				default:
					break;
				}
			}
			VintageTestLogger.info("total nodes: " + nodes.size());

			// 为验证节点信息赋值
			currentNodes = nodes;
			currentWorkingNodes = workingNodes;
			currentUnreachableNodes = unreachableNodes;
			String mapKey = service + "_" + cluster;
			nodeMap.put(mapKey, nodes);
			nodeWorkingMap.put(mapKey, workingNodes);
			nodeUnreachableMap.put(mapKey, unreachableNodes);

			// VintageTestLogger.info(mapKey);
			VintageTestLogger.info(nodes.toString());
		}

		public Executor getExecutor() {
			return null;
		}
	}

	/**
	 * client 订阅 clusterId 的节点变更
	 * 
	 * @param client
	 * @param clusterId
	 */
	public static void subscribeNode(NamingServiceClient client,
			String clusterId) {
		client.subscribeNodeChanges(clusterId, listener);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// serviceId 与 clusterId 为多对多关系
	/**
	 * client 订阅 clusterId 的 serviceId 节点变更
	 * 
	 * @param client
	 * @param serviceId
	 * @param clusterId
	 */
	public static void subscribeNode(NamingServiceClient client,
			String serviceId, String clusterId) {
		client.subscribeNodeChanges(serviceId, clusterId, listener);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * client 取消订阅 clusterId 节点变更
	 * 
	 * @param client
	 * @param clusterId
	 */
	public static void unsubscribeChange(NamingServiceClient client,
			String clusterId) {
		client.unsubscribeNodeChanges(clusterId, listener);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * client 订阅 clusterId 中的 serviceId节点变更
	 * 
	 * @param client
	 * @param serviceId
	 * @param clusterId
	 */
	public static void unsubscribeChange(NamingServiceClient client,
			String serviceId, String clusterId) {
		client.unsubscribeNodeChanges(serviceId, clusterId, listener);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 取消全部订阅
	 * 
	 * @param client
	 * @param serviceId
	 * @param clusterId
	 */
	public static void unsubscribeAllChanges(NamingServiceClient client,
			String serviceId, String clusterId) {
		client.unsubscribeNodeChanges(serviceId, clusterId, listener);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 取消订阅某集群的节点变更
	 * 
	 * @param client
	 * @param clusterId
	 */
	public static void unsubscribeAllChange(NamingServiceClient client,
			String clusterId) {

		client.unsubscribeAllNodeChanges(clusterId);
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 取消所有节点变更
	 * 
	 * @param client
	 */
	public static void unsubscribeAllChange(NamingServiceClient client) {
		client.unsubscribeAllNodeChanges();
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 获取 clusterId 的 service列表
	 * 
	 * @param client
	 * @param serviceId
	 * @param clusterId
	 * @return
	 */
	public static Set<NamingServiceNode> lookup(NamingServiceClient client,
			String serviceId, String clusterId) {
		//VintageTestLogger.info(serviceId+" "+clusterId);
		return client.lookup(serviceId, clusterId);
	}

	/**
	 * 获取集群中所有服务列表
	 * 
	 * @param client
	 * @param clusterId
	 * @return
	 */
	public static Set<NamingServiceNode> lookup(NamingServiceClient client,
			String clusterId) {
		return client.lookup(clusterId);
	}

	/**
	 * 获取working状态节点列表　－－－－　用于client主动汇报节点状态功能
	 * 
	 * @param nodeList
	 * @return
	 */
	public static Set<NamingServiceNode> getWorkingNodeList(
			NamingServiceClient client, String serviceId, String clusterId) {

		Set<NamingServiceNode> nodeList = lookup(client, serviceId, clusterId);
		Set<NamingServiceNode> wNodes = new HashSet<NamingServiceNode>();

		for (NamingServiceNode node : nodeList) {
			if (node.getNodeStatus().toString().equalsIgnoreCase("working")) {
				wNodes.add(node);
			}
		}

		return wNodes;

	}

	/**
	 * 获取unreachable状态节点列表　－－－－　用于client主动汇报节点状态功能
	 * 
	 * @param nodeList
	 * @return
	 */
	public static Set<NamingServiceNode> getUnreachableNodeList(
			NamingServiceClient client, String serviceId, String clusterId) {
		Set<NamingServiceNode> nodeList = lookup(client, serviceId, clusterId);
		Set<NamingServiceNode> uNodes = new HashSet<NamingServiceNode>();

		for (NamingServiceNode node : nodeList) {
			if (node.getNodeStatus().toString().equalsIgnoreCase("unreachable")) {
				uNodes.add(node);
			}
		}
		return uNodes;
	}

	/**
	 * 注册服务到 cluster
	 * 
	 * @param client
	 * @param clusterId
	 * @param ip
	 * @param port
	 */
	public static void register(NamingServiceClient client, String clusterId,
			String ip, int port) {
		NamingServiceNode node = new NamingServiceNode(new EndpointAddress(ip,
				port));
		node.setExtInfo(" ");
		client.register(clusterId, node);
		// VintageTestLogger.info(client.register(clusterId, node));
	
	}

	/**
	 * 带有 extInfo的节点注册到cluster
	 * 
	 * @param client
	 * @param clusterId
	 * @param ip
	 * @param port
	 * @param extinfo
	 */
	public static void register(NamingServiceClient client, String clusterId,
			String ip, int port, String extinfo) {
		NamingServiceNode node = new NamingServiceNode(new EndpointAddress(ip,
				port));
		node.setExtInfo(extinfo);
		client.register(clusterId, node);
	}

	/**
	 * 服务注册到 service
	 * 
	 * @param client
	 * @param serviceId
	 * @param clusterId
	 * @param ip
	 * @param port
	 * @param extinfo
	 */
	public static void register(NamingServiceClient client, String serviceId,
			String clusterId, String ip, int port) {
		String extinfo = "memcache%3A%2F%2F"+ip+"%3A"+port+
				"%2Fcn.sina.api.commons.cache.MemcacheClient%3Fgroup%3D"+
				serviceId+"%26nodeType%3Dservice%26";
		NamingServiceNode node = new NamingServiceNode(new EndpointAddress(ip,
				port));
		node.setExtInfo(extinfo);
		client.register(serviceId, clusterId, node);
	}
	
	public static void register(NamingServiceClient client, String serviceId,
			String clusterId, String ip, int port, String extinfo) {
		NamingServiceNode node = new NamingServiceNode(new EndpointAddress(ip,
				port));
		node.setExtInfo(extinfo);
		client.register(serviceId, clusterId, node);
	}
	
//	public static String register(String serviceId, String clusterId, String ip, int port) {
//		return VintageNamingWebUtils.register(serviceId, clusterId, ip, port);
//	}

	/**
	 * 取消注册 cluster, 无 service为默认service??
	 * 
	 * @param client
	 * @param clusterId
	 * @param ip
	 * @param port
	 */
	public static void unregister(NamingServiceClient client, String clusterId,
			String ip, int port) {
		client.unregister(clusterId, new NamingServiceNode(new EndpointAddress(
				ip, port)));
	}

	/**
	 * 取消注册 cluster service
	 * 
	 * @param client
	 * @param serviceId
	 * @param clusterId
	 * @param ip
	 * @param port
	 */
	public static void unregister(NamingServiceClient client, String serviceId,
			String clusterId, String ip, int port) {
		NamingServiceNode node = new NamingServiceNode(new EndpointAddress(ip,
				port));
		client.unregister(serviceId, clusterId, node);
	}

	/**
	 * 设置节点摘除策略为保留最低比例
	 * 
	 * @param client
	 */
	public static void setRatioStrategy(NamingServiceClient client) {
		client.setNodeExciseStrategy(new NodeExciseStrategy.Ratio(50));
	}

	/**
	 * 设置节点摘除策略为静态方式
	 * 
	 * @param client
	 */
	public static void setStaticsStrategy(NamingServiceClient client) {
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
	}

	/**
	 * 设置
	 * 
	 * @param client
	 */
	public static void setDynamicStrategy(NamingServiceClient client) {
		client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
	}

	public static void sleep(int seconds) {
		try {
			Thread.sleep(seconds);
		} catch (InterruptedException e) {
			return;
		}
	}

	public static Set<NamingServiceNode> getSubWorkingNodes(
			NamingServiceClient client, String serviceId, String clusterId) {
		
		Set<NamingServiceNode> wNodes = new HashSet<NamingServiceNode>();

		Map map = new HashMap();
		map = VintageNamingClientUtils.nodeWorkingMap;
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			VintageTestLogger.info("***************");
			VintageTestLogger.info(entry.getKey().toString());
			VintageTestLogger.info("***************");
			if (entry.getKey().toString()
					.equalsIgnoreCase(serviceId + "_" + clusterId)) {
				wNodes = (Set<NamingServiceNode>) entry.getValue();
			}
		}

		return wNodes;

	}
}
