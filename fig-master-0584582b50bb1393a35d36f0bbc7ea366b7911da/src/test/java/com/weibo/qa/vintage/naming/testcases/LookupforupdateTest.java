package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
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
//	private NamingServiceClient client;
	private String serviceId;
	private String clusterId;
	private int port = 1111;
	private String noexistSer;
	private String noexistClu;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		noexistSer = getRandomString(10);
		noexistClu = getRandomString(20);
		
//		config.setServiceId(serviceId);
//		client = new NamingServiceClient(config);
//		client.setNodeExciseStrategy(new NodeExciseStrategy.Dynamic());
//		client.start();
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

	private void clear() {
		VintageNamingWebUtils.deleteWhitelist(serviceId, this.localNodes);
		VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
		VintageNamingWebUtils.deleteService(serviceId);
	}

	@After
	public void tearDown() throws Exception {
//		clear();
	}

	@Test
	public void testRegsiterOneNode() {
		try {
			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			// register
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port,
					serviceId);
			String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			// lookupforupdate
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
	public void testRegsiterMultiNode() {
		int keysize = 10;
		try {
			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			// register
			VintageNamingWebUtils.batchregister(serviceId, clusterId, localIP, port, port+keysize, serviceId);
			
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
		try {
			// register
			VintageNamingWebUtils.batchregister(serviceId, clusterId, localIP, port, port+keysize, serviceId);
			
			String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);

			for (int i = 0; i < keysize-1; i++) {
				VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port + i);
				String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);				

				// lookupforupdate
				Set<NamingServiceNode> nodeSet = VintageNamingWebUtils
						.lookupforupdate(serviceId, clusterId, oldSign);
				assertEquals(keysize - 1 - i, nodeSet.size());

				nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId,
						newSign);
				assertNull(nodeSet);

				oldSign = newSign;
			}
			
			VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port + (keysize -1));
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils
					.lookupforupdate(serviceId, clusterId, oldSign);
			assertTrue(nodeSet.isEmpty());
			
			String newSign = VintageNamingWebUtils.getsign(serviceId, clusterId);		
			nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId,
					newSign);
			assertNull(nodeSet);
			
		} finally {
			VintageNamingWebUtils.batch_unregister(serviceId, clusterId, localIP, port, keysize);
		}
	}

	@Test
	public void testNoNode() {
		String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);
		Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId,
				clusterId, oldSign);
		assertNull(nodeSet);
	}

	@Test
	public void testRegUnreg() {
		String sign = VintageNamingWebUtils.getsign(serviceId, clusterId);

		VintageNamingWebUtils.register(serviceId, clusterId, localIP, port, serviceId);
		VintageNamingWebUtils.unregister(serviceId, clusterId, localIP, port);

		Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId,
				clusterId, sign);
		assertNull(nodeSet);
	}

	@Test
	public void testNoExist() {
		String sign = VintageNamingWebUtils.getsign(noexistSer, clusterId);
		try {
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(noexistSer, clusterId, sign);
			System.out.print(nodeSet);
			assertNull(nodeSet);
		} catch (Exception ex) {
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS, ex.getFactor());
		}

		try {
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, noexistClu, sign);
			System.out.print(nodeSet);
			assertNull(nodeSet);
		} catch (Exception ex) {
//			assertEquals(ExcepFactor.E_CLUSTER_ID_NOT_EXISTS, ex.getFactor());
		}

	}

	@Ignore
	@Test
	public void testNull() {
		String oldsign = VintageNamingWebUtils.getsign(serviceId, clusterId);
		paraNull(null, clusterId, oldsign);
		paraNull(serviceId, null, oldsign);
		paraNull(serviceId, clusterId, null);

	}

	@Test
	public void testBlank() {
		String oldsign = VintageNamingWebUtils.getsign(serviceId, clusterId);
		paraNull(" ", clusterId, oldsign);
		paraNull(serviceId, " ", oldsign);
		paraNull(serviceId, clusterId, " ");
	}

	private void paraNull(String service, String cluster, String sign) {
		try {
			Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(service, cluster, sign);
			System.out.print(nodeSet);
			assertNull(nodeSet);
		} catch (VintageException ex) {
			System.out.println(ex.getFactor());
		}
	}

}
