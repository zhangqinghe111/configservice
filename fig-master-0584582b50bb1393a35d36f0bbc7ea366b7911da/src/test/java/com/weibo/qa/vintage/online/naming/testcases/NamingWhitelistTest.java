package com.weibo.qa.vintage.online.naming.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class NamingWhitelistTest extends BaseTest{

	private Set<String> nodes = new HashSet<String>();
	private static String serviceId;
	private static String clusterId;
	
	@BeforeClass
	public static void init(){
//		serviceId = "vintage-test-qa-liuyu9-"+getRandomString(5);
		serviceId = "vintage-test-qa-liuyu9-test";
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService." + getRandomString(10);
		if (! VintageNamingWebUtils.existsService(serviceId)){
			VintageNamingWebUtils.addService(serviceId, NamingServiceType.statics.toString());
		}
		if (! VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
	}
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		nodes.clear();
		VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
	}

	@Test
	public void testAddWl() {
		try {
			int whitelist_nodes = VintageNamingWebUtils.getWhiteList(serviceId).size();
			VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			assertEquals(whitelist_nodes+1, VintageNamingWebUtils.getWhiteList(serviceId).size());
			assertTrue(VintageNamingWebUtils.existsWhitelist(serviceId, localIP));
		} catch (VintageException e) {
			fail("ERROR in testAddWl");
		} finally {
			VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
		}
	}
	
	/**
	 * 将不同的ip 100个，添加到 1个service中的白名单列表 连续删除 exists判断
	 */
	@Test
	public void testAddWlMultiNodes() {
		int wlNum;
		if (VintageNamingWebUtils.getWhiteList(serviceId) == null) {
			wlNum = 0;
		} else {
			wlNum = VintageNamingWebUtils.getWhiteList(serviceId).size();
		}

		String ip = "127.0.0.";
		try {
			for (int i = 1; i < 101; i++) {
				nodes.add(ip + i);
			}

			VintageNamingWebUtils.addWhitelist(serviceId, nodes);
			assertEquals(wlNum + 100, VintageNamingWebUtils.getWhiteList(serviceId).size());
		} catch (VintageException e) {
			fail("ERROR in testAddWlMultiNodes -- should not throw exception");
		} finally {
			VintageNamingWebUtils.deleteWhitelist(serviceId, nodes);
		}
	}
	
	/**
	 * 增加白名单后，所有cluster和port都能注册成功
	 */
	@Test
	public void testWhitelistAllReg() {
		try {
			VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, 10);
			
			for (int i = 1; i < 10; i++) {
				VintageNamingWebUtils.batchregister(serviceId, clusterId + i,
						localIP, 8070, 8090, "");
			}

			for (int i = 1; i < 10; i++) {
				assertEquals(20,
						VintageNamingWebUtils.lookup_set(serviceId, clusterId + i).size());
			}

		} catch (VintageException e) {
			// TODO: handle exception
			e.printStackTrace();
			fail("Error in testWhitelistAllReg");
		} finally {
			for (int i = 1; i < 10; i++) {
				VintageNamingWebUtils.batch_unregister(serviceId, clusterId+i, localIP, 8070, 20);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, 10);

			VintageNamingWebUtils.deleteWhitelist(serviceId, localNodes);
		}
	}
}
