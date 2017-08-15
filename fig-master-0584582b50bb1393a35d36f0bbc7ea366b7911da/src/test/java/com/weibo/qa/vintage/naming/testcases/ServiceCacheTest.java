package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
//import org.mockito.InOrder;
//import org.mockito.asm.tree.IntInsnNode;
import org.springframework.instrument.classloading.glassfish.GlassFishLoadTimeWeaver;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.ApacheHttpClient;
import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class ServiceCacheTest extends BaseTest {
	private NamingServiceClient client;
	private String extinfo = "ext";
	private int port = 1234;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		config.setServiceId(serviceId);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
		client.start();
	}

	protected void init() {
		if (!VintageNamingWebUtils.existsService(serviceId)) {
			VintageNamingWebUtils.addService(serviceId);
		}
		if (!VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
		VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	 * check service cache when config server starts; add some service(test1 and
	 * test2 ) before config server starts
	 */
	@Ignore
	@Test
	public void testHasServiceReStartCache() {
		assertEquals(2, VintageNamingClientUtils.redis.hgetAll("gl.service").size());

		Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getServiceInfoSet();
		assertEquals(2, infoSet.size());
	}

	@Test
	public void testInitServiceReStartCache() {
		int gl_services = VintageNamingClientUtils.redis.hgetAll("gl.service").size();

		Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getServiceInfoSet();
		System.out.println(infoSet);
		assertEquals(gl_services, infoSet.size());

		infoSet = VintageNamingWebUtils.getServiceInfoSet();
		assertEquals(gl_services, infoSet.size());
	}

	// check if getservice(),getservice(serviceId),getservice(serviceId&fuzzy)
	// from cache only
	@Test
	public void testGetFromCache() {
		try {
			int gl_services = VintageNamingWebUtils.getServiceInfoSet().size();
			addService(serviceId, NamingServiceType.statics.toString());
			assertEquals(gl_services, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(serviceId, VintageNamingWebUtils.getService(serviceId).getName());
			assertEquals(NamingServiceType.statics,
					VintageNamingWebUtils.getService(serviceId).getType());
			assertEquals(serviceId,
					VintageNamingWebUtils.getService(serviceId + "&fuzzy=false").getName());
			assertNull(VintageNamingWebUtils.getService(serviceId + "&fuzzy=true"));

			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getServiceInfoSet();
			assertEquals(gl_services+1, infoSet.size());
				assertEquals(serviceId,
						VintageNamingWebUtils.getService(serviceId).getName());
				assertEquals(serviceId,
						VintageNamingWebUtils.getService(serviceId + "&fuzzy=false")
								.getName());
				assertEquals(serviceId,
						VintageNamingWebUtils.getService(serviceId + "&fuzzy=true")
								.getName());
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	@Test
	public void testRedisImnormalCacheNormal() {
		int gl_services = VintageNamingWebUtils.getServiceInfoSet().size();
		try {
			for (int i = 0; i < 3; i++) {
				VintageNamingWebUtils.addService(serviceId + i);
			}
			RedisWebUtils.StopRedis();

			assertEquals(gl_services+3, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(serviceId + 0, VintageNamingWebUtils.getService(serviceId + 0)
					.getName());
		} finally {
			RedisWebUtils.StartRedis();
			for (int i = 0; i < 3; i++) {
				VintageNamingWebUtils.deleteService(serviceId + i);
			}
		}
	}

	@Test
	public void testRedisImnormalSleep() {
		int keysize = 3;
		int gl_services = VintageNamingWebUtils.getServiceInfoSet().size();
		try {
			for (int i = 0; i < keysize; i++) {
				if (!VintageNamingWebUtils.existsService(serviceId+i)){
					VintageNamingWebUtils.addService(serviceId + i);
				}
			}
			RedisWebUtils.StopRedis();


			assertEquals(gl_services+3, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(serviceId + 0, VintageNamingWebUtils.getService(serviceId + 0)
					.getName());
		} finally {
			RedisWebUtils.StartRedis();
			for (int i = 0; i < keysize; i++) {
				VintageNamingWebUtils.deleteService(serviceId + i);
			}
		}
	}

	// DEFAULT_MAP_SIZE = 64;
	@Test
	public void testCacheFull() {
		int keysize = 50000;
		int gl_services = VintageNamingWebUtils.getServiceInfoSet().size();
		try{
			for (int i = 0; i < keysize; i++) {
				VintageNamingWebUtils.addService(serviceId + i);
			}
	
	
			assertEquals(gl_services+keysize, VintageNamingWebUtils.getServiceInfoSet().size());
		} finally {
			for (int i = 0; i < keysize; i++) {
				VintageNamingWebUtils.deleteService(serviceId+i);
			}
		}
	}

	@Ignore
	@Test
	public void testHeartBeatDelay() {
		try {
			VintageNamingWebUtils.addService(serviceId, "dynamic");
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
			VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
			VintageNamingClientUtils.subscribeNode(client, serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);
			assertEquals(
					0,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());
			System.out.println("service");
			assertEquals(
					1,
					VintageNamingClientUtils.getWorkingNodeList(client, serviceId, clusterId)
							.size());
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingWebUtils.deleteWhitelist(serviceId, localNodes);
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	@Ignore
	@Test
	public void testRedisConnection() {
		int port = 1234;
		SwitcherUtils.setSwitcher(
				SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		for (int i = 0; i < 50; i++) {
			VintageNamingWebUtils.addService(serviceId + i,"dynamic");
			VintageNamingWebUtils.addWhitelist(serviceId + i, localNodes);
			for (int j = 0; j < 40; j++) {
				VintageNamingClientUtils.subscribeNode(client, serviceId + i, clusterId + j);
				VintageNamingClientUtils.register(client, serviceId + i, clusterId + j,
						localIP, port + j + i, extinfo);
			}
		}
		
		System.out.println(clusterId);
	}
}
