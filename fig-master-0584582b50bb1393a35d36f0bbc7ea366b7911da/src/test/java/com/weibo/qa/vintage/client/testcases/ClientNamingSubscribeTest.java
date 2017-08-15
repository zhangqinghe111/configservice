package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;  
import net.sourceforge.groboutils.junit.v1.TestRunnable; 

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.qa.vintage.naming.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 此类为订阅功能相关测试用例
 * 订阅功能：当client端向configserver发送订阅请求时，client的handler会监听相应cluster是否发生变动
 * 一般指节点变动，如有新增注册节点或有节点取消注册等，实时通知client端
 * 另lookup功能可查阅相应cluster下的节点数
 * 
 * lookup与订阅的区别在于lookup为手动查询，订阅则被动查询
 * 
 * 验证方式：心跳线程数据验证 + lookup出来的数据验证 + 两者一致性验证
 * 
 * @author lingling6
 * 
 */
public class ClientNamingSubscribeTest extends BaseTest {

	private NamingServiceClient client;

	private Set<NamingServiceNode> services = new HashSet();
	private Set<NamingServiceNode> nodes;
	private Set<String> wsnodes = new HashSet<String>();

	private String serviceKey = "";
	private String type = "dynamic";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		
		config.setServiceId(serviceId);
		config.setHeartbeatInterval(HEARTBEATINTERVAL);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();

