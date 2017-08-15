package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.rmi.CORBA.Util;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.Jedis;
 

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.failover.NodeExciseStrategy.Dynamic;
import com.weibo.vintage.model.EndpointAddress;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.model.NodeStatus;
import com.weibo.vintage.utils.ApacheHttpClient;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConstantsTest;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 此类为测试client主动向configServer汇报状态的测试用例 ---- dynamic
 * 主要为client端通过feature.configserver.heartbeat开关决定是否发送心跳
 * 默认为true，即发送心跳
 * 
 * @author sina
 * 
 */
public class ClientNamingReportTest extends BaseTest {

	private NamingServiceClient client;

	private Set<String> wNode = new HashSet<String>();

	private String extinfo = "ext";
	private String dType = "dynamic";

	private int maxRemoveHeartInterval = 60 * HEARTBEATINTERVAL;

	// 通过 httpClient 测试 sign值相关功能
	private ApacheHttpClient httpClient;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		
		config.setServiceId(serviceId);
		config.setHeartbeatInterval(HEARTBEATINTERVAL);
		config.setPullServerAddress(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();
		init(serviceId, clusterId);
		
		ServerWebUtils.activeDetect("off");
		ServerWebUtils.Heartbeat("on");
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		VintageNamingClientUtils.unsubscribeAllChange(client);
		clean(serviceId, clusterId);
		client.close();
		VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
	}

	protected void init(String service, String cluster) {
		addService(service, dType);
		VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
		addCluster(service, cluster);
		addWhiteList(service, localNodes);
	}

	protected void clean(String service, String cluster) {

		delWhiteList(service, localNodes);
		delCluster(service, cluster);
		VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
		delService(service);
	}

