package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.naming.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class ClientNamingUnsubscribeAllTest extends BaseTest {

	private NamingServiceClient client;
	private String serviceKey = "";
	private Set<NamingServiceNode> services = new HashSet();
	private Set<NamingServiceNode> nodes;
	private String type = "dynamic";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		
		config.setServiceId(serviceId);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();
		addService(serviceId, type);
		VintageNamingClientUtils.sleep(serviceCacheInterval);
		addCluster(serviceId, clusterId);
		addWhiteList(serviceId, localNodes);		
		serviceKey = serviceId + "_" + clusterId;

	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

	}

	/**
	 * 前提：没有注册node到节点到cluster && 未订阅cluster 验证：变更
	 */
	@Test
	public void testNoNode() {
		try {			
			VintageNamingClientUtils.unsubscribeAllChange(client);
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testNoNode");
		}
	}

	/**
	 * 测试点：unsubscribe after subscribe 验证：变更
	 */
	@Test
	public void testUnsubAfterSub() {
		try {
			// 向cluster中注册1个节点
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId,clusterId, localIP, 1234, "ext");
			Thread.sleep(3 * HEARTBEATINTERVAL);
			// check
			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			// unsubscribeAll
			VintageNamingClientUtils.unsubscribeAllChange(client, clusterId);

			// 在取消订阅后，注册节点，则无法收到变更
			VintageNamingClientUtils.register(client, clusterId, localIP, 1235, "ex");
			Thread.sleep(HEARTBEATINTERVAL);

			// check
			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			assertEquals(2, client.lookup(clusterId).size());

		} catch (Exception e) {
			fail("error in testUnsubAfterSub");
		} finally {
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1235);
		}
	}

	/**
	 * 测试点：subscribe 10 clusters --> unsubscribeAll 验证：变更
	 */
	@Test
	public void testMultiClusters() {
		try {
			// subscribe 10 clusters
			for (int i = 0; i < 10; i++) {
				if (!VintageNamingWebUtils.existCluster(serviceId, clusterId + i)) {
					VintageNamingWebUtils.addCluster(serviceId, clusterId + i);
				}
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);
			}

			// 取消clusterId0的所有节点变更
			VintageNamingClientUtils.unsubscribeAllChange(client, clusterId + 0);

			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			// 注册节点信息 10 个
			for (int i = 1200; i < 1210; i++) {
				for (int num = 0; num < 10; num++) {
					VintageNamingClientUtils.register(client, serviceId, clusterId + num, localIP,
							i, "ex");
				}
			}
			Thread.sleep(serviceCacheInterval);
			// nodeMap中有clusterId1~cluster9的信息
			System.out.print(VintageNamingClientUtils.nodeMap.size());
			assertEquals(9, VintageNamingClientUtils.nodeMap.size());			
			
			// 验证 nodeMap中每个集群的节点数目为 10个
			for (int i = 1; i < 10; i++) {
				assertEquals(10,
						VintageNamingClientUtils.nodeMap.get(serviceId + "_" + clusterId + i)
								.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testMultiCluster");
		} finally {
			VintageNamingClientUtils.unsubscribeAllChange(client);

			System.out.println(VintageNamingWebUtils.getCluster(serviceId));

			for (int i = 1200; i < 1210; i++) {
				for (int num = 0; num < 10; num++) {
					VintageNamingClientUtils.unregister(client, clusterId + num, localIP, i);
				}
			}

			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId + i);
			}
		}
	}
	
	/**
	 * 测试点：subscribe 100 clusters --> unsubscribeAll 验证：变更
	 */
	@Test
	public void testUnsub() {
		try {
			// 订阅 100 cluster
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.subscribeNode(client, clusterId + i);
			}

			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			// 取消订阅所有cluster
			VintageNamingClientUtils.unsubscribeAllChange(client);

			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			// 注册 10 个结点到 10个 cluster
			for (int i = 1200; i < 1210; i++) {
				for (int num = 0; num < 10; num++) {
					addCluster(serviceId, clusterId+num);
					VintageNamingClientUtils.register(client, serviceId, clusterId + num, localIP, i, "ex");
				}
			}

			Thread.sleep(serviceCacheInterval);

			assertEquals(0, VintageNamingClientUtils.nodeMap.size());

			for (int i = 0; i < 10; i++) {
				assertEquals(10, client.lookup(clusterId + i).size());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testUnsub");
		} finally {
			for (int i = 1200; i < 1210; i++) {
				for (int num = 0; num < 10; num++) {
					VintageNamingClientUtils.unregister(client, serviceId, clusterId + num,
							localIP, i);
				}
			}
			for (int j = 0; j < 10; j++) {
				delCluster(serviceId, clusterId+j);
			}
		}
	}

	/**
	 * 测试点：unsubscribeAll -- service cluster
	 */
	@Test
	public void testUnsubServiceCluster() {
		String serString ="service2";
		String cluString = "cluster";
		String serKeyString = serString + "_" + cluString;
		try {
			if(!VintageNamingWebUtils.existsService(serString)){
				VintageNamingWebUtils.addService(serString, type);
			}
			VintageNamingClientUtils.sleep(serviceCacheInterval);
			VintageNamingWebUtils.addCluster(serString, cluString);
			VintageNamingWebUtils.addWhitelist(serString, localNodes);
			
			VintageNamingClientUtils.subscribeNode(client, serString, cluString);
			VintageNamingClientUtils.unsubscribeAllChanges(client, serviceId, clusterId);

			VintageNamingClientUtils.register(client, serString, cluString, localIP, 1234, "ext");
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1234, "ext");

			Thread.sleep(serviceCacheInterval);

			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			assertNull(VintageNamingClientUtils.nodeMap.get(serviceKey));
			assertNotNull(VintageNamingClientUtils.nodeMap.get(serKeyString));
			assertEquals(1, VintageNamingClientUtils.nodeMap.get(serKeyString).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testUnsubServiceCluster");
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId) && VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
			}
			if (VintageNamingWebUtils.existsService(serString) && VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingClientUtils.unregister(client, serString, cluString, localIP, 1234);
			}
			for (String node : localNodes) {
				if (VintageNamingWebUtils.existsWhitelist(serString, node)){
					VintageNamingWebUtils.deleteWhitelist(serString, node);
				}
			}
			if (VintageNamingWebUtils.existsService(serString) && VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingWebUtils.deleteCluster(serString, cluString);
			}
			if (VintageNamingWebUtils.existsService(serString)) {
				VintageNamingWebUtils.deleteService(serString);		
			}
		}
	}

	/**
	 * 测试点：重复多次 unsubscribeAll
	 */
	@Test
	public void testRepeatUnsuball() {
		try {
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.unsubscribeAllChanges(client, serviceId, clusterId);
			}

			// 多次取消订阅可，可再次订阅
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1234, "ext");
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1235, "ext");

			Thread.sleep(HEARTBEATINTERVAL);

			assertEquals(2, VintageNamingClientUtils.nodeMap.get(serviceId + "_" + clusterId)
					.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testRepeatUnsuball");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1235);
		}
	}
}
