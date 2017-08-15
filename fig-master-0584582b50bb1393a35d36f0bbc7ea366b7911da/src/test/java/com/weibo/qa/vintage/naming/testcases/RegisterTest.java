package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import com.weibo.vintage.utils.VintageNamingWebUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.weibo.vintage.exception.VintageException;

/**
 * 此类主要测试与注册操作相关的用例
 * 注册时，启动heart线程 heartBeatProxy.sendHeartBeat
 * 
 * @author lingling6
 * 
 */
public class RegisterTest extends BaseTest {

	private Set<String> wNode = new HashSet<String>();
	private String regService;
	private String regCluster;
	private String extinfo = "ext";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		regService = getRandomString(10);
		regCluster = getRandomString(20);
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
		}
		if (!VintageNamingWebUtils.existCluster(regService, regCluster)) {
			VintageNamingWebUtils.addCluster(regService, regCluster);
		}
		VintageNamingWebUtils.addWhitelist(regService, localNodes);
	}

	protected void clean() {
		for (String nodes : localNodes) {
			if (VintageNamingWebUtils.existsWhitelist(regService, nodes)) {
				VintageNamingWebUtils.deleteWhitelist(regService, nodes);
			}
		}
		if (VintageNamingWebUtils.existsService(regService) && VintageNamingWebUtils.existCluster(regService, regCluster)) {
			VintageNamingWebUtils.deleteCluster(regService, regCluster);
		}
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
			assertEquals(0, VintageNamingWebUtils.lookup_set(regService, regCluster).size());
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
			VintageNamingWebUtils.register(regService, regCluster, localIP, 1234,
					extinfo);
			assertEquals(1, VintageNamingWebUtils.lookup_set(regService, regCluster).size());
		} catch (VintageException e) {
			fail("error in testRegOneNode  -- should not throw exception");

		} finally {
			VintageNamingWebUtils.unregister(regService, regCluster, localIP, 1234);


			assertEquals(0, VintageNamingWebUtils.lookup_set(regService, regCluster).size());
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
				VintageNamingWebUtils.register(regService, regCluster, localIP, 1234,
						extinfo);
				assertEquals(1, VintageNamingWebUtils.lookup_set(regService, regCluster)
						.size());
			}
		} catch (VintageException e) {
			fail("error in testRepeatReg");
		} finally {
			VintageNamingWebUtils.unregister(regService, regCluster, localIP, 1234);
            System.out.print(VintageNamingWebUtils.lookup_set(regService, regCluster));
			assertEquals(0, VintageNamingWebUtils.lookup_set(regService, regCluster).size());
		}
	}

	/**
	 * 重复注册： ip port相同，extinfo不同 操作：重复将一个node注册到相同的service和cluster
	 */
	@Test
	public void testRepeatRegWithDifExt() {
		String extInfoString = "difExtinfo";
		try {
			VintageNamingWebUtils.register(regService, regCluster, localIP, 1234,
					extinfo);
			assertEquals(1, VintageNamingWebUtils.lookup_set(regService, regCluster).size());
			VintageNamingWebUtils.register(regService, regCluster, localIP, 1234,
					extInfoString);
			Set<String> nodeSet = VintageNamingWebUtils.lookup_set(regService,
					regCluster);
			assertEquals(1, nodeSet.size());

			// node的extinfo信息更改了
			for (String node : nodeSet) {
				assertTrue(node.contains(extInfoString));
			}
		} catch (VintageException e) {
			fail("error in testRepeatReg");
		} finally {
			VintageNamingWebUtils.unregister(regService, regCluster, localIP, 1234);
			assertEquals(0, VintageNamingWebUtils.lookup_set(regService, regCluster).size());
		}
	}

	/**
	 * 连续将多个 node注册到同一个service的不同cluster
	 */
	@Test
	public void testRegMultiNodeMultiCluster() {
		int num = 10;
		try {
			// 将十个节点注册到服务
			VintageNamingWebUtils.batchaddCluster(regService, regCluster, num);
			for (int index = 1; index < 10; index++) {
				VintageNamingWebUtils.batchregister(regService, regCluster+index, localIP, 1200, 1200+num, "");
			}

			for (int index = 1; index < 10; index++) {
				assertEquals(10,
						VintageNamingWebUtils.lookup_set(regService, regCluster + index)
								.size());
			}
		} catch (VintageException ex) {
			fail("ERROR in testRegMultiNode: " + ex.getFactor().getErrorCode() + ex.getMessage());
		} finally {
			// 将十个节点取消注册
			for (int index = 1; index < 10; index++) {
				VintageNamingWebUtils.batchunregister(regService, regCluster+index, localIP, 1200, 1200+num);
			}

			for (int index = 1; index < 10; index++) {
				assertEquals(0,
						VintageNamingWebUtils.lookup_set(regService, regCluster + index)
								.size());
			}
			VintageNamingWebUtils.batchdeleteCluster(regService, regCluster, num);
		}
	}

	/**
	 * 多个node注册到不同的service的多个相同cluster
	 */
	@Test
	public void testRegNodeMultiService() {
		int num = 10;
		try {
			// 将10个node，注册到10个service的5个cluster
			VintageNamingWebUtils.batchaddService(regService, "statics", num);
			
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.addWhitelist(regService + i, localNodes);
			}
			for (int ser = 0; ser < 10; ser++) {
				VintageNamingWebUtils.batchaddCluster(regService+ser, regCluster, 5);
			}
			for (int ser = 0; ser < 10; ser++) {
				for (int clu = 1; clu <= 5; clu++) {
					VintageNamingWebUtils.batchregister(regService+ser, regCluster+clu, localIP, 1200, 1210, extinfo);
				}
			}
			// check the node number
			for (int ser = 0; ser < 10; ser++) {
				for (int clu = 1; clu <= 5; clu++) {
					assertEquals(10, VintageNamingWebUtils.lookup_set(regService + ser, regCluster + clu).size());
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
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.register(regService, regCluster, localIP, 1235,
						extinfo);
				assertEquals(1, VintageNamingWebUtils.lookup_set(regService, regCluster)
						.size());
				VintageNamingWebUtils.unregister(regService, regCluster, localIP, 1235);
				assertEquals(0, VintageNamingWebUtils.lookup_set(regService, regCluster)
						.size());
			}
		} catch (VintageException e) {
			fail("error in testSwitchRegUnreg");
		} finally {
			VintageNamingWebUtils.unregister(regService, regCluster, localIP, 1235);
		}
	}

	/**
	 * 节点在不同类型service下注册时的默认状态working或者unreachable
	 */
	@Test
	public void testRegisterNodeStatus() {
		String serString = getRandomString(10);
		String cluString = getRandomString(20);
		String serString2 = serString+"2";
		String cluString2 = cluString+"2";
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serString)) {
				VintageNamingWebUtils.addService(serString, "statics");
			}
			VintageNamingWebUtils.addWhitelist(serString, localNodes);
			VintageNamingWebUtils.addCluster(serString, cluString);
			VintageNamingWebUtils.register(serString, cluString, localIP, port, extinfo);

			Set<String> nodesSet = VintageNamingWebUtils.lookup_set(serString,
					cluString, "working");
			assertTrue(nodesSet.size()!=0 && !nodesSet.isEmpty());

			if (!VintageNamingWebUtils.existsService(serString2)){
				VintageNamingWebUtils.addService(serString2, "dynamic");
			}
			VintageNamingWebUtils.addWhitelist(serString2, localNodes);
			VintageNamingWebUtils.addCluster(serString2, cluString2);
			VintageNamingWebUtils.register(serString2, cluString2, localIP, port,
					extinfo);
			Set<String> nodesSet2 = VintageNamingWebUtils.lookup_set(serString2, cluString2, "unreachable");
			assertTrue(nodesSet2.size()!=0 && !nodesSet2.isEmpty());

		} catch (VintageException ex) {
			System.out.print(ex.getMessage());
			fail("Error in testRegisterNodeStatus");
		} finally {
			if (VintageNamingWebUtils.existsService(serString) && VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingWebUtils.unregister(serString, cluString, localIP, port);
			}
			if (VintageNamingWebUtils.existsService(serString2) && VintageNamingWebUtils.existCluster(serString2, cluString2)) {
				VintageNamingWebUtils.unregister(serString2, cluString2, localIP, port);
			}
			if (VintageNamingWebUtils.existsService(serString) && VintageNamingWebUtils.existCluster(serString, cluString)) {
				VintageNamingWebUtils.deleteCluster(serString, cluString);
			}
			if (VintageNamingWebUtils.existsService(serString2) && VintageNamingWebUtils.existCluster(serString2, cluString2)) {
				VintageNamingWebUtils.deleteCluster(serString2, cluString2);
			}
			VintageNamingWebUtils.deleteWhitelist(serString, localNodes);
			VintageNamingWebUtils.deleteWhitelist(serString2, localNodes);
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
			
			// 不在白名单中的节点无法注册
			String result = VintageNamingWebUtils.register(regService, regCluster, ip, 1234, extinfo);
			assertTrue(result.contains("error"));
		
			// 增加白名单
			wNode.add(ip);
			VintageNamingWebUtils.addWhitelist(regService, wNode);

			// 增加白名单后，可正常注册
			result = VintageNamingWebUtils.register(regService, regCluster, ip, 1234, extinfo);
			assertFalse(result.contains("error"));
		} catch (VintageException e) {
			System.out.print(e.getMessage());
			fail("error in testRegNotInWL");
		} finally {
			VintageNamingWebUtils.unregister(regService, regCluster, ip, 1234);
			VintageNamingWebUtils.deleteWhitelist(regService, wNode);
		}
	}

	/**
	 * 测试点：不加白名单的节点进行注册会排除异常，同时对不存在的cluster注册时，也不会增加该cluster
	 */
	@Test
	public void testRegNotInWhiteListNoCluster() {
		String ip = "10.75.14.29";
		String clusterNoExistString = "test/nocluster";
		
		// 不在白名单中的节点无法注册
		String result = VintageNamingWebUtils.register(serviceId, clusterNoExistString, ip, 1234);
		assertTrue(result.contains("error"));

		// 不存在的cluster依然不存在
		assertFalse(VintageNamingWebUtils.existCluster(regService, clusterNoExistString));
	}

	@Test
	public void testRegNoWhitelistNoCluNoSer() {
		String ip = "10.75.14.30";
		String serviceNoExistString = "test/noservice";
		String clusterNoExistString = "test/nocluster";
		// 不在白名单中的节点无法注册
		String result = VintageNamingWebUtils.register(serviceNoExistString, clusterNoExistString,
				ip, 1234, extinfo);
		assertTrue(result.contains("error"));
		
	}

	/**
	 * service不存在时，节点是否能注册、取消注册、lookup、getsign 预期：提示service不存在
	 */
	@Test
	public void testRegNoService() {
		String serString = "Service_others";
		String result = VintageNamingWebUtils.register(serString, clusterId, localIP, 1234, extinfo);
		assertTrue(result.contains("error"));
		
		try{
			VintageNamingWebUtils.unregister(serString, clusterId, localIP, 1234);
			fail("Error: run failed in testRegNoService");
		} catch (Exception e){
			
		}
		
		result = VintageNamingWebUtils.lookup(serString, clusterId);
		assertTrue(result.contains("error"));
	}

	/**
	 * cluster不存在时，节点是否能注册 预期：提示cluster若不存在 本期结果：自动在service下添加相应的cluster
	 */
	@Test
	public void testRegNoCluster() {
		String clusterString = "Cluster_others";
		try {
			String result = VintageNamingWebUtils.register(regService, clusterString, localIP, 1234);
			assertFalse(result.contains("error"));
		} catch (VintageException e) {
			fail("ERROR in testResNoCluster");
		} finally {
			VintageNamingWebUtils.unregister(regService, clusterString, localIP, 1234);
			VintageNamingWebUtils.deleteCluster(regService, clusterString);
		}
	}

	/**
	 * cluster不存在时，节点是否能取消注册、lookup
	 */
	@Test
	public void testUnregNoCluster() {
		String clusterString = "Cluster_others";
		
		try{
			VintageNamingWebUtils.unregister(regService, clusterString, localIP, 1234);
			fail("Error: run failed in testUnregNoCluster");
		}catch(Exception e){
			
		}
	
		// no cluster, no exception
		Set<String> nodesSet = VintageNamingWebUtils.lookup_set(regService, clusterString);
		assertEquals(0, nodesSet.size());
		
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
			String result = VintageNamingWebUtils.register(regService, regCluster, localIP, port,
					extinfo);
			assertTrue(result.contains("error"));
			try{
				VintageNamingWebUtils.unregister(regService, regCluster, localIP, port);
				fail("Error: run failed in testInvalidPort");
			} catch(Exception e){
				
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
				String result = VintageNamingWebUtils.register(regService, regCluster, ipString, port,
						extinfo);
				assertTrue(result.contains("error"));
				VintageNamingWebUtils.deleteWhitelist(regService, ipSet);
				fail("Error: run fail in testInvalidIP");
			} catch (Exception e) {
				
			} finally {
				try {
					VintageNamingWebUtils.unregister(regService, regCluster, ipString,
							port);
					fail("ERROR in testInvalidPort");
				} catch (Exception e) {
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
		
		String result = VintageNamingWebUtils.register(null, regCluster, localIP, port, extinfo);
		assertTrue(result.contains("error"));
			
		result = VintageNamingWebUtils.register(regService, null, localIP, port, extinfo);
		assertFalse(result.contains("error"));

		result = VintageNamingWebUtils.register(regService, regCluster, null, port, extinfo);
		assertTrue(result.contains("error"));
	
		try {
			VintageNamingWebUtils.unregister(null, regCluster, localIP, port);
			fail("ERROR in testParamNull");
		} catch (VintageException ex) {
		}

		try {
			VintageNamingWebUtils.unregister(regService, null, localIP, port);
		} catch (VintageException ex) {
			fail("ERROR in testParamNull");
		}

		try {
			VintageNamingWebUtils.unregister(regService, regCluster, null, port);
			fail("ERROR in testParamNull");
		} catch (Exception ex) {
		}

		result = VintageNamingWebUtils.lookup(null, regCluster);
		assertTrue(result.contains("error"));

		result = VintageNamingWebUtils.lookup(regService, null);
		assertFalse(result.contains("error"));
	}

	/**
	 * 取消注册不存在的节点
	 */
	@Test
	public void testUnregNodeNoExist() {
		try {
			VintageNamingWebUtils.unregister(regService, regCluster, "127.0.0.1", 1111);
		} catch (VintageException e) {
			e.printStackTrace();
			fail("Error in testUnregNodeNoExist");
		}
	}
}