	/**
	 * MultiNode: nodes registered be working stats by default 1 service multi
	 * cluster
	 */
	@Test
	public void testNodeRegisteredWithWorkingStatusByDef() {
		int iCount = 6;
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			for (int i = 0; i < iCount; i++) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId + i);
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);

				VintageNamingClientUtils.register(client, serviceId, clusterId + i, localIP, iPort, extinfo);
				VintageNamingClientUtils.register(client, serviceId, clusterId + i, localIP, iPort + 1, extinfo);
			}

			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

			for (int i = 0; i < iCount; i++) {
				assertEquals(2, VintageNamingClientUtils.getWorkingNodeList(client, serviceId,
								clusterId + i).size());
			}
			System.out.print("end");

		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {
			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId + i, localIP,
						iPort);
				VintageNamingClientUtils.unregister(client, serviceId, clusterId + i, localIP,
						iPort + 1);
			}
			VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
			for (int i = 0; i < iCount; i++) {
				delCluster(serviceId, clusterId + i);
			}

			client.close();

			VintageNamingClientUtils.sleep(6 * HEARTBEATINTERVAL);

			for (int j = 0; j < iCount; j++) {
				Map<String, String> m = VintageNamingClientUtils.redis.hgetAll(serviceId + "_"
						+ clusterId + j);

				System.out.println(j);
				System.out.println(m.toString());
				assertTrue(VintageNamingClientUtils.redis.hgetAll(
						serviceId + "_" + clusterId + j).isEmpty());
				// bugfree--169
				// assertTrue(Utils.redis.hgetAll(
				// "/naming/session_" + serviceId + "_" + clusterId + j)
				// .isEmpty());
			}

		}
	}

	/*
	 * register multi nodes to the same service_cluster;but only one node is
	 * working in the same service_cluster of one client
	 */
	@Test
	public void testMultiNodeRegisteredWithWorkingStatusByDef() {
		int iCount = 5;
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
				VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, iPort
						+ i, extinfo);
				VintageNamingClientUtils.sleep(1000);
			}

			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			assertEquals(5,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());

			System.out.print("end");
		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {
			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, iPort
						+ i);
			}

			client.close();

			VintageNamingClientUtils.sleep(6 * HEARTBEATINTERVAL);

			Map<String, String> m = VintageNamingClientUtils.redis.hgetAll(serviceId + "_"
					+ clusterId);
			System.out.println(m.toString());
			assertTrue(VintageNamingClientUtils.redis.hgetAll(serviceId + "_" + clusterId)
					.isEmpty());
			// bugfree--169
			// assertTrue(Utils.redis.hgetAll(
			// "/naming/session_" + serviceId + "_" + clusterId + j)
			// .isEmpty());

		}
	}

	/**
	 * MultiNode: nodes registered be working stats by default
	 */
	@Test
	public void testNodeRegisteredWithWorkingStatusByDefWithSub() {
		int iCount = 3;
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			for (int i = 0; i < iCount; i++) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId + i);
				VintageNamingClientUtils.register(client, serviceId, clusterId + i, localIP,
						iPort, extinfo);
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);
				//Utils.sleep(1000);
				 System.out.println(client.lookup(serviceId, clusterId + i));
			}
			VintageNamingClientUtils.sleep(15000);

			for (int i = 0; i < iCount; i++) {
				assertEquals(1, VintageNamingClientUtils.getWorkingNodeList(client, serviceId,
								clusterId + i).size());
				assertEquals(1, VintageNamingClientUtils.getSubWorkingNodes(client, serviceId,
								clusterId + i).size());
			}

			System.out.print("end...........");
		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {

			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId + i, localIP, iPort);
			}
			
			VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
			for (int i = 0; i < iCount; i++){
				delCluster(serviceId, clusterId + i);
			}

			client.close();

			VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);

			for (int j = 0; j < iCount; j++) {
				assertTrue(VintageNamingClientUtils.redis.hgetAll(
						serviceId + "_" + clusterId + j).isEmpty());
			}
			VintageNamingClientUtils.currentWorkingNodes.clear();
		}
	}

	/**
	 * 验证：多个service同一个cluster不同node，状态稳定
	 */
	@Test
	public void testMultiServiceOneClusterSataus() {
		int iCount =3;
		int iPort = 1234;
		SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		for (int i = 0; i < iCount; i++) {
			init(serviceId + i, clusterId);
			VintageNamingClientUtils.sleep(2);
		}

		try {

			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
						iPort++, extinfo);
				VintageNamingClientUtils.subscribeNode(client, serviceId + i, clusterId);
			}
			VintageNamingClientUtils.sleep(20*HEARTBEATINTERVAL);
			for (int i = 0; i < iCount; i++) {
				assertEquals(1, VintageNamingClientUtils.getWorkingNodeList(client, serviceId + i, clusterId).size());
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {
			iPort = 1234;
			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId, localIP, iPort++);
				VintageNamingClientUtils.sleep(2);
			}
			VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
			
			for (int i = 0; i < iCount; i++) {
				clean(serviceId + i, clusterId);
			}
			client.close();
		}
	}

	/**
	 * － 前提：将client向服务汇报状态的开关关闭 预期：向server注册新的节点，从server端拉取的信息状态为unreachable
	 */
	@Test
	public void testNodeRegisterWithSwitcherOff() {
		int iCount = 5;
		int iPort = 1234;
		try {

			for (int i = 0; i < iCount; i++) {
				init(serviceId + i, clusterId);
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
						iPort++, extinfo);
			}
			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
			VintageNamingClientUtils.sleep(60 * HEARTBEATINTERVAL);
			for (int i = 0; i < iCount; i++) {
				assertEquals(0, VintageNamingClientUtils.getWorkingNodeList(client, serviceId + i,
								clusterId).size());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			iPort = 1234;
			for (int i = 0; i < iCount; i++) {
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId, localIP,
						iPort++);
				clean(serviceId + i, clusterId);
			}
			client.close();
		}
	}

	/**
	 * 注册service cluster后，订阅，再注册新的cluster 预期：新的cluster的状态能够正常反应到server
	 */
	@Test
	public void testNodeRegisteredWithWorkingStatusByDefWithSubUnreg() {
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, iPort,
					extinfo);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			// register new cluster after subscribe
			VintageNamingWebUtils.addCluster(serviceId, clusterId+1);
			VintageNamingClientUtils.register(client, serviceId, clusterId + 1, localIP, 1235,
					extinfo);

			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + 1);

			VintageNamingClientUtils.sleep(15 * HEARTBEATINTERVAL);

			assertEquals(1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());
			assertEquals(
					1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId + 1)
							.size());

			assertEquals(1,
					VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId)
							.size());
			assertEquals(
					1,
					VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId + 1)
							.size());

		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {

			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, iPort);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId + 1, localIP, 1235);

			client.close();

			VintageNamingClientUtils.sleep(4000);

			assertTrue(VintageNamingClientUtils.redis.hgetAll(serviceId + "_" + clusterId)
					.isEmpty());
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId + 1);
		}
	}

	/**
	 * 将client向configServer发送心跳的开关反复开/关，最后关闭 验证：lookup验证，node的状态均为不可用状态
	 */
	@Test
	public void testNodeRegisteredWithWorkingStatusByDefWithSwitcherChange() {
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, iPort,
					extinfo);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			// register new cluster after subscribe
			VintageNamingWebUtils.addCluster(serviceId, clusterId+1);
			VintageNamingClientUtils.register(client, serviceId, clusterId + 1, localIP, 1235,
					extinfo);

			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + 1);

			VintageNamingClientUtils.sleep(5 * HEARTBEATINTERVAL);

			assertTrue(VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
					.isEmpty());
			assertTrue(VintageNamingClientUtils.getWorkingNodeList(client, serviceId,
					clusterId + 1).isEmpty());

			assertTrue(VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId)
					.isEmpty());
			assertTrue(VintageNamingClientUtils.getSubWorkingNodes(client, serviceId,
					clusterId + 1).isEmpty());

		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDefWithSwitcherChange"
					+ e.getMessage());
		} finally {

			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, iPort);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId + 1, localIP, 1235);

			client.close();
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId + 1);

			VintageNamingClientUtils.sleep(4000);

			assertTrue(VintageNamingClientUtils.redis.hgetAll(serviceId + "_" + clusterId)
					.isEmpty());
		}
	}

	/**
	 * 将client向configServer发送心跳的开关反复开/关，最后打开 验证：lookup验证，node的状态均为可用状态
	 */
	@Test
	public void testNodeRegisteredWithWorkingStatusByDefWithSwitcherChange2() {
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, iPort,
					extinfo);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			// register new cluster after subscribe
			VintageNamingWebUtils.addCluster(serviceId, clusterId+1);
			VintageNamingClientUtils.register(client, serviceId, clusterId + 1, localIP, 1235,
					extinfo);

			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + 1);

			VintageNamingClientUtils.sleep(10 * HEARTBEATINTERVAL);

			assertEquals(1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());
			assertEquals(
					1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId + 1)
							.size());

			assertEquals(1,
					VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId)
							.size());
			assertEquals(
					1,
					VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId + 1)
							.size());

		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDefWithSwitcherChange"
					+ e.getMessage());
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, iPort);
				VintageNamingClientUtils.unregister(client, serviceId, clusterId + 1, localIP, 1235);
			}

			client.close();

			VintageNamingClientUtils.sleep(4000);

			assertTrue(VintageNamingClientUtils.redis.hgetAll(serviceId + "_" + clusterId)
					.isEmpty());

			VintageNamingWebUtils.deleteCluster(serviceId, clusterId + 1);
		}
	}

	/**
	 * 前提：注册同一个service 2个cluster，各1个node 　 * 操作：取消注册1个cluster　&　再注册1个节点      *
	 * 验证：lookup验证
	 */
	@Test
	public void testRegUnregCheckStatus() {
		int iPort = 1234;
		try {
			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, iPort, extinfo);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			// register new cluster after subscribe
			VintageNamingWebUtils.addCluster(serviceId, clusterId+1);
			VintageNamingWebUtils.addCluster(serviceId, clusterId+2);

			VintageNamingClientUtils.register(client, serviceId, clusterId + 1, localIP, 1235, extinfo);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + 1);
			
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + 2);
			
			VintageNamingClientUtils.unregister(client, serviceId, clusterId + 1, localIP, 1235);
			VintageNamingClientUtils.register(client, serviceId, clusterId + 2, localIP, 1236, extinfo);

			VintageNamingClientUtils.sleep(10 * HEARTBEATINTERVAL);

			assertEquals(1, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
			assertTrue(VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId + 1).isEmpty());
			assertEquals(1, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId + 2).size());

			assertEquals(1,
					VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId)
							.size());
			assertTrue(VintageNamingClientUtils.getSubWorkingNodes(client, serviceId,
					clusterId + 1).isEmpty());
			assertEquals(
					1,
					VintageNamingClientUtils.getSubWorkingNodes(client, serviceId, clusterId + 2)
							.size());

		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDefWithSwitcherChange"
					+ e.getMessage());
		} finally {
			
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, iPort);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId + 1, localIP, 1235);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId + 2, localIP, 1236);
			client.close();
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId + 1);
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId + 2);

			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);

			assertTrue(VintageNamingClientUtils.redis.hgetAll(serviceId + "_" + clusterId)
					.isEmpty());
		}
	}

	@Test
	public void testServiceType() {
		int port = 1234;
		String serviceId = "serviceType";
		String clusterId = "clusterType";

		addService(serviceId, "statics");
		VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
		addCluster(serviceId, clusterId);
		addWhiteList(serviceId, localNodes);
		VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
		VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);

		try {
			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port, extinfo);

			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
			assertEquals(1, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());

			System.out.print("dynamic");
			VintageNamingWebUtils.updateService(serviceId, "dynamic");
			VintageNamingClientUtils.sleep(20*HEARTBEATINTERVAL);
			// 设置策略 如果所有节点都不可用，则采取设置40%节点保持为working状态
			assertEquals(1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());

			System.out.print("open switch");
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
			VintageNamingClientUtils.sleep(20*HEARTBEATINTERVAL);
			assertEquals(1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());

			System.out.print("close switch");
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
			VintageNamingClientUtils.sleep(20*HEARTBEATINTERVAL);
			assertEquals(1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());

			System.out.print("statics");
			VintageNamingWebUtils.updateService(serviceId, "statics");

			System.out.print("open switch");
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
			VintageNamingClientUtils.sleep(20*HEARTBEATINTERVAL);
			assertEquals(1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());

		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);

			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			VintageNamingWebUtils.deleteWhitelist(serviceId, localNodes);
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			VintageNamingWebUtils.deleteService(serviceId);
		}

	}

	/**
	 * 3 clients,every client register different node to the services;
	 */
	@Test
	public void testMultiCliDifNodeMultiSerOneCluSataus() {
		int iCount = 2;
		int iPort = 1234;
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		for (int i = 0; i < iCount; i++) {
			init(serviceId + i, clusterId);
			VintageNamingClientUtils.sleep(2);
		}

		NamingServiceClient client1 = new NamingServiceClient(config);
		client1.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client1.start();

		NamingServiceClient client2 = new NamingServiceClient(config);
		client2.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client2.start();

		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		clientList.add(client);
		clientList.add(client1);
		clientList.add(client2);

		try {
			for (NamingServiceClient client : clientList) {

				for (int i = 0; i < iCount; i++) {
					VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
							iPort++, extinfo);
					VintageNamingClientUtils.subscribeNode(client, serviceId + i, clusterId);
				}
			}

			VintageNamingClientUtils.sleep(20 * this.HEARTBEATINTERVAL);

			for (NamingServiceClient client : clientList) {
				for (int i = 0; i < iCount; i++) {
					assertEquals(
							clientList.size(),
							VintageNamingClientUtils.getWorkingNodeList(client, serviceId + i,
									clusterId).size());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {
			iPort = 1234;
			for (NamingServiceClient client : clientList) {
				for (int i = 0; i < iCount; i++) {
					VintageNamingClientUtils.unregister(client, serviceId + i, clusterId,
							localIP, iPort++);

				}
			}
			for (int i = 0; i < iCount; i++) {
				clean(serviceId + i, clusterId);
			}

		}
	}

	/**
	 * 3 clients,every client register same node to the 5 services;
	 */
	@Test
	public void testMultiCliSameNodeMultiSerOneCluSataus() {
		int iCount = 5;
		int iPort = 1234;

		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		for (int i = 0; i < iCount; i++) {
			init(serviceId + i, clusterId);
		}

		NamingServiceClient client1 = new NamingServiceClient(config);
		client1.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client1.start();

		NamingServiceClient client2 = new NamingServiceClient(config);
		client2.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client2.start();

		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		clientList.add(client);
		clientList.add(client1);
		clientList.add(client2);

		try {
			for (NamingServiceClient client : clientList) {
				for (int i = 0; i < iCount; i++) {
					VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
							iPort + i, extinfo);
					VintageNamingClientUtils.subscribeNode(client, serviceId + i, clusterId);
				}
			}

			VintageNamingClientUtils.sleep(10 * HEARTBEATINTERVAL);

			for (NamingServiceClient client : clientList) {
				for (int i = 0; i < iCount; i++) {
					assertEquals(
							1,
							VintageNamingClientUtils.getWorkingNodeList(client, serviceId + i,
									clusterId).size());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("ERROR in testNodeRegisteredWithWorkingStatusByDef"
					+ e.getMessage());
		} finally {
			iPort = 1234;
			for (NamingServiceClient client : clientList) {
				for (int i = 0; i < iCount; i++) {
					VintageNamingClientUtils.unregister(client, serviceId + i, clusterId,
							localIP, iPort + i);

				}
			}
			for (int i = 0; i < iCount; i++) {
				clean(serviceId + i, clusterId);
			}

		}
	}

	/**
	 * 原为移除长期unreachable节点，先更改为不移除
	 * 
	 * 2015-9－17
	 * @author liuyu9
	 * 
	 * */
	@Test
	public void RemoveLongTimeUnreachableNode() {
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		int port = 1234;
		try {
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);
			VintageNamingClientUtils.sleep(maxRemoveHeartInterval);

			assertTrue(VintageNamingWebUtils.lookup(serviceId, clusterId).contains(
					String.valueOf(port)));

			// 停止发送心跳
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
			VintageNamingClientUtils.sleep(maxRemoveHeartInterval);

			System.out.println(VintageNamingWebUtils.lookup(serviceId, clusterId));
			assertTrue(VintageNamingWebUtils.lookup(serviceId, clusterId)
					.contains(String.valueOf(port)));
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId) && VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			}
		}
	}

	/*
	 * 如果一开始没有心跳数据，预期：会删除，结果:不会删除，不处理这种没有心跳数据的node
	 */
	@Test
	public void NotRemoveLongTimeUnreachableNode() {
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
		int port = 1234;
		try {
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);
			VintageNamingClientUtils.sleep(maxRemoveHeartInterval);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);

			System.out.println(VintageNamingWebUtils.lookup(serviceId, clusterId));
			assertTrue(VintageNamingWebUtils.lookup(serviceId, clusterId).contains(
					String.valueOf(port)));

		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
		}
	}

	/*
	 * statics服务的不会删掉
	 */
	@Test
	public void NotRemoveStaitcsLongTimeUnreachableNode() {
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		int port = 1234;
		String staticSerString = "staticService";
		dType = "statics";
		try {
			init(staticSerString, this.clusterId);
			VintageNamingClientUtils.subscribeNode(client, staticSerString, clusterId);
			VintageNamingClientUtils.register(client, staticSerString, clusterId, localIP, port,
					extinfo);
			VintageNamingClientUtils.sleep(maxRemoveHeartInterval);

			assertTrue(VintageNamingWebUtils.lookup(staticSerString, clusterId)
					.contains(String.valueOf(port)));
		} finally {
			VintageNamingClientUtils.unregister(client, staticSerString, clusterId, localIP, port);
			clean(staticSerString, clusterId);
			dType = "dynamic";
		}
	}

	/**
	 * case： 查看并发请求
	 * 预期结果：Client能收到所有cluster节点变更的信息
	 * 
	 * @author liuyu9
	 *
	 */
	@Ignore
	@Test
	public void testConcurrent() throws Throwable {
		TestRunnable[] trs = new TestRunnable [10];  
        for(int i = 0; i < 10; i++){  
            trs[i]=new ThreadA();  
        }  
 
        // 用于执行多线程测试用例的Runner，将前面定义的单个Runner组成的数组传入 
        MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);  
         
        // 开发并发执行数组里定义的内容 
        mttr.runTestRunnables();  
	}
	
	private class ThreadA extends TestRunnable {  
        @Override  
        public void runTest() throws Throwable {  
            // 测试内容
        	performancenodeheatbeattest();  
        }  
    }  

	public void performancenodeheatbeattest() {
		System.out.println("===" + Thread.currentThread().getId() + " begin to execute subscribeTest");
		String index = Long.toString(Thread.currentThread().getId());
		int port = 1234;
		try {
			// 10service ,100 cluster for every client,one node for every cluster
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);

			 for (int i = 0; i < 10; i++) {
					 addService(serviceId + index + i, "statics");
			 }
			 VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
			 for (int i = 0; i < 10; i++){
				 addWhiteList(serviceId + index + i, localNodes);
			 }
			 for (int i = 0; i < 10; i++) {
				 for (int j = 0; j < 100; j ++){
					 addCluster(serviceId + index + i, clusterId + j);
				 }
			 }
			 NamingServiceClient client = new NamingServiceClient(config);
			 client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
			 client.start();
			 for (int i = 0; i < 10; i++){
				 for (int j = 0; j < 100; j++){
					 VintageNamingClientUtils.register(client, serviceId + index + i, clusterId + j, localIP, port++, extinfo);
					 VintageNamingClientUtils.subscribeNode(client, serviceId + index + i, clusterId + j);
				 }
			 }
			 
			VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);

			 for (int i = 0; i < 10; i++) {
				 for (int j = 0; j < 100; j++) {
					 assertEquals(1, VintageNamingClientUtils.getWorkingNodeList(client, serviceId + index + i, 
							 clusterId + j).size());
				 }
			 }
			
			System.out.println("end");
			
		} catch (VintageException ex) {
			// TODO: handle exception
			ex.printStackTrace();
		} finally {
			for (int i = 0; i < 10; i++){
				 for (int j = 0; j < 100; j++){
					 VintageNamingClientUtils.unregister(client, serviceId + index + i, clusterId + j, localIP, port++);
				 }
			 }
			VintageNamingClientUtils.unsubscribeAllChange(client);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
			for (int i = 0; i < 10; i++) {
				 for (int j = 0; j < 100; j++) {
					 delCluster(serviceId + index + i, clusterId + j);
					 delWhiteList(serviceId + index + i, localNodes);
				 }
			 }
			VintageNamingClientUtils.sleep(5*HEARTBEATINTERVAL);
			for (int i = 0; i < 10; i++){
				delService(serviceId + index + i);
			}
			client.close();
		}
	}

}
