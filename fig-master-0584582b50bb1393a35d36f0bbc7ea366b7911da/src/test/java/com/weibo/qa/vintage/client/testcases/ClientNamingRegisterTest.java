package com.weibo.qa.vintage.client.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.model.NodeStatus;

/**
 * 此类主要测试与注册操作相关的用例
 * 注册时，启动heart线程 heartBeatProxy.sendHeartBeat
 * 
 * @author lingling6
 * 
 * NodeExciseStrategy.Statics() 返回所有working和unreachable节点
 * NodeExciseStrategy.Dynamic() 返回所有working节点
 * NodeExciseStrategy.Ratio() 按照阈值返回至少保留的节点数
 */

public class ClientNamingRegisterTest extends BaseTest {

	private NamingServiceClient client;

	private Set<String> wNode = new HashSet<String>();

	private String regService;
	private String regCluster;
	private String extinfo = "ext";
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		regService = getRandomString(10);
		regCluster = getRandomString(20);
		
		config.setServiceId(regService);
		client = new NamingServiceClient(config);
		//client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
        client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, false);
        init();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
//		clean();
	}

	protected void init() {
		if (!VintageNamingWebUtils.existsService(regService)) {
			VintageNamingWebUtils.addService(regService);
			VintageNamingClientUtils.sleep(serviceCacheInterval);
		}
		if (!VintageNamingWebUtils.existCluster(regService, regCluster)) {
			VintageNamingWebUtils.addCluster(regService, regCluster);
		}
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
		VintageNamingWebUtils.addWhitelist(regService, localNodes);
	}

	protected void clean() {
		for (String nodes : localNodes) {
			if (VintageNamingWebUtils.existsWhitelist(regService, nodes)) {
				VintageNamingWebUtils.deleteWhitelist(regService, nodes);
			}
			VintageNamingClientUtils.sleep(100);
		}
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
		if (VintageNamingWebUtils.existsService(regService) && VintageNamingWebUtils.existCluster(regService, regCluster)) {
			VintageNamingWebUtils.deleteCluster(regService, regCluster);
		}
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
		if (VintageNamingWebUtils.existsService(regService)) {
			VintageNamingWebUtils.deleteService(regService);
		}
	}

	/**
	 * 没有注册节点的cluster，进行lookup 预期结果：为空，不是null
	 */
	@Test
	public void testLookupEmptyClu() {
		try {
			assertEquals(0, VintageNamingClientUtils.lookup(client, regService, regCluster).size());
		} catch (VintageException e) {
			System.out.println(e.getMessage());
			fail("ERROR in testLookupEmptyClu");
		}
	}

	/**
	 * 新建 service cluster 加白名单 --> 注册一个节点
	 */
	@Test
	public void testRegOneNode() {
		try {
			VintageNamingClientUtils.register(client, regService, regCluster, localIP, 1234,
					extinfo);
			assertEquals(1, VintageNamingClientUtils.lookup(client, regService, regCluster).size());
		} catch (VintageException e) {
			fail("error in testRegOneNode  -- should not throw exception");

		} finally {
			VintageNamingClientUtils.unregister(client, regService, regCluster, localIP, 1234);

            VintageNamingClientUtils.sleep(serviceCacheInterval);

			assertEquals(0, VintageNamingClientUtils.lookup(client, regService, regCluster).size());

			System.out.println(VintageNamingClientUtils.lookup(client, regService, regCluster));
		}
	}

	/**
	 * 重复注册： ip port extinfo都相同 操作：重复将一个node注册到相同的service和cluster
	 */
	@Test
	public void testRepeatReg() {
		try {
			// 重复注册多次
			for (int i = 1; i <= 10; i++) {
				VintageNamingClientUtils.register(client, regService, regCluster, localIP, 1234,
						extinfo);
				assertEquals(1, VintageNamingClientUtils.lookup(client, regService, regCluster)
						.size());
			}
		} catch (VintageException e) {
			fail("error in testRepeatReg");
		} finally {
			VintageNamingClientUtils.unregister(client, regService, regCluster, localIP, 1234);
            System.out.print(VintageNamingClientUtils.lookup(client, regService, regCluster));
            VintageNamingClientUtils.sleep(serviceCacheInterval);
			assertEquals(0, VintageNamingClientUtils.lookup(client, regService, regCluster).size());
		}
	}

	/**
	 * 重复注册： ip port相同，extinfo不同 操作：重复将一个node注册到相同的service和cluster
	 */
	@Test
	public void testRepeatRegWithDifExt() {
		String extInfoString = "difExtinfo";
		try {
			VintageNamingClientUtils.register(client, regService, regCluster, localIP, 1234,
					extinfo);
			assertEquals(1, VintageNamingClientUtils.lookup(client, regService, regCluster).size());
			VintageNamingWebUtils.register(regService, regCluster, localIP, 1234,
					extInfoString);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			Set<NamingServiceNode> nodeSet = VintageNamingClientUtils.lookup(client, regService,
					regCluster);
			assertEquals(1, nodeSet.size());

			// node的extinfo信息更改了
			for (NamingServiceNode node : nodeSet) {
				System.out.print(node.getExtInfo());
				assertEquals(extInfoString, node.getExtInfo());
			}
		} catch (VintageException e) {
			fail("error in testRepeatReg");
		} finally {
			VintageNamingClientUtils.unregister(client, regService, regCluster, localIP, 1234);
            VintageNamingClientUtils.sleep(serviceCacheInterval);
			assertEquals(0, VintageNamingClientUtils.lookup(client, regService, regCluster).size());
		}
	}

	/**
	 * 连续将多个 node注册到同一个service的不同cluster
	 */
	@Test
	public void testRegMultiNodeMultiCluster() {
		try {
			// 将十个节点注册到服务
			for (int index = 1; index < 10; index++) {
				VintageNamingWebUtils.addCluster(regService, regCluster+index);
				for (int i = 1200; i < 1210; i++) {
					VintageNamingClientUtils.register(client, regService, regCluster + index, localIP, i, extinfo);
					VintageNamingClientUtils.sleep(100);
				}
			}

			for (int index = 1; index < 10; index++) {
				assertEquals(10,
						VintageNamingClientUtils.lookup(client, regService, regCluster + index)
								.size());
				VintageNamingClientUtils.sleep(100);
			}
		} catch (VintageException ex) {
			fail("ERROR in testRegMultiNode: " + ex.getFactor().getErrorCode() + ex.getMessage());
		} finally {
			// 将十个节点取消注册
			for (int index = 1; index < 10; index++) {
				for (int i = 1200; i < 1210; i++) {
					VintageNamingClientUtils.unregister(client, regService, regCluster + index, localIP, i);
					VintageNamingClientUtils.sleep(100);
				}
			}

            VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);

			for (int index = 1; index < 10; index++) {
				assertEquals(0,
						VintageNamingClientUtils.lookup(client, regService, regCluster + index)
								.size());
				VintageNamingWebUtils.deleteCluster(regService, regCluster + index);
			}
		}
	}

	/**
	 * 多个node注册到不同的service的多个相同cluster
	 */
	@Test
	public void testRegNodeMultiService() {
		try {
			// 将10个node，注册到10个service的5个cluster
			for (int i = 1; i <= 10; i++) {
				if (!VintageNamingWebUtils.existsService(regService + i)) {
					VintageNamingWebUtils.addService(regService + i);
				}
				VintageNamingClientUtils.sleep(100);
			}
			VintageNamingClientUtils.sleep(serviceCacheInterval);
			for (int i = 1; i <= 10; i++) {
				VintageNamingWebUtils.addWhitelist(regService + i, localNodes);
			}
			for (int ser = 1; ser <= 10; ser++) {
				for (int clu = 1; clu <= 5; clu++) {
					VintageNamingWebUtils.addCluster(regService + ser, regCluster + clu);
					for (int i = 1200; i < 1210; i++) {
						VintageNamingClientUtils.register(client, regService + ser, regCluster + clu, localIP, i, extinfo);
						VintageNamingClientUtils.sleep(100);
					}
				}
			}
			// check the node number
			for (int ser = 1; ser <= 10; ser++) {
				for (int clu = 1; clu <= 5; clu++) {
					assertEquals(10, VintageNamingClientUtils.lookup(client, regService + ser, regCluster + clu).size());
					VintageNamingClientUtils.sleep(100);
				}
			}

		} catch (VintageException e) {
			fail("error in testRegNodeMultiService");
		} finally {
		}
	}

	/**
	 * 重复注册 反复注册与取消注册
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testSwitchRegUnreg() {
		try {
			// VintageNamingWebUtils.addService(regService);
			int num = 0;
			for (int i = 0; i < 10; i++) {
				System.out.println(++num);
				VintageNamingClientUtils.register(client, regService, regCluster, localIP, 1235,
						extinfo);
				VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
				assertEquals(1, VintageNamingClientUtils.lookup(client, regService, regCluster)
						.size());
				VintageNamingClientUtils.unregister(client, regService, regCluster, localIP, 1235);
                VintageNamingClientUtils.sleep(serviceCacheInterval);
				assertEquals(0, VintageNamingClientUtils.lookup(client, regService, regCluster)
						.size());
			}
		} catch (VintageException e) {
			fail("error in testSwitchRegUnreg");
		} finally {
			VintageNamingClientUtils.unregister(client, regService, regCluster, localIP, 1235);
		}
	}

	/**
	 * 节点在不同类型service下注册时的默认状态working或者unreachable
	 */
	@Test
	public void testRegisterNodeStatus() {
		String serString = "serStatus";
		String cluString = "cluStatus";
		String serString2 = "serStatus2";
		String cluString2 = "cluStatus2";
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serString)) {
				VintageNamingWebUtils.addService(serString, "statics");
			}
			VintageNamingClientUtils.sleep(serviceCacheInterval);
			VintageNamingWebUtils.addWhitelist(serString, localNodes);
			VintageNamingWebUtils.addCluster(serString, cluString);
			VintageNamingClientUtils.register(client, serString, cluString, localIP, port, extinfo);

			Set<NamingServiceNode> nodesSet = VintageNamingClientUtils.lookup(client, serString,
					cluString);
			for (NamingServiceNode node : nodesSet) {
				assertEquals(NodeStatus.working, node.getNodeStatus());
				VintageNamingClientUtils.sleep(100);
			}

			if (!VintageNamingWebUtils.existsService(serString2)){
				VintageNamingWebUtils.addService(serString2, "dynamic");
			}
			VintageNamingClientUtils.sleep(serviceCacheInterval);
			VintageNamingWebUtils.addWhitelist(serString2, localNodes);
			VintageNamingWebUtils.addCluster(serString2, cluString2);
			VintageNamingClientUtils.register(client, serString2, cluString2, localIP, port,
					extinfo);
			Set<NamingServiceNode> nodesSet2 = VintageNamingClientUtils.lookup(client, serString2,
					cluString2);
			for (NamingServiceNode node : nodesSet2) {
				// System.out.print(node.getNodeStatus()+"\n");
				assertEquals(NodeStatus.unreachable, node.getNodeStatus());
				VintageNamingClientUtils.sleep(100);
			}

		} catch (VintageException ex) {
			System.out.print(ex.getMessage());
			fail("Error in testRegisterNodeStatus");
		} finally {
			if (VintageNamingWebUtils.existsService(serString) && VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingClientUtils.unregister(client, serString, cluString, localIP, port);
			}
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			if (VintageNamingWebUtils.existsService(serString2) && VintageNamingWebUtils.existCluster(serString2, cluString2)) {
				VintageNamingClientUtils.unregister(client, serString2, cluString2, localIP, port);
			}
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			if (VintageNamingWebUtils.existsService(serString) && VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingWebUtils.deleteCluster(serString, cluString);
			}
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			if (VintageNamingWebUtils.existsService(serString2) && VintageNamingWebUtils.existCluster(serString2, cluString2)) {
				VintageNamingWebUtils.deleteCluster(serString2, cluString2);
			}
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			VintageNamingWebUtils.deleteWhitelist(serString, localNodes);
			VintageNamingWebUtils.deleteWhitelist(serString2, localNodes);
			VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			VintageNamingWebUtils.deleteService(serString);
			VintageNamingWebUtils.deleteService(serString2);
		}
	}

	/**
	 * 测试点：不加白名单的节点进行注册会抛出异常 加入白名单后，可正常进行注册
	 */
	@Test
	public void testRegNotInWhiteList() {
		String ip = "10.75.14.28";
		try {
			try {
				// 不在白名单中的节点无法注册
				VintageNamingClientUtils.register(client, regService, regCluster, ip, 1234);
				fail("error in testRegNotInWL");
			} catch (VintageException e) {
//				System.out.print(e.getFactor());
//				assertEquals(
//						ExcepFactor.E_NODE__NOT_IN_WHITELIST.getErrorCode(), e
//								.getFactor().getErrorCode());
			}

			// 增加白名单
			wNode.add(ip);
			VintageNamingWebUtils.addWhitelist(regService, wNode);

			// 增加白名单后，可正常注册
			VintageNamingClientUtils.register(client, regService, regCluster, ip, 1234, extinfo);
		} catch (VintageException e) {
			System.out.print(e.getMessage());
			fail("error in testRegNotInWL");
		} finally {
			VintageNamingClientUtils.unregister(client, regService, regCluster, ip, 1234);
			VintageNamingWebUtils.deleteWhitelist(regService, wNode);
		}
	}

	/**
	 * 测试点：不加白名单的节点进行注册会排除异常，同时对不存在的cluster注册时，也不会增加该cluster
	 */
	@Test
	public void testRegNotInWhiteListNoCluster() {
		String ip = "10.75.14.28";
		String clusterNoExistString = "test/nocluster";
		try {
			// 不在白名单中的节点无法注册
			VintageNamingClientUtils.register(client, serviceId, clusterNoExistString, ip, 1234);
			fail("error in testRegNotInWL");
		} catch (VintageException e) {
//			assertEquals(ExcepFactor.E_NODE__NOT_IN_WHITELIST.getErrorCode(), e
//					.getFactor().getErrorCode());
		}

		// 不存在的cluster依然不存在
		assertFalse(VintageNamingWebUtils.existCluster(regService, clusterNoExistString));
	}

	@Test
	public void testRegNoWhitelistNoCluNoSer() {
		String ip = "10.75.14.28";
		String serviceNoExistString = "test/noservice";
		String clusterNoExistString = "test/nocluster";
		try {
			// 不在白名单中的节点无法注册
			VintageNamingClientUtils.register(client, serviceNoExistString, clusterNoExistString,
					ip, 1234, extinfo);
			fail("error in testRegNotInWL");
		} catch (VintageException e) {
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e
//					.getFactor().getErrorCode());
		}
	}

	/**
	 * service不存在时，节点是否能注册、取消注册、lookup、getsign 预期：提示service不存在
	 */
	@Test
	public void testRegNoService() {
		String serString = "Service_others";
		try {
			VintageNamingClientUtils.register(client, serString, clusterId, localIP, 1234, extinfo);
			fail("Error in testResNoService");
		} catch (VintageException e) {
			System.out.println("register no service throw exception success");
		}

		try {
			VintageNamingClientUtils.unregister(client, serString, clusterId, localIP, 1234);
			fail("Error in testResNoService");
		} catch (VintageException e) {
			System.out.println("unregister no service throw exception success");
		}

		try {
			System.out.println(VintageNamingClientUtils.lookup(client, serString, clusterId));
			fail("Error in testResNoService");
		} catch (VintageException e) {
			System.out.println("lookup no service throw exception success");
		}
	}

	/**
	 * cluster不存在时，节点是否能注册 预期：提示cluster若不存在 本期结果：自动在service下添加相应的cluster
	 */
	@Test
	public void testRegNoCluster() {
		String clusterString = "Cluster_others";
		try {
			VintageNamingClientUtils.register(client, regService, clusterString, localIP, 1234);
		} catch (VintageException e) {
			fail("Error in testResNoCluster");
		} finally {
			VintageNamingClientUtils.unregister(client, regService, clusterString, localIP, 1234);
			VintageNamingWebUtils.deleteCluster(regService, clusterString);
		}
	}

	/**
	 * cluster不存在时，节点是否能取消注册、lookup
	 */
	@Test
	public void testUnregNoCluster() {
		String clusterString = "Cluster_others";
		try {
			VintageNamingClientUtils.unregister(client, regService, clusterString, localIP, 1234);
			fail("Error in testResNoCluster");
		} catch (VintageException e) {
//			assertEquals(ExcepFactor.E_CLUSTER_ID_NOT_EXISTS.getErrorCode(), e
//					.getFactor().getErrorCode());
		}

		try {
			// no cluster, no exception
			Set<NamingServiceNode> nodesSet = VintageNamingClientUtils.lookup(client, regService,
					clusterString);
			assertEquals(0, nodesSet.size());
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_CLUSTER_ID_NOT_EXISTS.getErrorCode(), e
					.getFactor().getErrorCode());
		}
	}

	/**
	 * port参数异常，为负数或者很大的数,注册与取消注册
	 */
	@Test
	public void testInvalidPort() {
		int[] portArray = new int[2];
		portArray[0] = -1234;
		portArray[1] = Integer.MAX_VALUE;
		for (int port : portArray) {
			try {
				VintageNamingClientUtils.register(client, regService, regCluster, localIP, port,
						extinfo);
				VintageNamingClientUtils.sleep(100);
				fail("ERROR in testInvalidPort when port is " + port);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(e instanceof IllegalArgumentException);
			}

			try {
				VintageNamingClientUtils.unregister(client, regService, regCluster, localIP, port);
				VintageNamingClientUtils.sleep(100);
				fail("ERROR in testInvalidPort when port is " + port);
			} catch (Exception e) {
				assertTrue(e instanceof IllegalArgumentException);
			}
		}
	}

	/**
	 * IP为异常的字符串
	 */
	@Test
	public void testInvalidIP() {
		int port = 1234;
		String ipString1 = "10.75.abc.27";
		String ipString2 = "abcd";
		Set<String> ipSet = new HashSet<String>();
		ipSet.add(ipString1);
		ipSet.add(ipString2);

		for (String ipString : ipSet) {
			try {
				VintageNamingClientUtils.register(client, regService, regCluster, ipString, port,
						extinfo);
				VintageNamingWebUtils.deleteWhitelist(regService, ipSet);
				fail("ERROR in testInvalidPort");
			} catch (Exception e) {
				assertTrue(e instanceof IllegalArgumentException);
			} finally {
				try {
					VintageNamingClientUtils.unregister(client, regService, regCluster, ipString,
							port);
					fail("ERROR in testInvalidPort");
				} catch (Exception e) {
					assertTrue(e instanceof IllegalArgumentException);
				}
			}
		}
	}

	/**
	 * 测试：参数设置异常 service或者cluster为null,或者IP 为null
	 */
	@Test
	public void testParamNull() {
		int port = 1234;
		try {
			VintageNamingClientUtils.register(client, null, regCluster, localIP, port, extinfo);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}

		try {
			VintageNamingClientUtils.register(client, regService, null, localIP, port, extinfo);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}

		try {
			VintageNamingClientUtils.register(client, regService, regCluster, null, port, extinfo);
			fail("ERROR in testParamNull");
		} catch (Exception ex) {
			assertTrue(ex instanceof IllegalArgumentException);
		}

		try {
			VintageNamingClientUtils.unregister(client, null, regCluster, localIP, port);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}

		try {
			VintageNamingClientUtils.unregister(client, regService, null, localIP, port);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}

		try {
			VintageNamingClientUtils.unregister(client, regService, regCluster, null, port);
			fail("ERROR in testParamNull");
		} catch (Exception ex) {
			assertTrue(ex instanceof IllegalArgumentException);
		}

		try {
			VintageNamingClientUtils.lookup(client, null, regCluster);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
			System.out.println("========testParamNull======" + ex.getMessage());
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}

		try {
			VintageNamingClientUtils.lookup(client, regService, null);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), ex
					.getFactor().getErrorCode());
		}

	}

	/**
	 * 取消注册不存在的节点
	 */
	@Test
	public void testUnregNodeNoExist() {
		try {
			VintageNamingClientUtils.unregister(client, regService, regCluster, "127.0.0.1", 1111);
		} catch (VintageException e) {
			e.printStackTrace();
			fail("Error in testUnregNodeNoExist");
		}
	}
}
