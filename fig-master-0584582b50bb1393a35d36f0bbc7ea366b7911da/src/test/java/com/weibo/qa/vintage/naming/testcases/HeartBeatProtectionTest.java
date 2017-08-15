package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConstantsTest;

/**
 * 这是谁写的用例这么坑爹！！！！！
 * 心跳服务的保护策略
 * feature.configserver.heartbeat 控制心跳发送，默认为true，若为false则不发送心跳
 * http://10.13.1.134:3355/heartbeatprotection.php?switch=on
 * 服务端心跳保护策略开关
 * 关闭：实时显示node节点状态
 * 打开：workingSize = (int) Math.ceil(clientSize * ratio)，
 * @author liuyu9
 * 2016-03-20 update
 */
public class HeartBeatProtectionTest extends BaseTest {

	private int port = 1234;
	private String extinfo = "extinfo";
	private String dType = "dynamic";
	private NamingServiceClient client = null;
	private double ratio = 0.6;
	
	@BeforeClass
	public static void setupBeforeclass() {
		ServerWebUtils.setHeartbeatHost(VintageConstantsTest.IP+":"+VintageConstantsTest.PORT);
	}

	@Before
	public void setUp() throws Exception {
		
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		serviceId2 = getRandomString(10);
		clusterId2 = getRandomString(20);
		
		super.setUp();
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();	
		
		init(serviceId, clusterId);
		ServerWebUtils.HeartbeatProtection("on");
	}

	@After
	public void tearDown() throws Exception {
//		clean(serviceId, clusterId);
		super.tearDown();
	}

