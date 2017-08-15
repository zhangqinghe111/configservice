package com.weibo.qa.vintage.online.naming.testcases;

import static org.junit.Assert.*;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 新增304开关，导致lookupforupdate的准确率只有25%，代码写死
 * 测试本用例时，需将开关设置为off，全部通过，设置为on，可能有一部分失败
 * 
 * printf "show resource feature.configserver.lookup.304.random\r\n" | nc ip port
 * */
public class LookupforupdateTest extends BaseTest {
	private int port = 1111;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = "vintage-test-qa-liuyu9-test";
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService." + getRandomString(10);
		init();
	}

	private void init() {
		if (!VintageNamingWebUtils.existsService(serviceId)) {
			VintageNamingWebUtils.addService(serviceId);
		}
		if (!VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
		VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
	}
	
	@After
	public void teardown() {
		VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
	}

	@Test
	public void testRegsiterMultiNode() {
		int keysize = 10;
		try {
			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			// register
			VintageNamingWebUtils.batchregister(serviceId, clusterId, localIP, port,
					serviceId, keysize);
			
			String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			// lookupforupdate
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(
					serviceId, clusterId, oldSign);
			assertEquals(keysize, nodeSet.size());

			nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId,
					newSign);
			assertNull(nodeSet);
		} finally {
			VintageNamingWebUtils.batch_unregister(serviceId, clusterId, localIP, port, keysize);
		}

	}
	
	/**
	 * 测试一次节点变更，lookupforupdate变更成功的概率
	 * */
	@Test
	public void test304lookupforupdateRepeatRatio() {
		String extinfo = getRandomString(20);
		try{
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					serviceId);
			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(
					serviceId, clusterId, oldSign);
			assertNull(nodeSet);
			for (int i = 0; i < 20; i++){
				nodeSet = VintageNamingWebUtils.lookupforupdate(
						serviceId, clusterId, oldSign);
				assertNull(nodeSet);
			}
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port, extinfo);
			String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);
			for (int i = 0; i < 20; i++){
				nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId, oldSign);
				if (nodeSet != null){
					System.out.println("========lookupforupdate repeat: " + i);
					assertEquals(1, nodeSet.size());
					break;
				}
			}
			assertTrue(nodeSet != null);
			
		} catch(Exception e) {} finally{
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
		}
	}

	@Test
	public void testModifyNode() {
		String extinfo = "extInfoAnother";
		try {
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					serviceId);
			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port, extinfo);
			String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(
					serviceId, clusterId, oldSign);
			assertEquals(1, nodeSet.size());

			nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId,
					newSign);
			assertNull(nodeSet);
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);
		}
	}

	@Test
	public void testUnRegsiterMultiNode() {
		int keysize = 10;
		int old_size = VintageNamingWebUtils.lookup_set(serviceId, clusterId).size();
		try {
			// register
			VintageNamingWebUtils.batchregister(serviceId, clusterId, localIP, port,
					serviceId, keysize);

			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			for (int i = 0; i < keysize-1; i++) {
				VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port + i);
				String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);				

				// lookupforupdate
				Set<NamingServiceNode> nodeSet = VintageNamingWebUtils
						.lookupforupdate(serviceId, clusterId, oldSign);
				assertEquals(keysize - 1 - i+old_size, nodeSet.size());

				nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId,
						newSign);
				assertNull(nodeSet);

				oldSign = newSign;
			}
			
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port + (keysize -1));
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils
					.lookupforupdate(serviceId, clusterId, oldSign);
			assertTrue(nodeSet.isEmpty() || nodeSet.size() == old_size);
			
			String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);		
			nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId,
					newSign);
			assertNull(nodeSet);
			
		} finally {
			VintageNamingWebUtils.batch_unregister(serviceId, clusterId, localIP, port, keysize);
		}
	}
}
