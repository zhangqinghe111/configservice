package com.weibo.qa.vintage.online.naming.testcases;

import static org.junit.Assert.*;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;  
import net.sourceforge.groboutils.junit.v1.TestRunnable; 

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.VintageConstantsTest;
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
public class SubscribeTest extends BaseTest {

	private NamingServiceClient client;

	private Set<NamingServiceNode> services = new HashSet();
	private Set<NamingServiceNode> nodes;
	private Set<String> wsnodes = new HashSet<String>();

	private String serviceKey = "";
	private String type = "dynamic";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		serviceId = "vintage-test-qa-liuyu9-test";
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService." + getRandomString(10);
		
		config.setServiceId(serviceId);
		config.setHeartbeatInterval(HEARTBEATINTERVAL);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();

		serviceKey = serviceId + "_" + clusterId;
		if (! VintageNamingWebUtils.existsService(serviceId)){
			VintageNamingWebUtils.addService(serviceId, NamingServiceType.statics.toString());
		}
		if (! VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
		// 增加白名单
		addWhiteList(serviceId, localNodes);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		wsnodes.clear();
		VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
	}
	
	// case6-2: 取消注册最后一个节点，是否能将变更信息推送至client端
	/*
	 * 预期结果：Client端能收到节点取消注册的信息
	 */
	@Test
	public void testSub5() {

		try {
			/**
			 * 防止之前用例的影响，先取消订阅
			 */
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);

			VintageNamingClientUtils.register(client, clusterId, localIP, 1236, "");
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);

			VintageNamingClientUtils.register(client, clusterId, localIP, 1234, "");
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			assertEquals(2, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(2, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());

			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1236);
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(0, VintageNamingClientUtils.lookup(client, serviceId, clusterId).size());
			System.out.println("end.................................");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1234);
			VintageNamingClientUtils.unregister(client, clusterId, localIP, 1236);
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

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
			 */
			for (int i = 1200; i < 1400; i++) {
				VintageNamingClientUtils.register(client, clusterId, localIP, i, "extInfo");
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			for (int i = 1200; i < 1399; i++) {
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

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
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
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
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.subscribeNode(client, clusterId);
			}

			assertTrue(VintageNamingClientUtils.currentNodes.isEmpty());

			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.register(client, clusterId, localIP, i, "extInfo");
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
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
		int num = 50;
		try {
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			for (int i = 0; i < num; i++) {
				VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId + i);
			}
					
			/**
			 * 节点注册
			 */
			for (int i = 0; i < num; i++) {
				VintageNamingClientUtils.register(client, clusterId + i, localIP, 1234, "exterinfoA");
				VintageNamingClientUtils.register(client, clusterId + i, localIP, 1235, "");
			}

			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			System.out.println(VintageNamingClientUtils.currentNodes);
			/**
			 * 检查client是否收到节点变更通知
			 */
			for (int i = 0; i < num; i++) {
				String serKey = serviceId + "_" + clusterId + i;
				assertEquals(2, VintageNamingClientUtils.nodeMap.get(serKey).size());
				assertEquals(2, VintageNamingClientUtils.lookup(client, serviceId, clusterId + i).size());
			}		
		} catch (Exception e) {
			fail("Error in testRepeatSubDifClu");
		} finally {
			for (int i = 0; i < num; i++) {
				if(VintageNamingWebUtils.existCluster(serviceId, clusterId+i)){
					VintageNamingClientUtils.unregister(client, clusterId + i, localIP, 1234);
					VintageNamingClientUtils.unregister(client, clusterId + i, localIP, 1235);
				}
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			for (int i = 0; i < num; i++) {
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
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			//Utils.register(client, clusterId, localIP, 1237, "\"exterinfoA\"");
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
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
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
			}
			for (int i = 1200; i < 1205; i++) {
				VintageNamingClientUtils.register(client, cluster2, localIP, i, "extInfo");
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			// 验证
			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(5, VintageNamingClientUtils.nodeMap.get(serKey).size());

			// 取消注册节点
			for (int i = 1200; i < 1202; i++) {
				VintageNamingClientUtils.unregister(client, cluster2, localIP, i);
				VintageNamingClientUtils.unregister(client, clusterId, localIP, i);
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			assertEquals(8, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
			assertEquals(3, VintageNamingClientUtils.nodeMap.get(serKey).size());

			// 给extinfo赋不同的值，重新注册。（首先清空nodemap，为了证明改变extinfo能向client推送信息）
			VintageNamingClientUtils.nodeMap.clear();
			VintageNamingClientUtils.register(client, cluster2, localIP, 1202, "extInfo-different");
			VintageNamingClientUtils.register(client, clusterId, localIP, 1202,
					"extInfo-different");
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

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
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

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
			}
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

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
	 * case 14: 先lookup，再订阅，最后进行节点变更
	 * 预期结果：订阅之前lookup，订阅不会引起心跳线程向client端Notify消息;订阅之后的节点变化可以Notify消息
	 */
	@Test
	public void testSubLookup() {
		int port = 1234;
		try {
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port, "");
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			Set<NamingServiceNode> nodes = client.lookup(serviceId, clusterId);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			System.out.print(VintageNamingClientUtils.nodeMap.get(serviceKey));
			assertNull(VintageNamingClientUtils.nodeMap.get(serviceKey));
			assertEquals(1, nodes.size());

			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKey).size());
		} catch (Exception e) {
			// TODO: handle exception
			fail("Error in testSubLookup!");
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.unsubscribeChange(client, serviceId, clusterId);
		}
	}

	/*
	 * 全部取消注册，并删除cluster，是否能向client端推送相关的信息
	 */
	@Test
	public void testunRegisterAllDeleteCluster() {
		int port = 8001;
		String serviceKeyString = serviceId + "_" + clusterId;
		try {
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port
						+ i);
			}
			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port + i, "extinformation");
			}
			
			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);


			assertEquals(10, VintageNamingClientUtils.nodeMap.get(serviceKeyString).size());

			for (int i = 0; i < 10; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port+ i);
			}

			VintageNamingClientUtils.sleep(VintageConstantsTest.HEARTBEATINTERVAL);

			//assertEquals(10, Utils.nodeMap.get(serviceKeyString).size());
			assertEquals(0, VintageNamingClientUtils.nodeMap.get(serviceKeyString).size());
		} finally {

		}
	}
}
