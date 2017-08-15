package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 这个类主要是测试白名单的用例
 * 
 * @author lingling6
 * 
 */
public class WhitelistTest extends BaseTest {
	private String whiteService;
	private String whiteCluster;
	private Set<String> nodes = new HashSet<String>();

	@Before
	public void setUp() throws Exception {
		super.setUp();
		whiteService = getRandomString(10);
		whiteCluster = getRandomString(20);
		
		VintageNamingWebUtils.addService(whiteService);
		ServerWebUtils.whitelist("on");
		
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		nodes.clear();
	}

	/**
	 * 为一个 service 增加一个白名单(本机) exists 判断
	 */
	@Test
	public void testAddWl() {

		nodes.add(localIP);
		try {
			VintageNamingWebUtils.addWhitelist(whiteService, nodes);
			assertEquals(1, VintageNamingWebUtils.getWhiteList(whiteService).size());
			assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, localIP));
		} catch (VintageException e) {
			fail("ERROR in testAddWl");
		} finally {
			VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
		}
	}

	/**
	 * 对某一ip重复添加白名单 以及重复删除 exists 判断
	 */
	@Test
	public void testRepeatAddWl() {
		try {
			nodes.add(localIP);
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.addWhitelist(whiteService, nodes);
				assertEquals(1, VintageNamingWebUtils.getWhiteList(whiteService).size());
				assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, localIP));
			}
		} catch (VintageException e) {
			fail("ERROR in testRepeatAddWl");
		} finally {
			try {
				for (int i = 0; i < 10; i++) {
					VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
				}
				assertTrue(!VintageNamingWebUtils.existsWhitelist(whiteService, localIP));
			} catch (VintageException e2) {
				fail("Error in delete multi times");
			}
		}
	}

	/**
	 * 对某一ip，反复添加、删除白名单 exists判断
	 */
	@Test
	public void testSwitchWl() {
		String ip = "10.210.23.23";
		nodes.add(ip);
		try {
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.addWhitelist(whiteService, nodes);
				assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, ip));
				VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
				assertTrue(!VintageNamingWebUtils.existsWhitelist(whiteService, ip));
			}
		} catch (VintageException e) {
			fail("ERROR in testSwitfchWl -- should not throw exception");
		} finally {
			assertTrue(!VintageNamingWebUtils.existsWhitelist(whiteService, ip));
		}
	}

	/**
	 * 将不同的ip 100个，添加到 1个service中的白名单列表 连续删除 exists判断
	 */
	@Test
	public void testAddWlMultiNodes() {
		int wlNum;
		if (VintageNamingWebUtils.getWhiteList(whiteService) == null) {
			wlNum = 0;
		} else {
			wlNum = VintageNamingWebUtils.getWhiteList(whiteService).size();
		}

		String ip = "127.0.0.";
		try {
			for (int i = 1; i < 101; i++) {
				nodes.add(ip + i);
			}

			VintageNamingWebUtils.addWhitelist(whiteService, nodes);
			assertEquals(wlNum + 100, VintageNamingWebUtils.getWhiteList(whiteService).size());
		} catch (VintageException e) {
			fail("ERROR in testAddWlMultiNodes -- should not throw exception");
		} finally {
			VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
			assertEquals(0, VintageNamingWebUtils.getWhiteList(whiteService).size());
		}
	}

	/**
	 * 将同样的ip，添加到 10个不同的service的白名单列表 exists判断
	 */
	@Test
	public void testAddOneNodeMultiService() {
		String ip = "1.2.3.4";
		nodes.add(localIP);
		nodes.add(ip);

		Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getServiceInfoSet();
		int serviceNum = infoSet.size();

		try {
			VintageNamingWebUtils.batchaddService(whiteService, "statics", 10);

			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.addWhitelist(whiteService + i, nodes);
				// check两个ip均在白名单中
				assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService + i, localIP));
				assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService + i, ip));
				assertEquals(2, VintageNamingWebUtils.getWhiteList(whiteService + i).size());

				// check两个ip不在下一个service的白名单中
				if (i < 9) {
					int j = i + 1;
					assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService + j,
							localIP));
					assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService + j, ip));
					assertEquals(0, VintageNamingWebUtils.getWhiteList(whiteService + j).size());
				}
			}

			// 逐个删除
			for (int i = 0; i < 10; i++) {
				try {
					VintageNamingWebUtils.deleteWhitelist(whiteService + i, nodes);
					assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService + i, ip));
					assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService + i,
							localIP));
					assertEquals(0, VintageNamingWebUtils.getWhiteList(whiteService + i).size());
					if (i < 9) {
						int j = i + 1;
						assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService + j,
								localIP));
						assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService + j, ip));
						assertEquals(2, VintageNamingWebUtils.getWhiteList(whiteService + j)
								.size());
					}

				} catch (VintageException e) {
					fail("ERROR in testAddOneNodeMultiService delete");
				}
			}

		} catch (VintageException e) {
			fail("ERROR in testAddOneNodeMultiService -- should not throw exception");
		} finally {
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.deleteWhitelist(whiteService + i, nodes);
				assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService + i, localIP));
				assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService + i, ip));
				VintageNamingWebUtils.deleteService(whiteService + i);
			}
			assertEquals(serviceNum, VintageNamingWebUtils.getServiceInfoSet().size());
		}
	}

	// 为不存在的service添加删除白名单
	@Test
	public void testWhitelistNoService() {
		nodes.clear();
		nodes.add(localIP);
		String ser = "wlist_test";
		// add
		try {
			VintageNamingWebUtils.addWhitelist(ser, nodes);
			fail("ERROR in add whitelist");
		} catch (VintageException ex) {
//			System.out.println(ex.getFactor().getErrorCode());
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), ex
//					.getFactor().getErrorCode());
		}

		// delete
		try {
			VintageNamingWebUtils.deleteWhitelist(ser, nodes);
			fail("ERROR in delete whitelist");
		} catch (VintageException ex) {
//			System.out.println(ex.getFactor().getErrorCode());
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), ex
//					.getFactor().getErrorCode());
		}

		// get
		try {
			assertEquals(0, VintageNamingWebUtils.getWhiteList(ser).size());
			fail("ERROR in get whitelist");
		} catch (VintageException ex) {
			// TODO: handle exception
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), ex
//					.getFactor().getErrorCode());
		}

		// exist
		try {
			assertFalse(VintageNamingWebUtils.existsWhitelist(ser, localIP));
			fail("ERROR in isExists whitelist");
		} catch (VintageException ex) {
			// TODO: handle exception
//			System.out.println(ex.getFactor().getErrorCode());
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), ex
//					.getFactor().getErrorCode());
		}
	}

	/**
	 * 验证ip：port的node是否能添加成功 exist对于IP:PORT形式的参数会报错，add和delete不会报错
	 */
	@Test
	public void testAddIPWithPort() {
		String ip = "10.75.14.27";
		String ip2 = "10.75.14.28";
		String portString = ":3344";

		Set<String> ipStringSet = new HashSet<String>();
		ipStringSet.add(ip);
		ipStringSet.add(ip2);

		try {
			// 多个IP:PORT形式的node
			for (String ipString : ipStringSet) {
				nodes.add(ipString + portString);
			}
			VintageNamingWebUtils.addWhitelist(whiteService, nodes);
			Set<String> whiteListSet = VintageNamingWebUtils.getWhiteList(whiteService);
			assertEquals(2, whiteListSet.size());

			// 白名单中只有IP，没有port
			for (String ipString : whiteListSet) {
				assertFalse(ipString.contains(portString));
			}

			for (String ipString : ipStringSet) {
				assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, ipString));
				try {
					assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, ipString
							+ portString));
				} catch (VintageException e) {
					// TODO: handle exception
					System.out.println(e.getFactor().getErrorCode());
//					assertEquals(
//							ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), e
//									.getFactor().getErrorCode());
				}

			}

			VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
			assertEquals(0, VintageNamingWebUtils.getWhiteList(whiteService).size());
			for (String ipString : ipStringSet) {
				assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService, ipString));
				// assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService, ipString
				// + portString));
			}

			// 再增加一个IP形式的node
			ipStringSet.add(localIP);
			nodes.add(localIP);
			VintageNamingWebUtils.addWhitelist(whiteService, nodes);
			assertEquals(3, VintageNamingWebUtils.getWhiteList(whiteService).size());
			for (String ipString : ipStringSet) {
				assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, ipString));
				// assertTrue(VintageNamingWebUtils.existsWhitelist(whiteService, ipString
				// + portString));
			}

			VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
			assertEquals(0, VintageNamingWebUtils.getWhiteList(whiteService).size());
			for (String ipString : ipStringSet) {
				assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService, ipString));
				// assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService, ipString
				// + portString));
			}

		} catch (Exception e) {
			// TODO: handle exceptione
			fail("Error in testAddIPWithPort");
		} finally {
			VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
		}
	}

	/**
	 * 非法字符串添加、delete、exists判断
	 */
	@Test
	public void testInvalidNode() {
		String[] invalidString = { "_", ";", "&", "%", "#", "$", "@", "*", "^",
				"~", "(", ")", "\\", "|", "+", "[", "]", "{", "}", "-", "<",
				">", "?", "a", "10.10.10.a", "a.a.a.a", "10.a.a.10",
				"10.10.10", "10.10.10.1000" };
		String excepString = ",";
		for (String ip : invalidString) {
			nodes.clear();
			nodes.add(ip);
			System.out.print(ip);
			try {
				VintageNamingWebUtils.addWhitelist(whiteService, nodes);
				fail("Error in testInvalidNode -- should throw exception");
			} catch (VintageException e) {
				System.out.println("add invalid node throw exception success");
			}
			try {
				VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
				fail("Error in testInvalidNode -- should throw exception");
			} catch (VintageException e) {
				System.out.println("delete invalid node throw exception success");
			}
			// try {
			// VintageNamingWebUtils.existsWhitelist(whiteService, ip);
			// fail("Error in testInvalidNode -- should throw exception");
			// } catch (VintageException e) {
			// assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
			// e.getFactor().getErrorCode());
			// }
		}
	}

	/**
	 * 重复删除不存在的白名单
	 * 
	 */
	@Test
	public void testDeleteNotExistsNode() {
		nodes.add(localIP);
		try {
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
				assertFalse(VintageNamingWebUtils.existsWhitelist(whiteService, localIP));
			}
		} catch (VintageException e) {
			fail("error in testDeleteNotExistsNode");
		}
	}

	/**
	 * 当 nodes 为空时，增加白名单，应该抛出异常
	 */
	@Test
	public void testAddWlWithNoneNode() {
		nodes.clear();
		try {
			VintageNamingWebUtils.addWhitelist(whiteService, nodes);
			fail("error in testAddWlWithNoneNode");
		} catch (VintageException e) {
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(), e
//					.getFactor().getErrorCode());
		} finally {
			try {
				VintageNamingWebUtils.deleteWhitelist(whiteService, nodes);
				fail("error in testAddWlWithNoneNode");
			} catch (VintageException e) {
//				assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR.getErrorCode(),
//						e.getFactor().getErrorCode());
			}
		}
	}

	/**
	 * 增加白名单后，所有cluster和port都能注册成功
	 */
	@Test
	public void testWhitelistAllReg() {
		try {
			VintageNamingWebUtils.addWhitelist(whiteService, localNodes);
			VintageNamingWebUtils.batchaddCluster(whiteService, whiteCluster, 10);
			for (int i = 1; i < 10; i++) {
				VintageNamingWebUtils.batchregister(whiteService, whiteCluster+i, localIP, 8070, 8090, "");
			}

			for (int i = 1; i < 10; i++) {
				assertEquals(20, VintageNamingWebUtils.lookup_set(whiteService, whiteCluster + i).size());
			}

		} catch (VintageException e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println(e.getMessage());
			fail("Error in testWhitelistAllReg");
		} finally {
			for (int i = 1; i < 10; i++) {
				VintageNamingWebUtils.batchunregister(whiteService, whiteCluster+i, localIP, 8070, 8090);
			}
			VintageNamingWebUtils.batchdeleteCluster(whiteService, whiteCluster, 10);

			VintageNamingWebUtils.deleteWhitelist(whiteService, localNodes);
		}
	}

	/**
	 * 删除白名单对于注册的影响 预期结果：抛出E_NODE__NOT_IN_WHITELIST异常
	 */
	@Test
	public void testDelWlstReg() {
		int port = 1234;
		try {
			VintageNamingWebUtils.addWhitelist(whiteService, localNodes);
			VintageNamingWebUtils.addCluster(whiteService, whiteCluster);
			VintageNamingWebUtils.register(whiteService, whiteCluster, localIP, port);
			assertEquals(1, VintageNamingWebUtils.lookup_set(whiteService, whiteCluster)
					.size());

			VintageNamingWebUtils.deleteWhitelist(whiteService, localNodes);
			VintageNamingWebUtils.unregister(whiteService, whiteCluster, localIP, port);
			assertEquals(0, VintageNamingWebUtils.lookup_set(whiteService, whiteCluster)
					.size());

			String result = VintageNamingWebUtils.register(whiteService, whiteCluster, localIP, port);
			assertTrue(result.contains("error"));
		} catch (VintageException ex) {
			// TODO: handle exception
//			assertEquals(ExcepFactor.E_NODE__NOT_IN_WHITELIST.getErrorCode(),
//					ex.getFactor().getErrorCode());
		} finally {
			VintageNamingWebUtils.unregister(whiteService, whiteCluster, localIP, port);
			VintageNamingWebUtils.deleteWhitelist(whiteService, localNodes);
			VintageNamingWebUtils.deleteCluster(whiteService, whiteCluster);
		}
	}

	/**
	 * 对于port为0或者没有port的node，不需要增加白名单既可以注册
	 */
	@Test
	public void testRegNodeNoPort() {
		String ip = "10.74.14.27";
		if (!VintageNamingWebUtils.existCluster(whiteService, whiteCluster)) {
			VintageNamingWebUtils.addCluster(whiteService, whiteCluster);
		}

		int nodeNum = VintageNamingWebUtils.lookup_set(whiteService, whiteCluster).size();
		// 白名单中没有ip
		Set<String> whitelistSet = VintageNamingWebUtils.getWhiteList(whiteService);
		assertEquals(0, whitelistSet.size());

		// 不在白名单中，进行注册
		VintageNamingWebUtils.register(whiteService, whiteCluster, ip, 0);
		int nodeNum1 = VintageNamingWebUtils.lookup_set(whiteService, whiteCluster).size();
		assertEquals(nodeNum + 1, nodeNum1);

		VintageNamingWebUtils.unregister(whiteService, whiteCluster, ip, 0);
		int nodeNum2 = VintageNamingWebUtils.lookup_set(whiteService, whiteCluster).size();
		assertEquals(nodeNum, nodeNum2);
	}

	/**
	 * 白名单开关对于注册的影响
	 */
	@Test
	public void testWhitelistSwitch() {
		assertEquals(0, VintageNamingWebUtils.getWhiteList(whiteService).size());
		Set<String> ips = new HashSet<String>();
		ips.add("127.0.0.1");
		ips.add("127.1.1.1");
		VintageNamingWebUtils.addCluster(whiteService, whiteCluster);
		try {
			for (String ip : ips) {
				ServerWebUtils.whitelist("on");
				try {
					String result = VintageNamingWebUtils.register(whiteService, whiteCluster, ip, 1234);
					assertTrue(result.contains("error"));
				} catch (VintageException e) {
					// TODO: handle exception
				}

				ServerWebUtils.whitelist("off");
				String result = VintageNamingWebUtils.register(whiteService, whiteCluster, ip, 1234);
				assertFalse(result.contains("error"));
			}
		} finally {
			for (String ip : ips) {
				VintageNamingWebUtils.unregister(whiteService, whiteCluster, ip, 1234);
			}
			VintageNamingWebUtils.deleteCluster(whiteService, whiteCluster);
			ServerWebUtils.whitelist("on");
		}
	}

	// parameters check

	@Test
	public void testParamsExistsWhiteList() {
		try {
			VintageNamingWebUtils.existsWhitelist("", localIP);
			fail("Error in testExistsWhiteList");
		} catch (VintageException ex) {
			System.out.println(ex.getFactor());
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		}
	}

	@Test
	public void testParamsExistsWhiteListNodeNull() {
		try {
			VintageNamingWebUtils.existsWhitelist(whiteService, "");
			fail("Error in testExistsWhiteListNodeNull");
		} catch (VintageException ex) {
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		}
	}

	@Test
	public void testParamsAddWhiteList() {
		try {
			VintageNamingWebUtils.addWhitelist("", localIP);
			fail("Error in testAddWhiteList");
		} catch (VintageException ex) {
			System.out.println("===testAddWhiteList===" + "errorCode="
					+ ex.getFactor().getErrorCode() + ",details="
					+ ex.getMessage());
		}
	}

	@Test
	public void testParamsAddWhiteListNodeNull() {
		try {
			VintageNamingWebUtils.addWhitelist(whiteService, "");
			fail("Error in testAddWhiteListNodeNull");
		} catch (VintageException ex) {
			System.out.println("===testAddWhiteListNodeNull===" + "errorCode="
					+ ex.getFactor().getErrorCode() + ",details="
					+ ex.getMessage());
		}
	}

	@Test
	public void testParamsDeleteWhiteList() {
		try {
			VintageNamingWebUtils.deleteWhitelist("", localIP);
			fail("Error in testDeleteWhiteList");
		} catch (VintageException ex) {
			System.out.println("===testDeleteWhiteList===" + "errorCode="
					+ ex.getFactor().getErrorCode() + ",details="
					+ ex.getMessage());
		}
	}

	@Test
	public void testParamsDeleteWhiteListNodeNull() {
		try {
			VintageNamingWebUtils.deleteWhitelist(whiteService, "");
			fail("Error in testDeleteWhiteListNodeNull");
		} catch (VintageException ex) {
			System.out.println("===testDeleteWhiteListNodeNull==="
					+ "errorCode=" + ex.getFactor().getErrorCode()
					+ ",details=" + ex.getMessage());
		}
	}

	@Test
	public void testParamsGetWhiteList() {
		try {
			assertTrue(VintageNamingWebUtils.getWhiteList("").size()==0 || VintageNamingWebUtils.getWhiteList("").isEmpty());
			fail("Error in testGetWhiteList");
		} catch (VintageException ex) {
			System.out.println(ex.getFactor());
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		}
	}

	@Test
	public void testParamsAddWhiteListWithPort() {
		int port = 1234;
		try {
			boolean success = VintageNamingWebUtils.addWhitelist(whiteService,
					localIP + ":" + port);
			assertTrue(success);
		} catch (VintageException ex) {
			System.out.println(ex.getFactor());
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		} finally {
			VintageNamingWebUtils.deleteWhitelist(whiteService, localIP);
		}
	}
}
