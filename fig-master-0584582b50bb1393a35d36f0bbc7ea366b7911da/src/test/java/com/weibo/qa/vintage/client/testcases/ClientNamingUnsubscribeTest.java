package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.naming.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class ClientNamingUnsubscribeTest extends BaseTest {

	private NamingServiceClient client;

	private Set<NamingServiceNode> services = new HashSet();
	private String serviceKey = "";
	private String type = "dynamic";

	@Before
	public void setUp() throws Exception {
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		
		super.setUp();
		config.setServiceId(serviceId);
		config.setHeartbeatInterval(HEARTBEATINTERVAL);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();
		addService(serviceId, type);
		VintageNamingClientUtils.sleep(serviceCacheInterval);
		addCluster(serviceId, clusterId);
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
		addWhiteList(serviceId, localNodes);
		serviceKey = serviceId + "_" + clusterId;
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * 订阅cluster后，取消订阅，不能收到变更
	 */
	@Test
	public void testUnsub() {
		int port = 2345;
		try {
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);

			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			assertEquals(1, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());
			assertTrue(VintageNamingClientUtils.nodeMap.get(serviceKey) == null || 
					VintageNamingClientUtils.nodeMap.get(serviceKey).isEmpty() || 
					VintageNamingClientUtils.nodeMap.get(serviceKey).size() == 0);

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			fail("Error in testUnsub");
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
		}
	}

	/**
	 * 重复取消订阅100次，验证：节点变更不会通知到client; 最后订阅一次，验证：节点变更会通知client
	 */
	@Test
	public void testRepeatUnsub() {
		int port = 2345;
		try {
			for (int i = 0; i < 100; i++) {
				VintageNamingClientUtils.unsubscribeChange(client, clusterId);
			}

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port, "");
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			assertNull(VintageNamingClientUtils.nodeMap.get(serviceId));

			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			assertEquals(1, VintageNamingClientUtils.nodeMap.get(serviceKey).size());

		} catch (Exception e) {
			e.printStackTrace();
			fail("Repeat unsubscribe should not throw exception");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	/**
	 * 重复订阅100次，验证 ：节点变更会通知到client; 最后取消订阅一次，验证：节点变更不会通知client
	 */
	@Test
	public void testRepeatsubUnsub() {
		int port = 2345;
		try {
			for (int i = 0; i < 100; i++) {
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			}

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port, "");
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			assertEquals(1, VintageNamingClientUtils.nodeMap.get(serviceKey).size());

			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			// Utils.sleep(3000);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			assertEquals(0, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());
			System.out.print(VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(1, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Repeat unsubscribe should not throw exception");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
		}
	}

	/**
	 * 测试点：重复执行订阅及取消订阅，最终为订阅状态
	 * 
	 * 操作：变更节点的注册状态 验证：能收到变更通知 操作2:取消订阅 验证2：无法收到变更通知
	 */
	@Test
	public void testSwitchSub() {
		try {
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
				VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			}
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1234, "ext");
			Thread.sleep(HEARTBEATINTERVAL);
			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			assertEquals(1, client.lookup(serviceId, clusterId).size());

			// 取消订阅
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
			Thread.sleep(HEARTBEATINTERVAL);
			System.out.print(VintageNamingClientUtils.currentNodes);
			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			assertEquals(0, client.lookup(serviceId, clusterId).size());

		} catch (Exception e) {
			e.printStackTrace();
			fail("error ini testSwitchSub");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
		}
	}

	/**
	 * 测试点：对同一service下的多个cluster进行部分取消,及对最后一个的取消操作
	 * 
	 * 验证：对于未取消的cluster可收到节点变更
	 * 
	 * 操作2：取消订阅最后一个cluster
	 * 
	 * 验证：收不到任何节点变更
	 */
	@Test
	public void testMultiCluster() {
		try {
			// 订阅同一个 service下的 5个cluster
			for (int i = 0; i < 5; i++) {
				if (!VintageNamingWebUtils.existCluster(serviceId, "cluster" + i)) {
					VintageNamingWebUtils.addCluster(serviceId, "cluster" + i);
				}
				VintageNamingClientUtils.subscribeNode(client, serviceId, "cluster" + i);
			}

			// 变更 5 个 cluster节点的变更，client均可收到变更通知
			for (int i = 1200; i < 1210; i++) {
				for (int num = 0; num < 5; num++) {
					VintageNamingClientUtils.register(client, serviceId, "cluster" + num, localIP,
							i, "ext");
				}
			}
			Thread.sleep(serviceCacheInterval);

			// 验证 client可收到 5个cluster的节点变更通知
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster0").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster1").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster2").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster3").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster4").size());

			// 取消订阅 4个cluster的节点变更，
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unsubscribeChange(client, serviceId, "cluster" + i);
			}

			for (int num = 0; num < 5; num++) {
				for (int i = 1200; i < 1205; i++) {
					VintageNamingClientUtils.unregister(client, serviceId, "cluster" + num,
							localIP, i);
				}
			}
			Thread.sleep(serviceCacheInterval);

			// 验证 client可收到 1个cluster的节点变更通知
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster0").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster1").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster2").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster3").size());
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster4").size());

			for (int i = 0; i < 5; i++) {
				System.out
						.print(client.lookup(serviceId, "cluster" + i).size());
				assertEquals(5, client.lookup(serviceId, "cluster" + i).size());
			}

			// 取消最后一个cluster节点变更
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, "cluster4");

			for (int i = 1205; i < 1210; i++) {
				for (int num = 0; num < 5; num++) {
					VintageNamingClientUtils.unregister(client, serviceId, "cluster" + num,
							localIP, i);
				}
			}

			VintageNamingClientUtils.sleep(serviceCacheInterval);
			// 任何cluster的节点变更均无法收到通知
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster0").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster1").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster2").size());
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster3").size());
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serviceId + "_cluster4").size());

			for (int i = 0; i < 5; i++) {
				assertEquals(0, client.lookup(serviceId, "cluster" + i).size());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testMultiCluster");
		} finally {
			VintageNamingClientUtils.unsubscribeAllChange(client);
			// 变更 5 个 cluster节点的变更，client均可收到变更通知
			for (int num = 0; num < 5; num++) {
				for (int i = 1200; i < 1210; i++) {
					VintageNamingClientUtils.unregister(client, serviceId, "cluster" + num,
							localIP, i);
				}
				VintageNamingWebUtils.deleteCluster(serviceId, "cluster" + num);
			}
		}
	}

	/**
	 * 测试点：对多个service下的多个cluster进行部分取消,及对最后一个的取消操作
	 * 
	 * 验证：对于未取消的cluster可收到节点变更
	 * 
	 * 操作2：取消订阅最后一个cluster
	 * 
	 * 验证：收不到任何节点变更
	 */
	@Test
	public void testMultiService() {
		try {
			// 订阅5个 service下的 5个cluster
			for (int i = 1; i <= 5; i++) {
				if (!VintageNamingWebUtils.existsService(serviceId + i)) {
					VintageNamingWebUtils.addService(serviceId + i, type);
				}
				if (!VintageNamingWebUtils.existCluster(serviceId + i, clusterId)) {
					VintageNamingWebUtils.addCluster(serviceId + i, clusterId);
				}
				VintageNamingWebUtils.addWhitelist(serviceId + i, localNodes);
				VintageNamingClientUtils.subscribeNode(client, serviceId + i, clusterId);
			}

			// 变更 5 个 cluster节点的变更，client均可收到变更通知
			for (int i = 1200; i < 1210; i++) {
				for (int num = 1; num <= 5; num++) {
					VintageNamingClientUtils.register(client, serviceId + num, clusterId, localIP,
							i, "ext");
				}
			}
			Thread.sleep(serviceCacheInterval);

			// 验证 client可收到 5个cluster的节点变更通知
			for (int i = 1; i <= 5; i++) {
				assertEquals(10,
						VintageNamingClientUtils.nodeMap.get(serviceId + i + "_" + clusterId)
								.size());
			}

			// 取消订阅 4个cluster的节点变更，
			for (int i = 1; i <= 4; i++) {
				VintageNamingClientUtils.unsubscribeChange(client, serviceId + i, clusterId);
			}

			for (int num = 1; num <= 5; num++) {
				for (int i = 1200; i < 1205; i++) {
					VintageNamingClientUtils.unregister(client, serviceId + num, clusterId,
							localIP, i);
				}
			}
			Thread.sleep(serviceCacheInterval);

			// 验证 client可收到 1个cluster的节点变更通知
			for (int i = 1; i <= 4; i++) {
				assertEquals(10,
						VintageNamingClientUtils.nodeMap.get(serviceId + i + "_" + clusterId)
								.size());
			}
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serviceId + "5_" + clusterId)
					.size());

			for (int i = 1; i <= 5; i++) {
				System.out
						.print(client.lookup(serviceId + i, clusterId).size());
				assertEquals(5, client.lookup(serviceId + i, clusterId).size());
			}

			// 取消最后一个cluster节点变更
			VintageNamingClientUtils.unsubscribeChange(client, serviceId + 5, clusterId);

			for (int i = 1205; i < 1210; i++) {
				for (int num = 1; num <= 5; num++) {
					VintageNamingClientUtils.unregister(client, serviceId + num, clusterId,
							localIP, i);
				}
			}

			VintageNamingClientUtils.sleep(serviceCacheInterval);
			// 任何cluster的节点变更均无法收到通知
			for (int i = 1; i <= 4; i++) {
				assertEquals(10,
						VintageNamingClientUtils.nodeMap.get(serviceId + i + "_" + clusterId)
								.size());
			}
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serviceId + "5_" + clusterId)
					.size());

			for (int i = 1; i <= 5; i++) {
				assertEquals(0, client.lookup(serviceId + i, clusterId).size());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testMultiCluster");
		} finally {
			// Utils.unsubscribeAllChange(client);
			// 变更 5 个 cluster节点的变更，client均可收到变更通知
			for (int num = 1; num <= 5; num++) {
				for (int i = 1200; i < 1210; i++) {
					VintageNamingClientUtils.unregister(client, serviceId + num, clusterId,
							localIP, i);
				}
				VintageNamingWebUtils.deleteWhitelist(serviceId + num, localNodes);
				VintageNamingClientUtils.sleep(100);
				VintageNamingWebUtils.deleteCluster(serviceId + num, clusterId);
				VintageNamingClientUtils.sleep(100);
				VintageNamingWebUtils.deleteService(serviceId + num);
			}
		}
	}

	/**
	 * 取消未订阅过的cluster，服务正常
	 */
	@Test
	public void testUnsubNoSub() {
		try {
			VintageNamingClientUtils.unsubscribeAllChange(client, "testCluster");
			assertTrue(true);
		} catch (Exception e) {
			fail("unsubscribe no subscribe cluster,should not throw exception");
		}
	}

	/**
	 * 取消订阅不存在的service和cluster
	 */
	@Test
	public void testUnsubSerNoExist() {
		String serString = "testService";
		String cluString = "testCluster";
		try {
			if (!VintageNamingWebUtils.existsService(serString)
					|| !VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingClientUtils.unsubscribeChange(client, serString, cluString);
			} else {
				fail("service and cluster exists!");
			}
			//fail("No exception in unsubscribe service and cluster");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/**
	 * 测试点：参数异常测试 操作：分别将cluster service listener设置为null 预期结果：参数异常
	 * E_PARAM_INVALID_ERROR
	 */
	@Test
	public void testParamAbnormal() {
		try {
			// clusterId = null
			try {
				VintageNamingClientUtils.unsubscribeChange(client, null);
				fail("should throw nullpointerexception");
			} catch (VintageException e) {
//				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
//						e.getFactor().getErrorCode());
			}

			// serviceId = null
			try {
				VintageNamingClientUtils.unsubscribeChange(client, null, clusterId);
				fail("should throw nullpointerexception");
			} catch (VintageException e) {
//				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
//						e.getFactor().getErrorCode());
			}

			// listerner = null
			try {
				client.unsubscribeNodeChanges(serviceId, clusterId, null);
				fail("should throw nullpointerexception");
			} catch (VintageException e) {
//				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
//						e.getFactor().getErrorCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testParamAbnormal");
		}
	}
}