	/**
	 * configserver启动时，节点就不可达，则不应用策略
	 * */
	@Test
	public void AlwaysUnreachableTest() {
		int clientSize = 5;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			closeSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}


			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						0,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}

	@Test
	public void AllWorkingToUnreachableProtectionOnToOffTest() {
		int clientSize = 5;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();
				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId, localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}


			for (NamingServiceClient namingServiceClient : clientList) {
				System.out.println(VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
						serviceId, clusterId));
				assertEquals(5,VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}

			closeSwitch();


			int workingSize = (int) Math.ceil(clientSize * ratio);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}

			System.out.println("close the protection switch");
			ServerWebUtils.HeartbeatProtection("off");
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(0, VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}

		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId, clusterId);
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId, localIP, port++);
			}
			ServerWebUtils.HeartbeatProtection("on");
		}
	}

	@Test
	public void AllWorkingToUnreachableProtectionOffToOnTest() {
		ServerWebUtils.HeartbeatProtection("off");
		int clientSize = 5;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}


			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						5,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}

			closeSwitch();


			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						0,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}

			ServerWebUtils.HeartbeatProtection("on");

			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						0,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}

		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId, localIP, port++);
			}
			ServerWebUtils.HeartbeatProtection("on");
		}
	}

	/*
	 * 关闭开关之前部分node为unreachable，打开开关之后，再将部分node变成unreachable
	 */
	@Test
	public void PartionWorkingToUnreachableProtectionOffToOnTest(){
		try {
		ServerWebUtils.HeartbeatProtection("off");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "5", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "5", "allworking");

		System.out.println(VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId));
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");

		Set<NamingServiceNode> nodeSets_1 = VintageNamingClientUtils.getWorkingNodeList(client,
				serviceId, clusterId);
		assertEquals(5, nodeSets_1.size());

		// 打开开关
		ServerWebUtils.HeartbeatProtection("on");
		closeSwitch();

		Set<NamingServiceNode> nodeSets_2 = VintageNamingClientUtils.getWorkingNodeList(client,
				serviceId, clusterId);
		assertEquals(5, nodeSets_2.size());

		assertEquals(nodeSets_1, nodeSets_2);
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "5", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "5", "clean");
		}
	}

	@Test
	public void TenNodesSixWorkingToUnreachableTest() {
		
		int clientSize = 4;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
		
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "2", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "2", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "3468", "2", "allworking");


		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		ServerWebUtils.stopHeartBeat("2468");
		ServerWebUtils.stopHeartBeat("3468");

		assertEquals(10,
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally{
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.stopHeartBeat("3468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "2", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "2", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "3468", "2", "clean");
			port=1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId, clusterId);
			}
		}
	}

	/*
	 * 新注册4个node，不发送心跳的node的状态会从working变成unreachable
	 */
	@Test
	public void RegisterWorkingTest() {
		int clientSize = 4;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "allworking");
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());

		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// register one of four new nodes
		for (int i = 0; i < 1; i++) {
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port++,
					extinfo);
		}

		assertEquals(getWorkingNodeSize(10) + 1,
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// register the left four new nodes
		for (int i = 0; i < 3; i++) {
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port++,
					extinfo);
		}

		assertEquals(getWorkingNodeSize(10) + 3,
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId, clusterId);
			}
			for (int i = 0; i < 4; i++){
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}
		}
	}

	@Test
	public void RegisterUnreachableTest() {
		int clientSize = 4;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "allworking");
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// register four new unreachable nodes
		// run another case;
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "4", "allunreachable");


		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally{
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "4", "clean");
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId, clusterId);
			}
		}
	}

	/*
	 * 取消注册两个node
	 */
	@Test
	public void UnregisterWorkingTest() {
		int clientSize = 4;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "allworking");
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// unregister four working nodes
		port = 1234;
		for (int i = 0; i < 4; i++) {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
		}


		assertEquals(getWorkingNodeSize(10) - 4,
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");
		}
	}

	/*
	 * 取消注册一个unreachablenode 验证：nodesize是否会变化；再全部取消注册unreachable
	 * nodes,验证是否会变化；再将开关关闭，生成新的unreachable node，观察unreachable node size是多少
	 */
	@Test
	public void UnregisterUnreachableTest() {
		int clientSize = 4;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "allworking");
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// unregister one unreachable nodes
		port = 2 * 1234;
		for (int i = 0; i < 1; i++) {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port + 2);
		}

		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		//将unreachable 的node全部取消注册 
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");

		assertEquals(4, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());

		//剩下的4个nodes 变成unreachable状态，观察unreachable node size是否从0开始 ；如果未清空，是不会再摘除node的
		closeSwitch();

		assertEquals(getWorkingNodeSize(4), VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		} finally {
			port = 1234;
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}

		}
	}

	/*
	 * 註冊unreachable node
	 * 再取消註冊一個working到unreachable的node,验证总数中是否包含unreachable的nodes
	 */
	@Test
	public void ReigterUnreachableUnregisterUnreachableTest() {
		int clientSize = 4;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "allworking");
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// register four new unreachable nodes
		// run another case;
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "4", "allunreachable");


		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		// unregister one working to unreachable nodes
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "1", "clean");
		
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "4", "clean");
			port = 1234;
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}
		}
	}

	@Test
	public void WorkingToUnreachableToWorkingTest() {
		try {
		// unregister 5 nodes working
		working(port, 4, "first");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "2", "allworking");
		

		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		ServerWebUtils.stopHeartBeat("2468");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "2", "allworking");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "4", "clean");
			port = 1234;
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}
		}
	}

	/*
	 * 2 unreachable -> 2 working -> 2 unreachable
	 */
	@Test
	public void WorkingToUnreachableToWorkingToUnreachableTest() {
		try {
		// unregister 5 nodes working
		working(port, 4, "first");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "2", "allworking");
		// run the second test: 2 nodes,working to unreachable,to working
		//ServerWebUtils.runTest("WorkingToUnreachableToWorkingToUnreachableThree");
		//ServerWebUtils.runTest("WorkingToUnreachableToWorkingToUnreachableTwo");
		

		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());

		ServerWebUtils.stopHeartBeat("1234");
		ServerWebUtils.stopHeartBeat("2468");
		
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "allworking");
		
		assertEquals(8,VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());

		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "6", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "4", "clean");
			port = 1234;
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}
		}
	}

	@Test
	public void FiveNodeLimitTest() {
		try {
		working(port, 1, "first");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "allworking");

		assertEquals(5, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());

		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(5),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "clean");
			port = 1234;
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
		}

	}

	@Test
	public void FourNodeLimitTest() {
		try {
		WorkingToUnreachable(port, 4, "extinfo");

		assertEquals(getWorkingNodeSize(4),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			port = 1234;
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}
		}
	}

	@Test
	public void SixNodeLimitTest() {
		try {
		working(port, 2, "first");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "allworking");

		assertEquals(6, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(6),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "clean");
			port = 1234;
			for (int i = 0; i < 4; i++) {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port++);
			}
		}
	}

	/*
	 * 4 nodes and 6 nodes(working to unreachable)
	 */
	@Test
	public void FourSixWorkingTest() {
		try {
		VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "3", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "7", "allworking");
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("2468");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "3", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "7", "clean");
		}
	}

	@Test
	public void SixFourWorkingTest() {
		try {
		VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "3", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "7", "allworking");
		
		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(7, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("2468");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "3", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "7", "clean");
		}
	}

	@Test
	public void FiveFiveWorkingTest() {
		try {
		VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "5", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "5", "allworking");

		assertEquals(10, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("1234");
		assertEquals(getWorkingNodeSize(10), VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		ServerWebUtils.stopHeartBeat("2468");
		assertEquals(getWorkingNodeSize(10),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "5", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "2468", "5", "clean");
		}
	}

	@Test
	public void MultiServiceMultiClusterTestService1() {
		String serviceId2 = "protectionService_2";
		String clusterId2 = "protectionCluster_2";
		VintageNamingClientUtils.subscribeNode(client, serviceId2, clusterId2);
		VintageNamingClientUtils.subscribeNode(client, serviceId2, clusterId);
		try {
		working(port, 1, "service_1");
		ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "allworking");

		assertEquals(5, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());

		init(serviceId2, clusterId2);
		ServerWebUtils.HeartBeatStatus(serviceId2, clusterId2, "2468", "3", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId2, clusterId2, "3468", "5", "allworking");
		//ServerWebUtils.runTest("MultiServiceMultiClusterTestService2");
		//ServerWebUtils.runTest("MultiServiceMultiClusterTestUnreachableService2");
		assertEquals(8, VintageNamingClientUtils
				.getWorkingNodeList(client, serviceId2, clusterId2).size());

		ServerWebUtils.HeartBeatStatus(serviceId2, clusterId, "4468", "6", "allworking");
		ServerWebUtils.HeartBeatStatus(serviceId2, clusterId, "5468", "6", "allworking");
		//ServerWebUtils.runTest("MultiServiceMultiClusterTestService3");
		
		//ServerWebUtils.runTest("MultiServiceMultiClusterTestUnreachableService3");
		assertEquals(12, VintageNamingClientUtils
				.getWorkingNodeList(client, serviceId2, clusterId).size());

		ServerWebUtils.stopHeartBeat("1234");
		ServerWebUtils.stopHeartBeat("3468");
		ServerWebUtils.stopHeartBeat("5468");

		assertEquals(getWorkingNodeSize(5),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId).size());
		assertEquals(getWorkingNodeSize(8),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId2, clusterId2).size());
		assertEquals(getWorkingNodeSize(12),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId2, clusterId).size());


		// unregister one node of one service
		port = 1234;
		VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);


		assertEquals(2, VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
				.size());
		assertEquals(getWorkingNodeSize(8),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId2, clusterId2).size());
		assertEquals(getWorkingNodeSize(12),
				VintageNamingClientUtils.getWorkingNodeList(client, serviceId2, clusterId).size());
		} finally {
			ServerWebUtils.stopHeartBeat("1234");
			ServerWebUtils.stopHeartBeat("2468");
			ServerWebUtils.stopHeartBeat("3468");
			ServerWebUtils.stopHeartBeat("4468");
			ServerWebUtils.stopHeartBeat("5468");
			ServerWebUtils.HeartBeatStatus(serviceId, clusterId, "1234", "4", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId2, clusterId2, "2468", "3", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId2, clusterId2, "3468", "5", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId2, clusterId, "4468", "6", "clean");
			ServerWebUtils.HeartBeatStatus(serviceId2, clusterId, "5468", "6", "clean");
			delWhiteList(serviceId2, localNodes);
			delCluster(serviceId2, clusterId);
			delCluster(serviceId2, clusterId2);
			delService(serviceId2);
		}
	}

	/*
	 * 以下方法为私有方法
	 */
	private void closeSwitch() {
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
	}

	private void openSwitch() {
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
	}

	protected void init(String service, String cluster) {
		addService(service, dType);
		addCluster(service, cluster);
		addWhiteList(service, localNodes);
	}

	protected void clean(String service, String cluster) {
		delWhiteList(service, localNodes);
		delCluster(service, clusterId);
		delService(service);
	}

	private int getWorkingNodeSize(int totalsize) {
		return (int) Math.ceil(totalsize * ratio);
	}

	/*
	 * 以下用例为辅助用例，在用例里另外启动，模拟unreachable node等状态的node
	 */
	@Ignore
	@Test
	public void PartionWorkingToUnreachableProtectionOffToOnWorkingToUnreachableTest() {
		ServerWebUtils.HeartbeatProtection("off");
		WorkingToUnreachable(2 * port, 5, "second");
	}

	@Ignore
	@Test
	public void TwoWorkingToUnreachableTest() {
		WorkingToUnreachable(2 * port, 2, "second");
	}

	@Ignore
	@Test
	public void TwoWorkingToUnreachableTest2() {
		WorkingToUnreachable(3 * port, 2, "third");
	}

	@Ignore
	@Test
	public void TwoWorkingToUnreachableTest3() {
		WorkingToUnreachable(4 * port, 2, "fourth");

	}

	@Ignore
	@Test
	public void RegisterUnreachableNodes() {
		unreachable(3 * port, 4, "newUnreachableNode");
	}

	@Ignore
	@Test
	public void RegisterSixWorkingToUnreachable() {
		WorkingToUnreachable(2 * port, 6, "second");
	}

	@Ignore
	@Test
	public void WorkingToUnreachableToWorkingTwoUnreachable() {
		WorkingToUnreachable(2 * port, 4, "second");
	}

	@Ignore
	@Test
	public void WorkingToUnreachableToWorkingUnreachableToWorkingTwo() {
		WorkingToUnreachableToWorking(3 * port, 2, "third");
	}

	@Ignore
	@Test
	public void WorkingToUnreachableToWorkingToUnreachableThree() {
		WorkingToUnreachable(2 * port, 4, "second");
	}

	@Ignore
	@Test
	public void WorkingToUnreachableToWorkingToUnreachableTwo() {
		WorkingToUnreachableToWorkingToUnreachable(3 * port, 2, "third");
	}

	@Ignore
	@Test
	public void FiveNodeLimitFourTest() {
		WorkingToUnreachable(2 * port, 4, "second");
	}

	@Ignore
	@Test
	public void WorkingToUnreachableTest1() {
		WorkingToUnreachable(1 * port, 4, "extinfo");
	}

	@Ignore
	@Test
	public void WorkingToUnreachableTest2() {
		WorkingToUnreachable(2 * port, 6, "extinfo");
	}

	@Ignore
	@Test
	public void FiveFiveWorkingTestUnreachable1() {
		WorkingToUnreachable(1 * port, 5, "extinfo");
	}

	@Ignore
	@Test
	public void FiveFiveWorkingTestUnreachable2() {
		WorkingToUnreachable(2 * port, 5, "extinfo");
	}

	@Ignore
	@Test
	public void MultiServiceMultiClusterTestService2() {
		serviceId = "protectionService_2";
		clusterId = "protectionCluster_2";
		init(serviceId, clusterId);
		working(2 * port, 3, "service_2");
	}

	@Ignore
	@Test
	public void MultiServiceMultiClusterTestService3() {
		serviceId = "protectionService_2";
		clusterId = "protectionCluster";
		init(serviceId, clusterId);
		working(3 * port, 6, "service_3");
	}

	@Ignore
	@Test
	public void MultiServiceMultiClusterTestUnreachableService1() {
		serviceId = "protectionService";
		clusterId = "protectionCluster";
		WorkingToUnreachable(4 * port, 4, "service_1");
	}

	@Ignore
	@Test
	public void MultiServiceMultiClusterTestUnreachableService2() {
		serviceId = "protectionService_2";
		clusterId = "protectionCluster_2";
		WorkingToUnreachable(5 * port, 5, "service_2");
	}

	@Ignore
	@Test
	public void MultiServiceMultiClusterTestUnreachableService3() {
		serviceId = "protectionService_2";
		clusterId = "protectionCluster";
		WorkingToUnreachable(6 * port, 6, "service_3");
	}

	// 以上用例为辅助用例，在用例里另外启动，模拟unreachable node等状态的node

	// 以下方法难为控制node状态的方法
	private void working(int startPort, int size, String extInformation) {
		port = startPort;
		extinfo = extInformation;
		int clientSize = size;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}

		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}

	private void unreachable(int startPort, int size, String extInformation) {
		port = startPort;
		extinfo = extInformation;
		int clientSize = size;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			closeSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}

		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}

	private void WorkingToUnreachable(int startPort, int size, String extInformation) {
		extinfo = extInformation;
		port = startPort;
		int clientSize = size;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}


			closeSwitch();
		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}

	private void WorkingToUnreachableToWorking(int startPort, int size,
			String extInformation) {
		extinfo = extInformation;
		port = startPort;
		int clientSize = size;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}


			closeSwitch();


			openSwitch();


		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}

	private void WorkingToUnreachableToWorkingToUnreachable(int startPort,
			int size, String extInformation) {
		extinfo = extInformation;
		port = startPort;
		int clientSize = size;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}

			openSwitch();

			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}


			closeSwitch();


			openSwitch();


			closeSwitch();


		} catch (VintageException ex) {
			ex.printStackTrace();
		}
	}
}
