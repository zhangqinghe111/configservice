package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class LocalcacheTest extends BaseTest {

	int startport = 1234;
	String extinfo = "extinfo";

	public void setUp() throws Exception {
		super.setUp();

		serviceId = getRandomString(10);
		clusterId = getRandomString(20);

		RedisWebUtils.StartRedis();

		if (!VintageNamingWebUtils.existsService(serviceId)) {
			VintageNamingWebUtils.addService(serviceId, "dynamic");
		}
		if (!VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
		VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
	}

	public void tearDown() throws Exception {
		super.tearDown();
		RedisWebUtils.StartRedis();
	}

	/*
	 * 
	 * 1. register configuration & no localcache 2. stop redis 3. check if
	 * client can get the configuration
	 */
	@Test
	public void testRegNoLocalcache() {
		RedisWebUtils.StartRedis();

		try {
			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, startport, 10);
			}

			for (int i = 0; i < 5; i++) {
				Set<String> NodeList = VintageNamingWebUtils.lookup_set(serviceId, clusterId + i);
				assertTrue(NodeList.size() == 10);
			}

			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, startport+10, 15);
			}
			RedisWebUtils.StopRedis();

			for (int i = 0; i < 5; i++) {
				Set<String> NodeList = VintageNamingWebUtils.lookup_set(serviceId, clusterId + i);
				assertTrue(NodeList.size() == 15);
			}
		} finally {

			RedisWebUtils.StartRedis();

			for (int i = 0; i < 5; i++) {
				VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, startport, 15);
			}
		}
	}

	/*
	 * update configuration; stop redis; restart redis
	 */
	@Test
	public void testRedisModifyReConnection() {
		try {
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, startport,
					extinfo);

			Set<String> nodeList = VintageNamingWebUtils.lookup_set(serviceId,
					clusterId);
			assertEquals(1, nodeList.size());

			VintageNamingWebUtils.register(serviceId, clusterId, localIP, startport + 1,
					extinfo);

			RedisWebUtils.StopRedis();

			nodeList = VintageNamingWebUtils.lookup_set(serviceId, clusterId);
			assertEquals(1, nodeList.size());

			RedisWebUtils.StartRedis();


			nodeList = VintageNamingWebUtils.lookup_set(serviceId, clusterId);
			assertEquals(2, nodeList.size());

			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP,
					startport + 1);
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, startport);
		} finally {
			RedisWebUtils.StartRedis();

			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP,
					startport + 1);
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, startport);
		}
	}

	@Test
	public void testRedisStopForLongTime() {
		try {
			RedisWebUtils.StartRedis();
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, startport,
					extinfo);

			Set<String> nodeList = VintageNamingWebUtils.lookup_set(serviceId,
					clusterId);
			assertEquals(1, nodeList.size());

			RedisWebUtils.StopRedis();

			nodeList = VintageNamingWebUtils.lookup_set(serviceId, clusterId);
			// size is 1, after long time wating, localcache still exists
			assertEquals(1, nodeList.size());
		} finally {
			RedisWebUtils.StartRedis();
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, startport);
		}
	}

}
