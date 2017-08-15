package com.weibo.qa.vintage.online.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class NamingRegisterTest extends BaseTest{

	private String extinfo = "ext";
	private Set<String> wNode = new HashSet<String>();
	private static String serviceId;
	private static String clusterId;
	
	@Before
	public void init() throws Exception{
		super.setUp();
		serviceId = "vintage-test-qa-liuyu9-test";
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService." + getRandomString(10);
		initWhiteList();
		if (! VintageNamingWebUtils.existsService(serviceId)){
			VintageNamingWebUtils.addService(serviceId, NamingServiceType.statics.toString());
		}
		if (! VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		cleanWhitelist();
		VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
	}

	protected void initWhiteList() {
		VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
	}

	protected void cleanWhitelist() {
		for (String nodes : localNodes) {
			if (VintageNamingWebUtils.existsWhitelist(serviceId, nodes)) {
				VintageNamingWebUtils.deleteWhitelist(serviceId, nodes);
			}
		}
	}
	
	/**
	 * 没有注册节点的cluster，进行lookup 预期结果：为空，不是null
	 */
	@Test
	public void testLookupEmptyClu() {
		try {
			assertTrue(VintageNamingWebUtils.lookup_set(serviceId, clusterId) == null || 
					VintageNamingWebUtils.lookup_set(serviceId, clusterId).size() == 0);
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
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, 1234,
					extinfo);
			assertEquals(1, VintageNamingWebUtils.lookup_set(serviceId, clusterId).size());
		} catch (VintageException e) {
			fail("error in testRegOneNode  -- should not throw exception");
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, 1234);
			assertEquals(0, VintageNamingWebUtils.lookup_set(serviceId, clusterId).size());
			System.out.println(VintageNamingWebUtils.lookup_set(serviceId, clusterId));
		}
	}

	/**
	 * 重复注册： ip port extinfo都相同 操作：重复将一个node注册到相同的service和cluster
	 */
	@Test
	public void testRepeatReg() {
		int old_size = VintageNamingWebUtils.lookup_set(serviceId, clusterId).size();
		try {
			// 重复注册多次
			for (int i = 1; i <= 10; i++) {
				VintageNamingWebUtils.register(serviceId, clusterId, localIP, 1234,
						extinfo);
				assertEquals(old_size+1, VintageNamingWebUtils.lookup_set(serviceId, clusterId)
						.size());
			}
		} catch (VintageException e) {
			fail("error in testRepeatReg");
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, 1234);
            System.out.print(VintageNamingWebUtils.lookup_set(serviceId, clusterId));
			assertEquals(old_size, VintageNamingWebUtils.lookup_set(serviceId, clusterId).size());
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
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);

			for (int index = 1; index < 10; index++) {	
				VintageNamingWebUtils.batchregister(serviceId, clusterId+index, localIP, 1200, 1210, extinfo);
			}
			for (int index = 1; index < 10; index++) {
				assertEquals(10,
						VintageNamingWebUtils.lookup_set(serviceId, clusterId + index)
								.size());
			}
		} catch (VintageException ex) {
			fail("ERROR in testRegMultiNode: " + ex.getFactor().getErrorCode() + ex.getMessage());
		} finally {
			// 将十个节点取消注册
			for (int index = 1; index < 10; index++) {
				VintageNamingWebUtils.batch_unregister(serviceId, clusterId + index, localIP, 1200, 10);
			}

			for (int index = 1; index < 10; index++) {
				assertEquals(0,
						VintageNamingWebUtils.lookup_set(serviceId, clusterId + index)
								.size());
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, 10);
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
				String result = VintageNamingWebUtils.register(serviceId, clusterId, ip, 1234);
				assertTrue(result.contains("error"));
			} catch (VintageException e) {
				System.out.print("Info: success in testRegNotInWhiteList");
			}

			// 增加白名单
			wNode.add(ip);
			VintageNamingWebUtils.addWhitelist(serviceId, wNode);

			// 增加白名单后，可正常注册
			String result = VintageNamingWebUtils.register(serviceId, clusterId, ip, 1234, extinfo);
			assertFalse(result.contains("error"));
		} catch (VintageException e) {
			System.out.print(e.getMessage());
			fail("error in testRegNotInWL");
		} finally {
			VintageNamingWebUtils.unregister(serviceId, clusterId, ip, 1234);
			VintageNamingWebUtils.deleteWhitelist(serviceId, wNode);
		}
	}
}

