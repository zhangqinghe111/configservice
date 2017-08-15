package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;


/**
 * 心跳场景模拟
 * 
 * */

public class NodeDetectTest extends BaseTest {

	private NamingServiceClient client;
	
	private Set<String> nodes = new HashSet<String>();
	private String ip = "10.13.1.134";
	private int startPort = 6081;
	private int endPort = 6110;
	private String serviceTC;
	private String serviceYF;
	private String extinfo = "extinfo";

	@Before
	public void setUp() throws Exception {
		super.setUp();

		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		serviceTC = serviceId + "_tc";
		serviceYF = serviceId + "_yf";
		config.setServiceId(serviceId);
		config.setHeartbeatInterval(2000);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();

		nodes.add(ip);
		addService(serviceId, "dynamic");
		addCluster(serviceId, clusterId);
		addWhiteList(serviceId, nodes);

		VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
		ServerWebUtils.activeDetect("on");
		ServerWebUtils.dynamicDetect("on");
		ServerWebUtils.Heartbeat("off");
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
//		delWhiteList(serviceId, nodes);
//		delCluster(serviceId, clusterId);
//		delService(serviceId);
	}

	@Test
	public void testNodeStatusStatics() {
		try {
			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, ip, port, "");
			}

			System.out.print(VintageNamingClientUtils.currentNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			// 启动30个端口为200
			System.out.print("changing the status: all nodes 200.................");
			ServerWebUtils.Status200(10);
			ServerWebUtils.Status200(20);

			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			System.out.print(VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentWorkingNodes.size());

			// 启动71-10为404
			System.out
					.print("changing the status: 10 nodes 404.................");
			ServerWebUtils.kill(10);
			ServerWebUtils.Status404(10);
			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			assertEquals(10, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(20, VintageNamingClientUtils.currentWorkingNodes.size());

			// 启动all为404
			System.out
					.print("changing the status: all nodes 404.................");
			ServerWebUtils.kill(30);
			ServerWebUtils.Status404(10);
			ServerWebUtils.Status404(20);
			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: 10 nodes 503.................");
			ServerWebUtils.kill(10);
			ServerWebUtils.Status503(10);
			// 启动71-10为503
			assertEquals(20, VintageNamingClientUtils.currentNodes.size());
			assertEquals(20, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: all nodes 503.................");
			ServerWebUtils.kill(20);
			ServerWebUtils.Status503(20);
			// 启动all为503
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());
			System.out.print(VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: 10 nodes slow.................");
			ServerWebUtils.kill(10);
			ServerWebUtils.StatusSlow(10);

			assertEquals(10, VintageNamingClientUtils.currentNodes.size());
			assertEquals(10, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: all nodes slow.................");
			ServerWebUtils.kill(20);
			ServerWebUtils.StatusSlow(20);
			// 启动all为slow
			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: all nodes 503.................");
			ServerWebUtils.kill(30);
			ServerWebUtils.Status503(30);
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());
			System.out.print(VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: kill all......................");
			ServerWebUtils.kill(30);
			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			System.out.print(VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: all nodes unregister.................");
			// 全部取消注册
			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, ip, port);
			}
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out
					.print("changing the status: all nodes register again.................");
			// 全部重新注册
			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, ip, port, "");
			}
			assertEquals(30, VintageNamingClientUtils.currentNodes.size());
			assertEquals(30, VintageNamingClientUtils.currentUnreachableNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentWorkingNodes.size());

			System.out.print("changing the status: end.................");

		} catch (Exception ex) {
			// TODO: handle exception
			System.out.print(ex.getMessage());
			fail("Error in testNodeStatus");

		} finally {
			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, ip, port);
			}
			ServerWebUtils.kill(30);
		}
	}

	/**
	 * 三个service和cluster是否会同时探测
	 */
	@Test
	public void testNodeStatusTwoServie() {
		String serviceString = "hservice1";
		String clusterString = "abcluster1";
		String serviceString2 = "hservice2";
		String clusterString2 = "abcluster2";
		try {
			if (!VintageNamingWebUtils.existsService(serviceString)) {
				VintageNamingWebUtils.addService(serviceString, "dynamic");
			}
			if (!VintageNamingWebUtils.existCluster(serviceString, clusterString)) {
				VintageNamingWebUtils.addCluster(serviceString, clusterString);
			}
			VintageNamingWebUtils.addWhitelist(serviceString, nodes);
			VintageNamingClientUtils.subscribeNode(client, serviceString, clusterString);

			if (!VintageNamingWebUtils.existsService(serviceString2)) {
				VintageNamingWebUtils.addService(serviceString2, "dynamic");
			}
			if (!VintageNamingWebUtils.existCluster(serviceString2, clusterString2)) {
				VintageNamingWebUtils.addCluster(serviceString2, clusterString2);
			}
			VintageNamingWebUtils.addWhitelist(serviceString2, nodes);
			VintageNamingClientUtils.subscribeNode(client, serviceString2, clusterString2);

			// 第一个service
			for (int port = startPort; port <= startPort + 9; port++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, ip, port, "");
			}
			// 第二个service
			for (int port = startPort + 5; port <= startPort + 7; port++) {
				VintageNamingClientUtils.register(client, serviceString, clusterString, ip, port, "");
			}
			// 第三个service
			for (int port = startPort + 20; port <= startPort + 23; port++) {
				VintageNamingClientUtils.register(client, serviceString2, clusterString2, ip, port, "");
			}

			// TODO 自动改变节点状态 全部启动为200
			System.out.print("all 200.....................................");
			ServerWebUtils.Status200(30);

			assertEquals(
					4,
					VintageNamingClientUtils.nodeWorkingMap.get(
							serviceString2 + "_" + clusterString2).size());
			assertEquals(
					0,
					VintageNamingClientUtils.nodeUnreachableMap.get(
							serviceString2 + "_" + clusterString2).size());
			assertEquals(
					3,
					VintageNamingClientUtils.nodeWorkingMap.get(
							serviceString + "_" + clusterString).size());
			assertEquals(
					0,
					VintageNamingClientUtils.nodeUnreachableMap.get(
							serviceString + "_" + clusterString).size());
			assertEquals(10,
					VintageNamingClientUtils.nodeWorkingMap.get(serviceId + "_" + clusterId)
							.size());
			assertEquals(0,
					VintageNamingClientUtils.nodeUnreachableMap.get(serviceId + "_" + clusterId)
							.size());

			System.out.print("all kill.....................................");
			ServerWebUtils.kill(30);

			assertEquals(
					0,
					VintageNamingClientUtils.nodeWorkingMap.get(
							serviceString2 + "_" + clusterString2).size());
			assertEquals(
					4,
					VintageNamingClientUtils.nodeUnreachableMap.get(
							serviceString2 + "_" + clusterString2).size());
			assertEquals(
					0,
					VintageNamingClientUtils.nodeWorkingMap.get(
							serviceString + "_" + clusterString).size());
			assertEquals(
					3,
					VintageNamingClientUtils.nodeUnreachableMap.get(
							serviceString + "_" + clusterString).size());
			assertEquals(0,
					VintageNamingClientUtils.nodeWorkingMap.get(serviceId + "_" + clusterId)
							.size());
			assertEquals(10,
					VintageNamingClientUtils.nodeUnreachableMap.get(serviceId + "_" + clusterId)
							.size());

			System.out.print("10 200.....................................");
			ServerWebUtils.Status200(10);
			assertEquals(
					0,
					VintageNamingClientUtils.nodeWorkingMap.get(
							serviceString2 + "_" + clusterString2).size());
			assertEquals(
					4,
					VintageNamingClientUtils.nodeUnreachableMap.get(
							serviceString2 + "_" + clusterString2).size());
			assertEquals(
					3,
					VintageNamingClientUtils.nodeWorkingMap.get(
							serviceString + "_" + clusterString).size());
			assertEquals(
					0,
					VintageNamingClientUtils.nodeUnreachableMap.get(
							serviceString + "_" + clusterString).size());
			assertEquals(10,
					VintageNamingClientUtils.nodeWorkingMap.get(serviceId + "_" + clusterId)
							.size());
			assertEquals(0,
					VintageNamingClientUtils.nodeUnreachableMap.get(serviceId + "_" + clusterId)
							.size());

			ServerWebUtils.kill(10);

		} catch (Exception e) {
			// TODO: handle exception
			System.out.print(e.getMessage());
			fail("Error in testNodeStatusTwoServie");
		} finally {
			for (int port = startPort; port <= startPort + 9; port++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, ip, port);
			}
			for (int port = startPort + 5; port <= startPort + 7; port++) {
				VintageNamingClientUtils.unregister(client, serviceString, clusterString, ip, port);
			}
			
			for (int port = startPort + 20; port <= startPort + 23; port++) {
				VintageNamingClientUtils.unregister(client, serviceString2, clusterString2, ip, port);
			}
			VintageNamingWebUtils.deleteWhitelist(serviceString, nodes);
			VintageNamingWebUtils.deleteCluster(serviceString, clusterString);
			VintageNamingWebUtils.deleteService(serviceString);
			VintageNamingWebUtils.deleteWhitelist(serviceString2, nodes);
			VintageNamingWebUtils.deleteCluster(serviceString2, clusterString2);
			VintageNamingWebUtils.deleteService(serviceString2);

			ServerWebUtils.kill(30);
		}
	}

	/*
	 * test the service type:dynamic to statics
	 */
	@Test
	public void testServiceType() {
		try {
			ServerWebUtils.kill(30);

			serviceId = serviceId + 2;
			String serviceKey = serviceId + "_" + clusterId;
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				System.out.print("add statics service.................");
				VintageNamingWebUtils.addService(serviceId);
			}
			if (!VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId);
			}

			VintageNamingWebUtils.addWhitelist(serviceId, nodes);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);

			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, ip, port, "extinfo");
			}

			System.out.print("register end.................");
			assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			ServerWebUtils.Status503(30);
			assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			ServerWebUtils.kill(30);
			assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("update: statics to dynamic...");
			VintageNamingWebUtils.updateService(serviceId, "dynamic");
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("200...");
			ServerWebUtils.Status200(30);
			assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("503...");
			ServerWebUtils.kill(30);
			ServerWebUtils.Status503(30);
			assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("kill...");
			ServerWebUtils.kill(30);
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("update: dynamic to statics...");
			VintageNamingWebUtils.updateService(serviceId, "statics");

			System.out.print("200...");
			ServerWebUtils.Status200(30);
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("kill...");
			ServerWebUtils.kill(30);
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("503...");
			ServerWebUtils.Status503(30);
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			System.out.print("end..........................");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ServerWebUtils.kill(30);
			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, ip, port);
			}
			VintageNamingWebUtils.deleteWhitelist(serviceId, nodes);
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			VintageNamingWebUtils.deleteService(serviceId);
			serviceId = "hqtestservice";
		}
	}

	@Test
	public void testNodeStatusMultiCluster() {
		try {
			for (int i = 0; i < 5; i++) {
				if (!VintageNamingWebUtils.existCluster(serviceId, clusterId + i)) {
					VintageNamingWebUtils.addCluster(serviceId, clusterId + i);
					VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);
				}
			}

			for (int i = 0; i < 5; i++) {
				for (int port = startPort; port <= startPort + 9; port++) {
					VintageNamingClientUtils.register(client, serviceId, clusterId + i, ip, port, "extinfo");
				}
			}
			for (int i = 0; i < 5; i++) {
				String servicekeyString = serviceId + "_" + clusterId + i;
				assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(servicekeyString).size());
				assertEquals(10, VintageNamingClientUtils.nodeUnreachableMap.get(servicekeyString).size());
			}

			ServerWebUtils.Status200(10);
			for (int i = 0; i < 5; i++) {
				String servicekeyString = serviceId + "_" + clusterId + i;
				assertEquals(10, VintageNamingClientUtils.nodeWorkingMap.get(servicekeyString)
						.size());
				assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(servicekeyString)
						.size());
			}

			ServerWebUtils.kill(10);
			for (int i = 5; i < 10; i++) {
				if (!VintageNamingWebUtils.existCluster(serviceId, clusterId + i)) {
					VintageNamingWebUtils.addCluster(serviceId, clusterId + i);
					VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);
				}
			}

			for (int i = 5; i < 10; i++) {
				for (int port = startPort; port <= startPort + 9; port++) {
					VintageNamingClientUtils.register(client, serviceId, clusterId + i, ip, port,
							"extinfo");
				}
			}

			for (int i = 0; i < 10; i++) {
				String servicekeyString = serviceId + "_" + clusterId + i;
				assertEquals(0, VintageNamingClientUtils.nodeWorkingMap.get(servicekeyString)
						.size());
				assertEquals(10, VintageNamingClientUtils.nodeUnreachableMap.get(servicekeyString)
						.size());
			}

			ServerWebUtils.Status200(10);
			for (int i = 0; i < 10; i++) {
				String servicekeyString = serviceId + "_" + clusterId + i;
				System.out.print(servicekeyString);
				assertEquals(10, VintageNamingClientUtils.nodeWorkingMap.get(servicekeyString)
						.size());
				assertEquals(0, VintageNamingClientUtils.nodeUnreachableMap.get(servicekeyString)
						.size());
			}

		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			ServerWebUtils.kill(10);
			for (int i = 0; i < 10; i++) {
				for (int port = startPort; port <= startPort + 9; port++) {
					VintageNamingClientUtils.unregister(client, serviceId, clusterId + i, ip, port);
				}
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId + i);
			}
		}
	}

	@Test
	public void testSpecialService() {
		try {
			ServerWebUtils.dynamicDetect("off"); // close the switch,use
														// 8080 and 8090 default
			ServerWebUtils.kill(30);

			VintageNamingWebUtils.addService(serviceTC, "dynamic");
			VintageNamingWebUtils.addCluster(serviceTC, clusterId);
			VintageNamingWebUtils.addWhitelist(serviceTC, nodes);
			VintageNamingClientUtils.subscribeNode(client, serviceTC, clusterId);

			VintageNamingWebUtils.addService(serviceYF, "dynamic");
			VintageNamingWebUtils.addCluster(serviceYF, clusterId);
			VintageNamingWebUtils.addWhitelist(serviceYF, nodes);
			VintageNamingClientUtils.subscribeNode(client, serviceYF, clusterId);

			String serviceKeyTC = serviceTC + "_" + clusterId;
			String serviceKeyYF = serviceYF + "_" + clusterId;
			String serviceKey = serviceId + "_" + clusterId;

			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.register(client, serviceTC, clusterId, ip, port, extinfo);
				VintageNamingClientUtils.register(client, serviceYF, clusterId, ip, port, extinfo);
				VintageNamingClientUtils.register(client, serviceId, clusterId, ip, port, extinfo);
			}

			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKeyTC).size());
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKeyYF).size());
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());

			ServerWebUtils.Status8080();
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKeyTC).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKeyYF).size());
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());

			ServerWebUtils.Status8090();
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKeyTC).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKeyYF).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());

			ServerWebUtils.kill(30);
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKeyTC).size());
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKeyYF).size());
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKey).size());

			ServerWebUtils.Status8090();
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKeyTC).size());
			assertEquals(30, VintageNamingClientUtils.nodeUnreachableMap.get(serviceKeyYF).size());
			assertEquals(30, VintageNamingClientUtils.nodeWorkingMap.get(serviceKey).size());
		} finally {
			ServerWebUtils.kill(30);

			for (int port = startPort; port <= endPort; port++) {
				VintageNamingClientUtils.unregister(client, serviceTC, clusterId, ip, port);
				VintageNamingClientUtils.unregister(client, serviceYF, clusterId, ip, port);
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, ip, port);
			}

			VintageNamingWebUtils.deleteWhitelist(serviceYF, nodes);
			VintageNamingWebUtils.deleteWhitelist(serviceTC, nodes);

			VintageNamingWebUtils.deleteCluster(serviceTC, clusterId);
			VintageNamingWebUtils.deleteCluster(serviceYF, clusterId);

			VintageNamingWebUtils.deleteService(serviceTC);
			VintageNamingWebUtils.deleteService(serviceYF);
		}

	}

	/*
	 * test detect strategy
	 */
	@Test
	public void testClientDetectStrategy() {
		int keysize = 20;
		try {
			for (int port = startPort; port <= startPort + keysize; port++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, ip, port, "");
			}

			client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
			Set<NamingServiceNode> nodeSet = client
					.lookup(serviceId, clusterId);
			assertEquals(21, nodeSet.size());

			client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(0, nodeSet.size());

			client.setNodeExciseStrategy(new NodeExciseStrategy.Ratio(0));
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(0, nodeSet.size());

			client.setNodeExciseStrategy(new NodeExciseStrategy.Ratio(50));
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(10, nodeSet.size());

			// the borderline
			client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
			ServerWebUtils.Status200(10);
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(21, nodeSet.size());
			
			client.setNodeExciseStrategy(new NodeExciseStrategy.Ratio(50));
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(10, nodeSet.size());

			client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(10, nodeSet.size());		
			
			client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
			ServerWebUtils.kill(10);
			ServerWebUtils.Status200(30);
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(21, nodeSet.size());

			client.setNodeExciseStrategy(new NodeExciseStrategy.Ratio(50));
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(21, nodeSet.size());
			
			client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
			nodeSet = client.lookup(serviceId, clusterId);
			assertEquals(21, nodeSet.size());
		} finally {
			ServerWebUtils.kill(30);
			for (int port = startPort; port <= startPort + keysize; port++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, ip, port);
			}
		}
	}
}