		serviceKey = serviceId + "_" + clusterId;
		// 增加service
		addService(serviceId, type);
		VintageNamingClientUtils.sleep(serviceCacheInterval);
		// 增加cluster
		addCluster(serviceId, clusterId);
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
		// 增加白名单
		addWhiteList(serviceId, localNodes);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		wsnodes.clear();
	}

	/**
	 * case1: client订阅空的cluster， 预期结果：可收到节点变更的通知
	 * 订阅周期代码写死为20s
	 */
	@Test
	public void testSubOneNode() throws InterruptedException {
		try {
			
			/**
			 * 订阅空的cluster
			 */
			services = client.lookup(clusterId);
			assertEquals(0, services.size());
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			nodes = VintageNamingClientUtils.currentNodes;
			assertEquals(0, nodes.size());
			
			/**
			 * 节点注册
			 */
			VintageNamingClientUtils.register(client, clusterId, localIP, 1234);

			Thread.sleep(2 * HEARTBEATINTERVAL);

			/**
			 * 检查client是否收到节点变更通知
			 */
			nodes = VintageNamingClientUtils.currentNodes;
			assertEquals(1, nodes.size());
			Set<NamingServiceNode> nServices = client.lookup(clusterId);
			assertEquals(nServices.size(), 1);
			assertEquals(nodes, nServices);

		} catch (Exception e) {
			fail("error in testSubOneNode");
		} finally {
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	/**
	 * case2： 操作：client订阅非空的cluster 预期结果：可收到节点变更的通知
	 */
	@Test
	public void testSubReg() {
		try {
			/**
			 * 订阅非空的cluster
			 */
			assertTrue(VintageNamingClientUtils.currentNodes.isEmpty());
			VintageNamingClientUtils.register(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			assertNull(VintageNamingClientUtils.nodeMap.get(serviceKey));
			VintageNamingClientUtils.subscribeNode(client, clusterId);
			Thread.sleep(2 * HEARTBEATINTERVAL);

			/**
			 * check：client是否收到节点变更通知
			 */
			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			nodes = client.lookup(clusterId);
			assertEquals(1, nodes.size());

			/**
			 * 节点变更：取消注册，检查是否收到变更通知
			 */
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());

		} catch (Exception e) {
			fail("error in testSubReg");
		} finally {
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unsubscribeChange(client, clusterId);
		}
	}

	/**
	 * case3: 测试点：节点未订阅节点变更， 预期结果：则不会收到通知
	 */
	@Test
	public void testNoSubNoNotice() {
		try {
			/**
			 * 节点变更
			 */

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1235, "");
			Thread.sleep(2 * HEARTBEATINTERVAL);

			/**
			 * 没有订阅时，不会收到节点变更通知
			 */
			assertNull(VintageNamingClientUtils.nodeMap.get(serviceKey));

			/**
			 * 检查实际的节点情况
			 */
			services = VintageNamingClientUtils.lookup(client, serviceId, clusterId);
			assertEquals(services.size(), 1);
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("error in noSubNoNotice should not into Exception");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1235);
		}
	}

	// case6-1: 取消注册最后一个节点，是否能将变更信息推送至client端
	/*
	 * 前提：Client订阅一个没有节点的cluster 操作：cluster中注册一个节点，sleep一段时间后将该节点取消注册
	 * 预期结果：Client端能收到节点取消注册的信息
	 */
	@Test
	public void testSub4() {
		try {
			/**
			 * 防止之前用例的影响，先取消订阅
			 */
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			/**
			 * 注册节点
			 */
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1234, "");
			// sleep的时间与client-server之间的心跳时间有十分紧密的关系
			// this.sleep(2000);

			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(1, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(1, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());
			/**
			 * 取消注册节点
			 */
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKey).size());

			System.out.println("end.................................");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			fail("error in testSub6 should not into Exception");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	// case6-2: 取消注册最后一个节点，是否能将变更信息推送至client端
	/*
	 * 前提：Client订阅一个非空的cluster 操作：cluster中注册一个节点，sleep一段时间后将该节点全部取消注册
	 * 预期结果：Client端能收到节点取消注册的信息
	 */
	@Test
	public void testSub5() {

		try {
			/**
			 * 防止之前用例的影响，先取消订阅
			 */
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);

			VintageNamingClientUtils.register(client, clusterId, localIP, 1236, "");
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);

			VintageNamingClientUtils.register(client, clusterId, localIP, 1234, "");
			VintageNamingClientUtils.sleep(3 * HEARTBEATINTERVAL);
			assertEquals(2, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(2, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());

			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1236);

			VintageNamingClientUtils.sleep(3 * HEARTBEATINTERVAL);
			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());
			System.out.println("end.................................");
			// sleep的时间与client-server之间的心跳时间有十分紧密的关系
			// this.sleep(3000);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1236);

			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	/**
	 * case6: 测试点：注册节点从 200个逐渐取消注册 验证：最后一个节点取消注册时,是否可正常收到通知
	 */
	@Test
	public void testContinueUnsub() {
		try {

			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			/**
			 * 节点变更：注册200个节点;sleep一段时间，取消注册199个节点
			 */
			for (int i = 1200; i < 1400; i++) {
				VintageNamingClientUtils.register(client, clusterId, localIP, i, "extInfo");
			}
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			for (int i = 1200; i < 1399; i++) {
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
			}
			Thread.sleep(4 * HEARTBEATINTERVAL);
			/**
			 * 验证client是否收到节点变更通知
			 */
			assertEquals(1, VintageNamingClientUtils.currentNodes.size());
			nodes = client.lookup(clusterId);
			assertEquals(1, nodes.size());

			/**
			 * 取消注册最后一个节点,检查client是否收到节点变更通知
			 */
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1399);
			Thread.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());
		} catch (Exception e) {
			fail("error in testContinueUnsub");
		} finally {
			for (int i = 1200; i < 1400; i++) {
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
			}

			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	/**
	 * case7: 重复订阅 1个 cluster, 重复100 次 预期结果：能正常接收节点变更通知
	 */
	@Test
	public void testRepeatSub() {
		try {
			// 重复订阅
			for (int i = 0; i < 100; i++) {
				VintageNamingClientUtils.subscribeNode(client, clusterId);
			}

			Thread.sleep(3 * HEARTBEATINTERVAL);
			assertTrue(VintageNamingClientUtils.currentNodes.isEmpty());

			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.register(client, clusterId, localIP, i, "extInfo");
			}
			Thread.sleep(3 * HEARTBEATINTERVAL);

			assertEquals(10, VintageNamingClientUtils.currentNodes.size());
			nodes = client.lookup(clusterId);
			assertEquals(10, nodes.size());

		} catch (Exception e) {
			fail("error in testRepeatSub");
		} finally {
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
			}

			VintageNamingClientUtils.unsubscribeChange(client, clusterId);
		}
	}

	/*
	 * case 8：重复订阅 前提：Client订阅同一个service的80个cluster 操作：每一个cluter节点变更（节点注册和取消注册）
	 * 预期结果：Client能收到所有cluster节点变更的信息
	 */
	@Test
	public void testRepeatSubDifClu() {
		/**
		 * 订阅空的cluster
		 */
		try {
			for (int i = 20; i < 100; i++) {
				addCluster(serviceId, clusterId+i);
			}
			for (int i = 20; i < 100; i++) {
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);
			}
			VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
					
			/**
			 * 节点注册
			 */
			for (int i = 20; i < 100; i++) {
				VintageNamingClientUtils.register(client, clusterId + i, localIP, 1234, "exterinfoA");
				VintageNamingClientUtils.register(client, clusterId + i, localIP, 1235, "");
				VintageNamingClientUtils.sleep(10);
			}

			VintageNamingClientUtils.sleep(2* HEARTBEATINTERVAL);

			System.out.println(VintageNamingClientUtils.currentNodes);
			/**
			 * 检查client是否收到节点变更通知
			 */
			for (int i = 20; i < 100; i++) {
				String serKey = serviceId + "_" + clusterId + i;
				assertEquals(2, VintageNamingClientUtils.nodeMap.get(serKey).size());
				assertEquals(2, VintageNamingClientUtils.lookup(client, serviceId, clusterId + i).size());
			}		
		} catch (Exception e) {
			fail("Error in testRepeatSubDifClu");
		} finally {
			for (int i = 20; i < 100; i++) {
				if(VintageNamingWebUtils.existCluster(serviceId, clusterId+i)){
					VintageNamingClientUtils.unregister(client, clusterId + i, localIP, 1234);
					VintageNamingClientUtils.unregister(client, clusterId + i, localIP, 1235);
				}
			}
			VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
			for (int i = 20; i < 100; i++) {
				delCluster(serviceId, clusterId+i);
			}

			for (int i = 20; i < 100; i++) {
				VintageNamingClientUtils.unsubscribeChange(client, clusterId + i);
			}
		}
	}

	
	/**
	 * case 8_1：重复订阅 前提：Client订阅同一个service的80个cluster 操作：每一个cluter节点变更（节点注册和取消注册）
	 * 预期结果：Client能收到所有cluster节点变更的信息
	 * 
	 * @author liuyu9
	 * bug: register节点externinfo如果为\"exterinfoA\"，则client拿不到节点变化信息，nodes为空
	 */
	@Test
	public void testRepeatSubDifClu_2() {
		/**
		 * 订阅空的cluster
		 */
		try{
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.register(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.register(client, clusterId, localIP, 1236);
			VintageNamingClientUtils.register(client, clusterId, localIP, 1235, "exterinfoA");
			//Utils.register(client, clusterId, localIP, 1237, "\"exterinfoA\"");
			VintageNamingClientUtils.sleep(5 * HEARTBEATINTERVAL);
			nodes = VintageNamingClientUtils.currentNodes;
			System.out.println(nodes);
			assertEquals(3, nodes.size());
			Set<NamingServiceNode> nServices = client.lookup(clusterId);
			assertEquals(nServices.size(), 3);
			assertEquals(nodes, nServices);
		}catch(Exception e){
			fail("Error in testRepeatSubDifClu_2 ");
		} finally{
			for (int port = 1234; port < 1237; port++){
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			}
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}
	
	/*
	 * case 9 重复订阅：同一client连续订阅80个不同service下的节点
	 * 前提：Client订阅80个不同的service下的cluster 操作：每一个cluter节点变更（节点注册和取消注册）
	 * 预期结果：Client能收到所有service下cluster的节点变更的信息
	 */
	@Test
	public void testRepeatSubDifSer() {
		try {
			// 首先add相应的service
			for (int i = 20; i < 100; i++) {
				addService(serviceId + i, type);
			}
			for (int i = 20; i < 100; i++){
				VintageNamingWebUtils.addWhitelist(serviceId + i, localNodes);
			}
			// 订阅
			for (int i = 20; i < 100; i++) {
				VintageNamingWebUtils.addCluster(serviceId + i,  clusterId);
				VintageNamingClientUtils.subscribeNode(client, serviceId + i, clusterId);
			}
			// 每个service下注册节点
			for (int i = 20; i < 100; i++) {
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP, 1234,
						"");
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP, 1235,
						"");
			}
			VintageNamingClientUtils.sleep(10 * HEARTBEATINTERVAL);
			// 验证client节点变更情况
			for (int i = 20; i < 100; i++) {
				String serKey = serviceId + i + "_" + clusterId;
				assertEquals(2, VintageNamingClientUtils.nodeMap.get(serKey).size());
				assertEquals(2, VintageNamingClientUtils.lookup(client, serviceId + i, clusterId)
						.size());
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			fail("Error in testRepeatSubDifSer");
		} finally {
			for (int i = 20; i < 100; i++) {
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId, localIP,
						1234);
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId, localIP,
						1235);
			}
			for (int i = 20; i < 100; i++) {
				VintageNamingClientUtils.unsubscribeChange(client, serviceId + i, clusterId);
			}
			for (int i = 20; i < 100; i++) {
				VintageNamingWebUtils.deleteCluster(serviceId + i, clusterId);
				VintageNamingClientUtils.sleep(100);
			}
			for (int i = 20; i < 100; i++) {
				VintageNamingWebUtils.deleteWhitelist(serviceId + i, localNodes);
				VintageNamingClientUtils.sleep(100);
			}
			for (int i = 20; i < 100; i++) {
				VintageNamingWebUtils.deleteService(serviceId + i);
				VintageNamingClientUtils.sleep(100);
			}

		}
	}

	/**
	 * case12 & case13 测试点：订阅一个service对应的两个 cluster 操作：分别变更两个cluster节点信息，
	 * 验证：是否可正常收到变更 操作2：取消订阅一个 cluster节点信息 操作3：变更取消订阅的cluster中节点信息
	 * 验证3：验证收不到已取消订阅的cluster节点信息 操作4：变更未取消的 cluster中节点信息
	 * 验证4：验证可收到订阅cluster中节点变更信息
	 */
	@Test
	public void testSubTwoClusters() {
		String cluster2 = getRandomString(20);
		VintageNamingWebUtils.addCluster(serviceId, cluster2);
		String serKey = serviceId + "_" + cluster2;
		try {
			// 订阅同一个service的不同cluster
			VintageNamingClientUtils.subscribeNode(client, clusterId);
			VintageNamingClientUtils.subscribeNode(client, cluster2);

			/**
			 * 两个cluster中的节点发生变化：节点注册、取消注册、以及注册相同的节点（不同的extinfo）
			 */
			// 注册节点
			for (int i = 1200; i < 1210; i++) {
				VintageNamingClientUtils.register(client, clusterId, localIP, i, "extInfo2");
				VintageNamingClientUtils.sleep(100);
			}
			for (int i = 1200; i < 1205; i++) {
				VintageNamingClientUtils.register(client, cluster2, localIP, i, "extInfo");
				VintageNamingClientUtils.sleep(100);
			}
			Thread.sleep(3 * HEARTBEATINTERVAL);
			// 验证
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serKey).size());

			// 取消注册节点
			for (int i = 1200; i < 1202; i++) {
				VintageNamingClientUtils.unregister(client, cluster2, localIP, i);
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
				VintageNamingClientUtils.sleep(100);
			}
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(8, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serKey).size());

			// 给extinfo赋不同的值，重新注册。（首先清空nodemap，为了证明改变extinfo能向client推送信息）
			VintageNamingClientUtils.nodeMap.clear();
			VintageNamingClientUtils.register(client, cluster2, localIP, 1202, "extInfo-different");
			VintageNamingClientUtils.register(client, clusterId, localIP, 1202,
					"extInfo-different");
			VintageNamingClientUtils.sleep(3 * HEARTBEATINTERVAL);
			assertEquals(8, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serKey).size());

			/**
			 * 取消订阅其中一组
			 */
			VintageNamingClientUtils.unsubscribeChange(client, cluster2);
			// 两个cluster的节点发生改变
			for (int i = 1210; i < 1220; i++) {
				VintageNamingClientUtils.register(client, cluster2, localIP, i, "ext2");
				VintageNamingClientUtils.register(client, clusterId, localIP, i);
				VintageNamingClientUtils.sleep(100);
			}
			Thread.sleep(3 * HEARTBEATINTERVAL);
			// 验证 clusterId 节点为 10 个， cluster2 的节点为5个
			assertEquals(18, VintageNamingClientUtils.nodeMap.get(serviceId + "_" + clusterId)
					.size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serviceId + "_" + cluster2)
					.size());

			assertEquals(13, client.lookup(cluster2).size());
			assertEquals(18, client.lookup(clusterId).size());

		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testSubTwoCluster");
		} finally {
			for (int i = 1200; i < 1220; i++) {
				VintageNamingClientUtils.unregister(client, cluster2, localIP, i);
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
				VintageNamingClientUtils.sleep(100);
			}
			try {
				VintageNamingWebUtils.deleteCluster(serviceId, cluster2);
			} catch (Exception e) {
				e.printStackTrace();
			}

			VintageNamingClientUtils.unsubscribeChange(client, clusterId);
			VintageNamingClientUtils.unsubscribeChange(client, cluster2);
		}
	}

	/**
	 * 用例同上。 同 cluster不同service 只是前提为：订阅不同service，对应的同样cluster
	 */
	@Test
	public void testSubTwoService() {
		String service2 = getRandomString(10);
		String serKey = service2 + "_" + clusterId;
		addService(service2, type);
		addCluster(service2, clusterId);
		
		wsnodes.add(localIP);
		addWhiteList(service2, wsnodes);
		try {
			// 订阅两 service
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.subscribeNode(client, service2, clusterId);

			// 向两 service注册节点
			for (int i = 1200; i < 1210; i++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, i, "ex");
				VintageNamingClientUtils.sleep(100);
			}
			for (int i = 1200; i < 1205; i++) {
				VintageNamingClientUtils.register(client, service2, clusterId, localIP, i, "extinfo");
				VintageNamingClientUtils.sleep(100);
			}
			Thread.sleep(4 * HEARTBEATINTERVAL);
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serKey).size());

			// 取消注册节点
			for (int i = 1200; i < 1202; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, i);
				VintageNamingClientUtils.unregister(client, service2, clusterId, localIP, i);
				VintageNamingClientUtils.sleep(100);
			}
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			System.out.print(VintageNamingClientUtils.nodeMap.get(serviceKey));
			assertEquals(8, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serKey).size());

			VintageNamingClientUtils.nodeMap.clear();
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1202,
					"dif-extinfo");
			VintageNamingClientUtils.register(client, service2, clusterId, localIP, 1202,
					"dif-extinfo");
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(8, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serKey).size());

			// 取消订阅 service2
			VintageNamingClientUtils.unsubscribeChange(client, service2, clusterId);
			// 继续注册节点
			for (int i = 1210; i < 1220; i++) {
				VintageNamingClientUtils.register(client, service2, clusterId, localIP, i, "ex");
				VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, i, "ex");
				VintageNamingClientUtils.sleep(100);
			}
			Thread.sleep(3 * HEARTBEATINTERVAL);
			// 验证
			assertEquals(18, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serKey).size());

			assertEquals(18, client.lookup(serviceId, clusterId).size());
			assertEquals(13, client.lookup(service2, clusterId).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("error in testSubTwoService");
		} finally {
			for (int i = 1200; i < 1307; i++) {
				VintageNamingClientUtils.unregister(client, service2, clusterId, localIP, i);
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, i);
				VintageNamingClientUtils.sleep(100);
			}
			VintageNamingWebUtils.deleteWhitelist(service2, wsnodes);
			VintageNamingWebUtils.deleteCluster(service2, clusterId);
			VintageNamingWebUtils.deleteService(service2);

			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
			VintageNamingClientUtils.unsubscribeChange(client, service2, clusterId);
		}
	}

	/**
	 * case 14: 先lookup，再订阅，最后进行节点变更
	 * 预期结果：订阅之前lookup，订阅不会引起心跳线程向client端Notify消息;订阅之后的节点变化可以Notify消息
	 */
	@Test
	public void testSubLookup() {
		int port = 1234;
		try {
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port, "");
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);

			Set<NamingServiceNode> nodes = client.lookup(serviceId, clusterId);
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			System.out.print(VintageNamingClientUtils.nodeMap.get(serviceKey));
			assertNull(VintageNamingClientUtils.nodeMap.get(serviceKey));
			assertEquals(1, nodes.size());

			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);
			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
		} catch (Exception e) {
			// TODO: handle exception
			fail("Error in testSubLookup!");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	/**
	 * 订阅不存在的service和cluster 预期：会提示相关信息 结果：正常订阅，没有提示相关信息
	 */
	@Test
	public void testSubServiceNotExist() {
		String serString = "testService1";
		String cluString = "testCluster1";
		try {

			VintageNamingClientUtils.subscribeNode(client, serString, cluString);

			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);

			// service和cluster先增加了，再删除
			VintageNamingWebUtils.addService(serString, type);
			VintageNamingWebUtils.addCluster(serString, cluString);
			VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
			
			VintageNamingClientUtils.subscribeNode(client, serString, cluString);
			VintageNamingWebUtils.deleteCluster(serString, cluString);
			VintageNamingWebUtils.deleteService(serString);

			VintageNamingClientUtils.sleep(6 * HEARTBEATINTERVAL);

			//fail("No exception in subscribe service and cluster");
		} catch (VintageException e) {
			// TODO: handle exception
			e.printStackTrace();
			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS, e.getFactor());
		} finally {
			if (VintageNamingWebUtils.existsService(serString)) {
				if (VintageNamingWebUtils.existCluster(serString, cluString)) {
					VintageNamingWebUtils.deleteCluster(serString, cluString);
				}

				VintageNamingWebUtils.deleteService(serString);
			}
			VintageNamingClientUtils.unsubscribeChange(client, serString, serString);
		}

	}

	/**
	 * 测试点：异常测试
	 * 
	 * 操作1：订阅 cluster为空 操作2：捕获异常后，服务仍然正常
	 * 
	 * 实际：当clusterId = null时，抛出了异常
	 */
	@Test
	public void testSubClusterNull() {
		try {
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());

			VintageNamingClientUtils.unsubscribeAllChange(client, clusterId);

			VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, 1234, "ext");
			Thread.sleep(2 * HEARTBEATINTERVAL);
			System.out.print(VintageNamingClientUtils.currentNodes.size());
			assertEquals(0, VintageNamingClientUtils.currentNodes.size());

			// 订阅 cluster为null的节点变更
			try {
				VintageNamingClientUtils.subscribeNode(client, null);
				fail("not throw exception when subscribe null cluster");
			} catch (VintageException e) {
				e.printStackTrace();
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
						e.getFactor().getErrorCode());
			}

			// 订阅 server为null的节点变更
			try {
				VintageNamingClientUtils.subscribeNode(client, null, clusterId);
				fail("not throw exception when subscribe null server");
			} catch (VintageException e) {
				e.printStackTrace();
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
						e.getFactor().getErrorCode());
			}

			// 订阅listener为null
			try {
				client.subscribeNodeChanges(serviceId, clusterId, null);
				fail("not throw exception when subscribe null server");
			} catch (VintageException e) {
				e.printStackTrace();
				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
						e.getFactor().getErrorCode());
			}

		} catch (Exception e) {
			fail("error in testSubNull");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, 1234);
		}
	}

	/*
	 * 全部取消注册，并删除cluster，是否能向client端推送相关的信息
	 */
	@Test
	public void testunRegisterAllDeleteCluster() {
		String ip = "10.75.14.27";
		Set<String> nodes = new HashSet<String>();
		nodes.add(ip);

		int port = 8001;
		String serverString = "serTest";
		String clusterString = "cluTest";
		String serviceKeyString = serverString + "_" + clusterString;
		try {
			VintageNamingClientUtils.subscribeNode(client, serverString, clusterString);
			addService(serverString, type);
			addCluster(serverString, clusterString);
			addWhiteList(serverString, nodes);

			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.unregister(client, serverString, clusterString, ip, port
						+ i);
				VintageNamingClientUtils.sleep(100);
			}
			VintageNamingClientUtils.sleep(5 * HEARTBEATINTERVAL);
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.register(client, serverString, clusterString, ip, port
						+ i, "extinformation");
				VintageNamingClientUtils.sleep(100);
			}

			VintageNamingClientUtils.sleep(5 * HEARTBEATINTERVAL);
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceKeyString).size());

			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.unregister(client, serverString, clusterString, ip, port+ i);
				VintageNamingClientUtils.sleep(100);
			}
			delWhiteList(serverString, nodes);
			delCluster(serverString, clusterString);
			delService(serverString);

			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

			//assertEquals(10, Utils.nodeMap.get(serviceKeyString).size());
			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKeyString).size());
		} finally {
			if (VintageNamingClientUtils.redis.hgetAll("se." + serverString).size() > 0) {
				for (int i = 0; i < 10; i++) {
					VintageNamingClientUtils.unregister(client, serverString, clusterString, ip, port + i);
					VintageNamingClientUtils.sleep(100);
				}
				VintageNamingWebUtils.deleteWhitelist(serverString, nodes);
				VintageNamingWebUtils.deleteCluster(serverString, clusterString);
				VintageNamingWebUtils.deleteService(serverString);
			}

		}
	}

	@Test
	public void testSubBlankSerClu() {
		String serviceKeyString = serviceId + "_" + clusterId;
		VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
		VintageNamingClientUtils.sleep(2 * HEARTBEATINTERVAL);

		assertTrue(VintageNamingClientUtils.nodeMap.get(serviceKeyString) == null ||
				VintageNamingClientUtils.nodeMap.get(serviceKeyString).isEmpty() ||
				VintageNamingClientUtils.nodeMap.get(serviceKeyString).size() == 0);

	}
	
	@Test
	public void testRegSleep() {
	try {
			/**
			 * 订阅非空的cluster
			 */
			VintageNamingClientUtils.register(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.subscribeNode(client, clusterId);
			//assertEquals(1, Utils.currentNodes.size());
			VintageNamingClientUtils.register(client, clusterId, localIP, 1235);
			VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
			
			assertEquals(2, VintageNamingClientUtils.currentNodes.size());
			
	
	 	} catch (Exception e) {
	 		fail("error in testSubReg");
	 	// System.out.print(e.getMessage());
	 	} finally {
	 		VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
	 		VintageNamingClientUtils.unregister(client, clusterId, localIP, 1235);
	 		VintageNamingClientUtils.unsubscribeChange(client, clusterId);
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
		TestRunnable[] trs = new TestRunnable [50];  
        for(int i = 0; i < 50; i++){  
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
            subscribeTest();  
        }  
    }  
	
	public void subscribeTest() throws Exception {
		System.out.println("===" + Thread.currentThread().getId() + " begin to execute subscribeTest");
		String i = Long.toString(Thread.currentThread().getId());
		StringBuilder builder = new StringBuilder();
		StringBuilder cluster = new StringBuilder();
		try {
			/**
			 * 订阅非空的cluster
			 */
			for (int j = 0; j < 100; j++){
				cluster.setLength(0);
				cluster.append(clusterId).append(i).append(j);
				addCluster(serviceId, cluster.toString());
				VintageNamingClientUtils.subscribeNode(client, cluster.toString());
				VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
				VintageNamingClientUtils.register(client, cluster.toString(), localIP, 1234);
				VintageNamingClientUtils.register(client, cluster.toString(), localIP, 1235, "externinfoA");
				VintageNamingClientUtils.sleep(50);
			}
			VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
			
			/**
			 * 检查client是否收到节点变更通知
			 */
			for (int j = 0; j < 100; j++){
				builder.setLength(0);
				builder.append(serviceId).append("_").append(clusterId).append(i).append(j);
				cluster.setLength(0);
				cluster.append(clusterId).append(i).append(j);
				assertEquals(2, VintageNamingClientUtils.nodeMap.get(builder.toString()).size());
				assertEquals(2, VintageNamingClientUtils.lookup(client, serviceId, cluster.toString()).size());
			}
			
	 	} catch (Exception e) {
	 		fail("error in testSubReg");
	 	// System.out.print(e.getMessage());
	 	} finally {
	 		for (int j = 0; j < 100; j++){
	 			cluster.setLength(0);
				cluster.append(clusterId).append(i).append(j);
			if(VintageNamingWebUtils.existCluster(serviceId, cluster.toString())){
				VintageNamingClientUtils.unregister(client, cluster.toString(), localIP, 1234);
				VintageNamingClientUtils.unregister(client, cluster.toString(), localIP, 1235);
				VintageNamingClientUtils.sleep(50);
			}
	 		}
			VintageNamingClientUtils.sleep(2*HEARTBEATINTERVAL);
			for ( int j = 0; j < 100; j++){
				cluster.setLength(0);
				cluster.append(clusterId).append(i).append(j);
				delCluster(serviceId, cluster.toString());
				VintageNamingClientUtils.unsubscribeChange(client, cluster.toString());
			}
	 	}
	}
}
