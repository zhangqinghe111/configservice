package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import javax.rmi.CORBA.Util;

//import org.apache.thrift.transport.TFileTransport.truncableBufferedInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.model.NamingServiceNode;
import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConstantsTest;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/*
 * test cases of the hot local cache  for nodes in server
 * 注意：使用服务端的接口lookup进行测试，不要使用client的方法
 */
public class ServerNodeCacheTest extends BaseTest {

	private int serverCacheWriteTime = VintageConstantsTest.Server_Cache_Write_Time;
	private int serverCacheVisitTime = VintageConstantsTest.Server_Cache_Visit_Time;
	private int serverCacheTime;
	private NamingServiceClient client;
	private int port = 1234;
	private String extinfo = clusterId + "/Service";
	private String serviceKeyString = serviceId + "_" + clusterId;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		config.setServiceId(serviceId);
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();

		serverCacheTime = (serverCacheVisitTime < serverCacheWriteTime) ? serverCacheVisitTime
				: serverCacheWriteTime;

		ServerWebUtils.ClusterCache("on");
	}

	@After
	public void tearDown() throws Exception {
		ServerWebUtils.ClusterCache("off");
	}

	protected void init(String service, String cluster) {
		if (!VintageNamingWebUtils.existsService(service)) {
			VintageNamingWebUtils.addService(service, "dynamic");
		}
		if (!VintageNamingWebUtils.existCluster(service, cluster)) {
			VintageNamingWebUtils.addCluster(service, cluster);
		}
		VintageNamingWebUtils.addWhitelist(service, localNodes);
	}

	protected void clean(String service, String cluster) {
		if (VintageNamingWebUtils.existsService(service)) {
			VintageNamingWebUtils.deleteWhitelist(service, localNodes);

			VintageNamingWebUtils.deleteCluster(service, cluster);

			VintageNamingWebUtils.deleteService(service);
		}
	}

	private int GetCacheSubString(String subString, String cacheString) {
		String[] strings = cacheString.split(subString);
		return strings.length - 1;
	}

	private int GetCacheSubString(int port, String cacheString) {
		String[] strings = cacheString.split(String.valueOf(port));
		return strings.length - 1;
	}

	private void checkCacheNull(String cacheData) {
		assertEquals(1, GetCacheSubString("null", cacheData));
	}

	@Test
	public void WriteOperationCacheTest() {
		try {
			// no data in cache, no data in redis;register
			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);
			Map<String, String> redisData = VintageNamingClientUtils.redis
					.hgetAll(serviceKeyString);
			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, redisData.size());
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString("null", cacheData));

			// lookup and check if there is data in cache
			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString(localIP, cacheData));

			// there is data in cache and redis, modify data
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP,
					port + 1, extinfo);
			assertEquals(2, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString("null", cacheData));

			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));
			assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString(port + 1, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString("null", cacheData));

			// no data in cache;modify nodes
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP,
					port + 2, extinfo);
			assertEquals(3, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString("null", cacheData));

			// node data in cache;unregister one node
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP,
					port + 2);
			assertEquals(2, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString("null", cacheData));

			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));
			assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString(port + 1, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));

			// data in cache;data in redis ;unregister one node
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP,
					port + 1);
			assertEquals(1, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);

			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));

			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			assertEquals(0, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);

			// no data in redis and cache;unregister node not exist
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			assertEquals(0, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);

			// service_cluster,nodes:[];lookup
			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(0, GetCacheSubString(port, lookupretValue));
			assertEquals(1, GetCacheSubString("200", lookupretValue));
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP,
					port + 1);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP,
					port + 2);
			clean(serviceId, clusterId);
		}

	}

	/*
	 * 缓存中有service1_cluster1 service2_cluster2 现在注册service1_cluster2
	 */
	@Test
	public void MultiSerCluWriteCacheTest() {
		String service1 = serviceId + 1;
		String service2 = clusterId + 2;
		String cluster1 = clusterId + 1;
		String cluster2 = clusterId + 2;

		String servicekey1 = service1 + "_" + cluster1;
		String servicekey2 = service2 + "_" + cluster2;
		String servicekey3 = service1 + "_" + cluster2;
		try {
			init(service1, cluster1);
			init(service2, cluster2);
			init(service1, cluster2);

			VintageNamingClientUtils.register(client, service1, cluster1, localIP, port, extinfo);
			VintageNamingClientUtils.register(client, service2, cluster2, localIP, port + 1,
					extinfo);

			String cacheData = ServerWebUtils.getservercache(servicekey1);
			checkCacheNull(cacheData);
			cacheData = ServerWebUtils.getservercache(servicekey2);
			checkCacheNull(cacheData);

			VintageNamingWebUtils.lookup(service1, cluster1);
			VintageNamingWebUtils.lookup(service2, cluster2);

			cacheData = ServerWebUtils.getservercache(servicekey1);
			assertEquals(1, GetCacheSubString(port, cacheData));
			cacheData = ServerWebUtils.getservercache(servicekey2);
			assertEquals(1, GetCacheSubString(port + 1, cacheData));

			VintageNamingClientUtils.register(client, service1, cluster2, localIP, port + 2,
					extinfo);

			cacheData = ServerWebUtils.getservercache(servicekey1);
			assertEquals(1, GetCacheSubString(port, cacheData));
			cacheData = ServerWebUtils.getservercache(servicekey2);
			assertEquals(1, GetCacheSubString(port + 1, cacheData));
			cacheData = ServerWebUtils.getservercache(servicekey3);
			checkCacheNull(cacheData);

			VintageNamingClientUtils.unregister(client, service1, cluster2, localIP, port + 2);
			cacheData = ServerWebUtils.getservercache(servicekey1);
			assertEquals(1, GetCacheSubString(port, cacheData));
			cacheData = ServerWebUtils.getservercache(servicekey2);
			assertEquals(1, GetCacheSubString(port + 1, cacheData));
			cacheData = ServerWebUtils.getservercache(servicekey3);
			checkCacheNull(cacheData);
		} finally {
			VintageNamingClientUtils.unregister(client, service1, cluster1, localIP, port);
			VintageNamingClientUtils.unregister(client, service2, cluster2, localIP, port + 1);
			VintageNamingClientUtils.unregister(client, service1, cluster2, localIP, port + 2);

			clean(service2, cluster2);
			VintageNamingWebUtils.deleteCluster(service1, cluster2);
			clean(service1, cluster1);
		}
	}

	@Test
	public void GetOperationCacheTest() {
		// servers and clusters
		int serverSize = 3;
		int clusterSize = 3;
		try {

			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					init(serviceId + i, clusterId + j);
					VintageNamingClientUtils.register(client, serviceId + i, clusterId + j,
							localIP, port, extinfo);
					VintageNamingClientUtils.register(client, serviceId + i, clusterId + j,
							localIP, port + 1, extinfo);
				}
			}

			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					String cacheData = ServerWebUtils
							.getservercache(serviceId + i + "_"
									+ clusterId + j);
					checkCacheNull(cacheData);
				}

			}

			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					String lookupretValue = VintageNamingWebUtils.lookup(serviceId
							+ i, clusterId + j);
					assertEquals(1, GetCacheSubString(port, lookupretValue));
					assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
				}

			}

			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					String cacheData = ServerWebUtils
							.getservercache(serviceId + i + "_"
									+ clusterId + j);
					assertEquals(1, GetCacheSubString(port, cacheData));
					assertEquals(1, GetCacheSubString(port + 1, cacheData));
					assertEquals(0, GetCacheSubString("null", cacheData));
				}

			}

			// data in cache,lookup again
			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					String lookupretValue = VintageNamingWebUtils.lookup(serviceId
							+ i, clusterId + j);
					assertEquals(1, GetCacheSubString(port, lookupretValue));
					assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
				}

			}

			// delete data in redis directly
			VintageNamingClientUtils.redis.del(serviceKeyString);
			assertEquals(0, VintageNamingClientUtils.redis.hgetAll(serviceKeyString).size());
			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					String lookupretValue = VintageNamingWebUtils.lookup(serviceId
							+ i, clusterId + j);
					assertEquals(1, GetCacheSubString(port, lookupretValue));
					assertEquals(1, GetCacheSubString(port + 1, lookupretValue));

					// check the format
					Set<NamingServiceNode> nodeSet = client.lookup(serviceId
							+ i, clusterId + j);
					assertEquals(2, nodeSet.size());
				}

			}

		} finally {
			// TODO: handle exception
			for (int i = 0; i < serverSize; i++) {
				for (int j = 0; j < clusterSize; j++) {
					VintageNamingClientUtils.unregister(client, serviceId + i,
							clusterId + j, localIP, port);
					VintageNamingClientUtils.unregister(client, serviceId + i,
							clusterId + j, localIP, port + 1);
				}
				for (int j = 0; j < clusterSize; j++) {
					VintageNamingWebUtils.deleteCluster(serviceId + i, clusterId + j);
				}
				VintageNamingWebUtils.deleteWhitelist(serviceId + i, localNodes);
				VintageNamingWebUtils.deleteService(serviceId + i);
			}
		}
	}

	@Test
	public void UnregAllDeleteServiceCacheTest() {
		try {
			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);

			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(0, GetCacheSubString(port, lookupretValue));
			assertEquals(1, GetCacheSubString("200", lookupretValue));

			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));

			clean(serviceId, clusterId);

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));

			try {
				lookupretValue = VintageNamingWebUtils.lookup(serviceId,
						clusterId);
			} catch (VintageException ex) {
				assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS,
						ex.getFactor());
			}

			try {
				VintageNamingClientUtils.register(client, serviceId, clusterId, localIP,
						port, extinfo);
			} catch (VintageException ex) {
				assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS,
						ex.getFactor());
			}

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			System.out.println(cacheData);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));

			try {
				VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP,
						port);
			} catch (VintageException ex) {
				assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS,
						ex.getFactor());
			}

		} finally {

		}
	}

	/*
	 * 过期时间
	 */
	@Test
	public void MultiSerCluTimeCacheTest() {
		try {
			System.out.println(serverCacheTime);
			System.out.println(serverCacheTime / 2);

			// 注册一组service和cluster
			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);

			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));
			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			// 过期时间到一半时：注册另一个service和cluster

			String serviceKeyString2 = serviceId + 1 + "_" + clusterId
					+ 1;
			init(serviceId + 1, clusterId + 1);
			VintageNamingClientUtils.register(client, serviceId + 1, clusterId + 1, localIP,
					port + 1, extinfo);
			// 回种
			lookupretValue = VintageNamingWebUtils.lookup(serviceId + 1,
					clusterId + 1);
			assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
			cacheData = ServerWebUtils.getservercache(serviceKeyString2);
			assertEquals(1, GetCacheSubString(port + 1, cacheData));

			// 等待过期时间一半，第一组service和cluster的数据会过期，第二组没有过期
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);
			cacheData = ServerWebUtils.getservercache(serviceKeyString2);
			assertEquals(1, GetCacheSubString(port + 1, cacheData));

			// 等待过期时间一半，第二组service和cluster的数据会过期，
			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);
			cacheData = ServerWebUtils.getservercache(serviceKeyString2);
			checkCacheNull(cacheData);
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			VintageNamingClientUtils.unregister(client, serviceId + 1, clusterId + 1,
					localIP, port + 1);
			clean(serviceId, clusterId);
			clean(serviceId + 1, clusterId + 1);
		}

	}

	@Test
	public void CacheTimeTest() {
		try {
			System.out.println(serverCacheWriteTime);
			System.out.println(serverCacheVisitTime);
			System.out.println(serverCacheTime);

			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);

			// 种缓存
			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));


			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			checkCacheNull(cacheData);
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			clean(serviceId, clusterId);
		}
	}

	/*
	 * 写的过期时间大于读的过期时间
	 */
	@Test
	public void WriteMtVisitCacheTimeTest() {
		try {
			System.out.println(serverCacheWriteTime);
			System.out.println(serverCacheVisitTime);
			System.out.println(serverCacheTime);

			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);

			// 种缓存
			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));


			// lookup again
			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString("null", cacheData));
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			clean(serviceId, clusterId);
		}
	}

	@Test
	public void WriteMtVisitCacheTimeTest2() {
		try {
			System.out.println(serverCacheWriteTime);
			System.out.println(serverCacheVisitTime);
			System.out.println(serverCacheTime);

			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);

			// 种缓存
			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));


			// lookup again
			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			// lookup another time
			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString("null", cacheData));
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			clean(serviceId, clusterId);
		}
	}

	/*
	 * 读的过期时间（15s）大于写的过期时间（10s），小于2倍
	 */
	@Ignore
	@Test
	public void VisitMtWriteCacheTimeTest() {
		try {
			System.out.println(serverCacheWriteTime);
			System.out.println(serverCacheVisitTime);
			System.out.println(serverCacheTime);

			init(serviceId, clusterId);
			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);

			// 种缓存
			String lookupretValue = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString("null", cacheData));

			// lookup again
			lookupretValue = VintageNamingWebUtils.lookup(serviceId, clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(1, GetCacheSubString("null", cacheData));
		} finally {
			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			clean(serviceId, clusterId);
		}
	}

	/*
	 * 结果为：要么返回[]的nodes列表，要么报出serviceid not exist的异常
	 */
	@Test
	public void CacheUnavailableRedisDown() {
		try {
			init(serviceId, clusterId);

			VintageNamingClientUtils.register(client, serviceId, clusterId, localIP, port,
					extinfo);

			VintageNamingWebUtils.lookup(serviceId, clusterId);

			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			assertEquals(1, GetCacheSubString(port, cacheData));


			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);

			RedisWebUtils.StopRedis();

			String lookupRetVal = VintageNamingWebUtils.lookup(serviceId,
					clusterId);
			assertEquals(1, GetCacheSubString("200", lookupRetVal));
			assertEquals(0, GetCacheSubString(port, lookupRetVal));

			cacheData = ServerWebUtils.getservercache(serviceKeyString);
			System.out.println(cacheData);
			assertEquals(0, GetCacheSubString(port, cacheData));
			assertEquals(0, GetCacheSubString("null", cacheData));
		} catch (VintageException ex) {
			System.out.println(ex.getMessage());
			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS, ex.getFactor());
			String cacheData = ServerWebUtils.getservercache(serviceKeyString);
			checkCacheNull(cacheData);
		} finally {
			RedisWebUtils.StartRedis();

			VintageNamingClientUtils.unregister(client, serviceId, clusterId, localIP, port);
			clean(serviceId, clusterId);
		}
	}

	/*
	 * 最大值设置为20
	 */
	@Test
	public void CacheCapacityTest() {
		int serverSize = 21;
		try {
			for (int i = 0; i < serverSize; i++) {
				init(serviceId + i, clusterId);
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
						port, extinfo);
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
						port + 1, extinfo);
			}

			for (int i = 0; i < serverSize; i++) {
				String servicekeyString = serviceId + i + "_" + clusterId;
				String cacheData = ServerWebUtils
						.getservercache(servicekeyString);
				assertEquals(0, GetCacheSubString(port, cacheData));
				assertEquals(0, GetCacheSubString(port + 1, cacheData));
				assertEquals(1, GetCacheSubString("null", cacheData));
			}

			for (int i = 0; i < (serverSize - 1) / 2; i++) {
				String lookupretValue = VintageNamingWebUtils.lookup(
						serviceId + i, clusterId);
				assertEquals(1, GetCacheSubString(port, lookupretValue));
				assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
			}

			int cacheCount = 0;
			for (int i = 0; i < serverSize; i++) {
				String cacheData = ServerWebUtils.getservercache(serviceId
						+ i + "_" + clusterId);

				if (GetCacheSubString(port, cacheData) == 1
						&& GetCacheSubString(port + 1, cacheData) == 1) {
					cacheCount++;
				}

			}

			assertEquals((serverSize - 1) / 2, cacheCount);

			System.out.println("cache for the other 10 services");

			for (int i = (serverSize - 1) / 2; i < (serverSize - 1); i++) {
				String lookupretValue = VintageNamingWebUtils.lookup(
						serviceId + i, clusterId);
				assertEquals(1, GetCacheSubString(port, lookupretValue));
				assertEquals(1, GetCacheSubString(port + 1, lookupretValue));
			}

			cacheCount = 0;

			for (int i = 0; i < serverSize; i++) {
				String cacheData = ServerWebUtils.getservercache(serviceId
						+ i + "_" + clusterId);

				if (GetCacheSubString(port, cacheData) == 1
						&& GetCacheSubString(port + 1, cacheData) == 1) {
					cacheCount++;
				}
			}

			assertEquals(16, cacheCount);

			System.out.println("cache for the last service");

			String lookupretValue = VintageNamingWebUtils.lookup(serviceId
					+ (serverSize - 1), clusterId);
			assertEquals(1, GetCacheSubString(port, lookupretValue));
			assertEquals(1, GetCacheSubString(port + 1, lookupretValue));

			cacheCount = 0;
			for (int i = 0; i < serverSize; i++) {
				String cacheData = ServerWebUtils.getservercache(serviceId
						+ i + "_" + clusterId);

				if (GetCacheSubString(port, cacheData) == 1
						&& GetCacheSubString(port + 1, cacheData) == 1) {
					cacheCount++;
				}

			}
			System.out.println(cacheCount);
			assertEquals(17, cacheCount);

			for (int time = 1; time < 5; time++) {
				System.out.println("cache for the last service" + time);

				lookupretValue = VintageNamingWebUtils.lookup(serviceId + time,
						clusterId);
				assertEquals(1, GetCacheSubString(port, lookupretValue));
				assertEquals(1, GetCacheSubString(port + 1, lookupretValue));

				cacheCount = 0;
				for (int i = 0; i < serverSize; i++) {
					String cacheData = ServerWebUtils
							.getservercache(serviceId + i + "_"
									+ clusterId);
					if (GetCacheSubString(port, cacheData) == 1
							&& GetCacheSubString(port + 1, cacheData) == 1) {
						cacheCount++;
					}

				}
				assertEquals(17, cacheCount);
			}

		} finally {
			for (int i = 0; i < serverSize; i++) {
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId,
						localIP, port);
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId,
						localIP, port + 1);

				VintageNamingWebUtils.deleteCluster(serviceId + i, clusterId);
				VintageNamingWebUtils.deleteWhitelist(serviceId + i, localNodes);
				VintageNamingWebUtils.deleteService(serviceId + i);
			}
		}

	}

	@Ignore
	@Test
	public void CacheBigCapacityTest() {
		int serverSize = 20010;
		try {
			for (int i = 0; i < serverSize; i++) {
				init(serviceId + i, clusterId);
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
						port, extinfo);
				VintageNamingClientUtils.register(client, serviceId + i, clusterId, localIP,
						port + 1, extinfo);
			}

			for (int i = 0; i < serverSize; i++) {
				String servicekeyString = serviceId + i + "_" + clusterId;
				String cacheData = ServerWebUtils
						.getservercache(servicekeyString);
				assertEquals(0, GetCacheSubString(port, cacheData));
				assertEquals(0, GetCacheSubString(port + 1, cacheData));
				assertEquals(1, GetCacheSubString("null", cacheData));
			}

			for (int i = 0; i < serverSize; i++) {
				String lookupretValue = VintageNamingWebUtils.lookup(
						serviceId + i, clusterId);
				System.out.println(lookupretValue);
				assertEquals(2, GetCacheSubString(localIP, lookupretValue));
				assertEquals(1,
						GetCacheSubString(serviceId + i, lookupretValue));
			}

			int cacheCount = 0;
			for (int i = 0; i < serverSize; i++) {
				String cacheData = ServerWebUtils.getservercache(serviceId
						+ i + "_" + clusterId);

				if (GetCacheSubString(port, cacheData) == 1
						&& GetCacheSubString(port + 1, cacheData) == 1) {
					cacheCount++;
				}

			}

			System.out.println(cacheCount);
		} finally {
			for (int i = 0; i < serverSize; i++) {

				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId,
						localIP, port);
				VintageNamingClientUtils.unregister(client, serviceId + i, clusterId,
						localIP, port + 1);

				clean(serviceId + i, clusterId);
			}
		}
	}
}
