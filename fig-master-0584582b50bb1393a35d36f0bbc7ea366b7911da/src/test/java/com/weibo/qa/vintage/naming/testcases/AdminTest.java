package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.model.NamingServiceCluster;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.VintageNamingWebUtils;
import com.weibo.qa.vintage.naming.testcases.BaseTest;

/**
 * 这个类主要是测试管理接口的用例 该类使用的是core工程中的相关函数 不是通过httpclient调用
 *  添加服务后需要5s的更新时间，服务端confs可配
 *  
 * @author lingling6
 * 
 */
public class AdminTest extends BaseTest {

	private String serviceId;
	private String clusterId;
	private String serviceId2;
	private Boolean cached = false;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		serviceId2 = getRandomString(10);
		clusterId2 = getRandomString(20);		
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * 添加一个serviceId,缓存中是ok，redis中数据是ok
	 * 
	 */
	@Test
	public void testAddOneService() {
		try {
			VintageNamingWebUtils.addService(serviceId, cached);
			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getServiceInfoSet();
			assertFalse(infoSet.toString().contains(serviceId));
			sleep(serviceCacheInterval);
			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			assertTrue(infoSet.toString().contains(serviceId));
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	/**
	 * 重复增加serviceId 重复增加一个serviceId 10 次,serviceIdID相同类型相同；serviceIdID相同类型不同
	 */
	@Test
	public void testRepeatAddService() {
		Set<NamingServiceInfo> infoSet;
		try {
			VintageNamingWebUtils.addService(serviceId, cached);
			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			assertFalse(infoSet.toString().contains(serviceId));
			VintageNamingWebUtils.addService(serviceId);
			fail("error in testRepeatAddService");
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS.getErrorCode(), e
					.getFactor().getErrorCode());
		}

		try {
			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			assertFalse(infoSet.toString().contains(serviceId));
			VintageNamingWebUtils.addService(serviceId, "dynamic", cached);
			fail("error in add the same serviceId with different type");
		} catch (VintageException e2) {
			assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS.getErrorCode(), e2
					.getFactor().getErrorCode());
		}

		// there are data in serviceId cache;then add the same serviceId repeatedly
		try {
			assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
			assertEquals(serviceId, VintageNamingWebUtils.getService(serviceId).getName());
			assertEquals("statics", VintageNamingWebUtils.getService(serviceId).getType()
					.toString());

			VintageNamingWebUtils.addService(serviceId, "dynamic", cached);
			fail("error in add the same serviceId with different type");
		} catch (VintageException e2) {
			assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS.getErrorCode(), e2
					.getFactor().getErrorCode());
		}

		finally {
			assertEquals("statics", VintageNamingWebUtils.getService(serviceId).getType()
					.toString());
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	/**
	 * 反复增加/删除一个 serviceId 10次,下行接口调用结果，主从同步时间
	 */
	@Test
	public void testSwitchService() {

		for (int i = 0; i < 2; i++) {
			VintageNamingWebUtils.addService(serviceId, cached);
			//assertEquals(1, VintageNamingWebUtils.getServiceInfoSet().size());
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
			VintageNamingWebUtils.deleteService(serviceId);
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
		}

		// cache exists
		for (int i = 0; i < 2; i++) {
			VintageNamingWebUtils.addService(serviceId);
			assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
			VintageNamingWebUtils.deleteService(serviceId);
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
		}

		try {
			for (int i = 0; i < 2; i++) {
				VintageNamingWebUtils.addService(serviceId, cached);
				assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
				VintageNamingWebUtils.deleteService(serviceId);
			}
			fail("");
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS, ex.getFactor());
		}

	}

	/**
	 * 连续添加 100 个服务， 连续删除 99个服务,删除最后一个服务
	 */
	@Test
	public void testContinueControllService() {
		int serviceIdNum = 0;
		Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getServiceInfoSet();
		try {
			for (int i = 0; i < 10; i++) {
				if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2+i)) {
					VintageNamingWebUtils.addService(serviceId2 + i);
				}
			}

			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2+0));
			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			for (int i = 0; i < 10; i++) {
				assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2+0));
			}

			for (int i = 0; i < 9; i++) {
				VintageNamingWebUtils.deleteService(serviceId2 + i);
			}

			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			for (int i = 0; i < 10; i++) {
				assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2+0));
			}
			infoSet = VintageNamingWebUtils.getServiceInfoSet();
			for (int i = 0; i < 9; i++) {
				assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2+0));
			}
			assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2+9));
			
		} finally {
			try{
			if (VintageNamingWebUtils.getServiceInfoSet() != null) {
				for (int i = 0; i < 10; i++) {
					VintageNamingWebUtils.deleteService(serviceId2 + i);
				}
			}
			} catch (Exception e) {
				
			}
		}
	}

	/**
	 * 参数类型测试: addserviceId时定义好type,缓存和redis中都ok
	 */
	@Test
	public void testAddServiceWithType() {
		try {
			VintageNamingWebUtils.addService(serviceId, "dynamic", cached);
			VintageNamingWebUtils.addService(serviceId2, "statics", cached);
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
			assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId2));
			sleep(serviceCacheInterval);

			assertEquals(NamingServiceType.dynamic, VintageNamingWebUtils.getService(serviceId)
					.getType());
			assertEquals(NamingServiceType.statics, VintageNamingWebUtils.getService(serviceId2)
					.getType());
			for (NamingServiceInfo serviceIdInfo : VintageNamingWebUtils.getServiceInfoSet()) {
				if (serviceIdInfo.getName() == serviceId2) {
					assertEquals(NamingServiceType.statics,
							serviceIdInfo.getType());
				}
				if (serviceIdInfo.getName() == serviceId) {
					assertEquals(NamingServiceType.dynamic,
							serviceIdInfo.getType());
				}
			}
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
			VintageNamingWebUtils.deleteService(serviceId2);
		}
	}

	/**
	 * 重复删除serviceId
	 */
	@Test
	public void testRepeatDelService() {
		try {

			try {
				// 从来就没有缓存时
				if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
					VintageNamingWebUtils.addService(serviceId, cached);
				}
				assertFalse(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));
				VintageNamingWebUtils.deleteService(serviceId);
				VintageNamingWebUtils.deleteService(serviceId);
				//fail("ERROR in testRepeatDelService");
			} catch (VintageException e) {
				
			}

			try {
				// 第二种情况 没有缓存：有缓存，缓存被更新
				VintageNamingWebUtils.addService(serviceId, cached);
				sleep(serviceCacheInterval);
				assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));

				VintageNamingWebUtils.deleteService(serviceId);
				VintageNamingWebUtils.deleteService(serviceId);
				//fail("ERROR in testRepeatDelService");
			} catch (VintageException e) {
				
			}

			try {
				// 缓存仍然存在时，不会报异常
				VintageNamingWebUtils.addService(serviceId);
				assertTrue(VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId));

				VintageNamingWebUtils.deleteService(serviceId);
				VintageNamingWebUtils.deleteService(serviceId);
				//fail("ERROR in testRepeatDelService");
			} catch (VintageException e) {
				
			}

		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}

	/**
	 * 删除一个不存在的serviceId
	 */
	@Test
	public void testDeleteServiceNotExist() {
		try {
			if (VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
			VintageNamingWebUtils.deleteService(serviceId);
			fail("ERROR in testDeleteServiceNotExist");
		} catch (VintageException e) {
			System.out.println(e.getFactor().getErrorCode());
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e
//					.getFactor().getErrorCode());
		}
	}

	/**
	 * serviceId增加了白名单，是否能删除对应的serviceId
	 */
	@Test
	public void testDeleteServiceHasWlist() {
		try {
			if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			if (!VintageNamingWebUtils.getCluster(serviceId).toString().contains(clusterId)) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId);
			}
			VintageNamingWebUtils.addWhitelist(serviceId, localNodes);
			VintageNamingWebUtils.deleteService(serviceId);
			fail("ERROR in testDeleteServiceHasWlist");
		} catch (VintageException e) {
			e.printStackTrace();
			//assertEquals(ExcepFactor.E_REMOVE_SERVICE_HAS_WHTIENODE.getErrorCode(),e.getFactor().getErrorCode());
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId) && VintageNamingWebUtils.existCluster(serviceId, clusterId)){
			VintageNamingWebUtils.deleteWhitelist(serviceId, localNodes);
			}
			if (VintageNamingWebUtils.existsService(serviceId) && VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			}
			if (VintageNamingWebUtils.existsService(serviceId)) {
			VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}

	/**
	 * 删除有clusterId的serviceId
	 */
	@Test
	public void testDeleteServiceHasCluster() {
		try {
			if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			for (int i = 0; i < 5; i++){
				addCluster(serviceId, clusterId+i);
			}
			VintageNamingWebUtils.deleteService(serviceId);
			fail("ERROR IN testDeleteServiceHasCluster --- should throw exception");
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_REMOVE_SERVICE.getErrorCode(), e
					.getFactor().getErrorCode());
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				for (NamingServiceCluster clu : VintageNamingWebUtils.getCluster(serviceId)) {
					System.out.println(clu.toString());
					VintageNamingWebUtils.deleteCluster(serviceId, clu.toString());
				}
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}

	/**
	 * 非法 serviceId add get delete
	 */
	@Test
	public void testInvalidService() {

		String serviceId = "&&&";
		// add 不合法的serviceId
		try {
			VintageNamingWebUtils.addService(serviceId);
			VintageNamingWebUtils.deleteService(serviceId);
			fail("should throw exception");
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, e.getFactor());

		}
	}

	/**
	 * updateserviceId接口,serviceId从static变为dynamic
	 * @bug update直接更新了内存。。。
	 */
	@Test
	public void testUpdateServiceToDynamic() {
		try {
			// 没有缓存
			VintageNamingWebUtils.addService(serviceId, "statics", cached);
			VintageNamingWebUtils.updateService(serviceId, "dynamic");
			assertEquals(NamingServiceType.dynamic, VintageNamingWebUtils.getService(serviceId)
					.getType());
			//System.out.println(VintageNamingWebUtils.getServiceInfoSet());
			//assertNull(VintageNamingWebUtils.getServiceInfoSet());

			// 有缓存
			VintageNamingWebUtils.addService(serviceId2, "statics", cached);
			VintageNamingWebUtils.updateService(serviceId2, "dynamic");
			assertEquals(NamingServiceType.statics, VintageNamingWebUtils.getService(serviceId2)
					.getType());
			assertEquals(NamingServiceType.dynamic, VintageNamingWebUtils.getService(serviceId2)
					.getType());
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
			VintageNamingWebUtils.deleteService(serviceId2);
		}
	}

	/**
	 * updateserviceId接口,serviceId从dynamic变为static
	 */
	@Test
	public void testUpdateServiceToStatics() {

		try {
			VintageNamingWebUtils.addService(serviceId, "dynamic", cached);
			assertEquals(NamingServiceType.dynamic.toString(), 
					VintageNamingWebUtils.getService(serviceId).getType().toString());
			VintageNamingWebUtils.updateService(serviceId, "statics");
			for (int i = 0; i < 10; i++){
				System.out.println(VintageNamingWebUtils.getService(serviceId).getType().toString());
			}
			assertEquals(NamingServiceType.statics.toString(), 
					VintageNamingWebUtils.getService(serviceId).getType().toString());
		} catch (Exception e) {
			fail("Error in testUpdateServiceToStatics()");
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
		}

	}

	/**
	 * 不存在的serviceId进行updateserviceId
	 */
	@Test
	public void testUpdateNotExistsSer() {
		String serString = "serNotExists";
		try {
			VintageNamingWebUtils.updateService(serString, "statics");
			fail("ERROR in testUpdateNotExistsSer");
		} catch (VintageException e) {
			System.out.println("Success: testUpdateNotExistsSer");
//			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e
//					.getFactor().getErrorCode());
		}
	}

	/**
	 * 添加一个clusterId
	 */
	@Test
	public void testAddOneCluster() {
		try {
			int cluNum = 0;
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			if (VintageNamingWebUtils.getCluster(serviceId) != null) {
				cluNum = VintageNamingWebUtils.getCluster(serviceId).size();
			}
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
			assertEquals(cluNum + 1, VintageNamingWebUtils.getCluster(serviceId).size());
		} catch (Exception e) {
			System.out.print(e.getMessage());
			fail("error in addOneCluster");
		} finally {
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			if (VintageNamingWebUtils.getCluster(serviceId).size() == 0) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}

	/**
	 * 反复增加/删除一个 clusterId 100次
	 */
	@Test
	public void testSwichAddCluster() {
		try {
			if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			for (int i = 0; i < 100; i++) {
				if (!VintageNamingWebUtils.getCluster(serviceId).toString().contains(clusterId)) {
					VintageNamingWebUtils.addCluster(serviceId, clusterId);
				}
				if (VintageNamingWebUtils.getCluster(serviceId).toString().contains(clusterId)) {
					VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
				}
			}
		} catch (VintageException e) {
			fail("error in testSwichAddCluster");
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				for (NamingServiceCluster clu : VintageNamingWebUtils.getCluster(serviceId)){
					VintageNamingWebUtils.deleteCluster(serviceId, clu.toString().split("\"")[1]);
				}
			VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}

	/**
	 * 重复增加一个 clusterId 10 次,重复删除多次
	 */
	@Test
	public void testRepeatAddDelCluster() {
		try {
			if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId);
			}
			fail("error in testRepeatAddDelCluster");
		} catch (VintageException e) {
			
		}
		try {
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			}
			fail("error in testRepeatAddDelCluster");
		} catch (VintageException e) {
			
		} finally {
			if (VintageNamingWebUtils.getCluster(serviceId).toString().contains(clusterId)) {
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			}
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	/**
	 * 连续添加 100 个clusterId， 连续删除 99个clusterId,删除最后一个clusterId
	 * 当clusterId下有节点时，会禁止删除此clusterId
	 */
	@Test
	public void testMultiCluster() {
		if (!VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
			VintageNamingWebUtils.addService(serviceId);
		}
		for (int i = 0; i < 100; i++) {
			if (VintageNamingWebUtils.getCluster(serviceId).toString().contains(clusterId+i)) {
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId+i);
			}
		}
		int clusNum = 0;
		Set<NamingServiceCluster> clusterIds = VintageNamingWebUtils.getCluster(serviceId);
		if (clusterIds != null) {
			clusNum = clusterIds.size();
		}
		try {
			for (int i = 0; i < 100; i++) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId + i);
			}
			assertEquals(clusNum + 100, VintageNamingWebUtils.getCluster(serviceId).size());
			for (int i = 0; i < 99; i++) {
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId + i);
			}

			assertEquals(clusNum + 1, VintageNamingWebUtils.getCluster(serviceId).size());

			VintageNamingWebUtils.deleteCluster(serviceId, clusterId + 99);

			int clusNum1 = 0;
			Set<NamingServiceCluster> clusterIds1 = VintageNamingWebUtils.getCluster(serviceId);
			if (clusterIds1 != null) {
				clusNum1 = clusterIds1.size();
			}
			assertEquals(clusNum, clusNum1);

		} catch (VintageException e) {
			e.printStackTrace();
			fail("ERROR in testMultiCluster");
		} finally {
			for (int i = 0; i < 100; i++) {
				if (VintageNamingWebUtils.getCluster(serviceId).toString().contains(clusterId+i)) {
					VintageNamingWebUtils.deleteCluster(serviceId, clusterId+i);
				}
			}
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	/**
	 * 10个serviceId增加相同的20个集群,之后删除
	 */
	@Test
	public void testMultiService() {
		try {
			for (int i = 0; i < 10; i++) {
				if (!VintageNamingWebUtils.existsService(serviceId + i)) {
					VintageNamingWebUtils.addService(serviceId + i);
				}
			}

			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 20; j++) {
					if (!VintageNamingWebUtils.existCluster(serviceId + i, clusterId + j)) {
						VintageNamingWebUtils.addCluster(serviceId + i, clusterId + j);
					}
				}
			}

			for (int i = 0; i < 10; i++) {
				assertEquals(20, VintageNamingWebUtils.getCluster(serviceId + i).size());
			}

			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 20; j++) {
					VintageNamingWebUtils.deleteCluster(serviceId + i, clusterId + j);
				}
			}

			for (int i = 0; i < 10; i++) {
				assertEquals(0, VintageNamingWebUtils.getCluster(serviceId + i).size());
			}

		} catch (VintageException e) {
			e.printStackTrace();
			fail("ERROR in testMultiCluster");
		} finally {
			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.deleteService(serviceId + i);
			}
		}
	}

	/**
	 * clusterId中有节点，是否能删除对应的clusterId
	 */
	@Test
	public void testDeleteClusterHasNodes() {
		int port = 3344;

		try {
			VintageNamingWebUtils.addService(serviceId);
			addCluster(serviceId, clusterId);
			addWhiteList(serviceId, localNodes);
			VintageNamingWebUtils.register(serviceId, clusterId, localIP, port);

			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			fail("Error in testDeleteClusterHasNodes");
		} catch (VintageException e) {
			// TODO: handle exception
			e.printStackTrace();
			assertEquals(ExcepFactor.E_REMOVE_CLUSTER.getErrorCode(), e
					.getFactor().getErrorCode());

		} finally {
		}
	}

	/**
	 * 对于不存在的serviceId/clusterId进行deleteclusterId操作
	 */
	@Test
	public void testDeleteNoExists() {
		try {
			VintageNamingWebUtils.deleteCluster(serviceId2, clusterId);
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e
					.getFactor().getErrorCode());
		}

		try {
			VintageNamingWebUtils.addService(serviceId);
			String clusterIdStr = "clusterIdNoExists";
			VintageNamingWebUtils.deleteCluster(serviceId, clusterIdStr);
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_CLUSTER_ID_NOT_EXISTS.getErrorCode(), e
					.getFactor().getErrorCode());
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}

	/**
	 * 为不存在的serviceId增加 clusterId
	 */
	@Test
	public void testAddClusterWithNotExistsService() {
		String clu = "clus";
		try {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
			fail("ERROR IN testAddClusterWithNotExistsService, should throw exception");
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e
					.getFactor().getErrorCode());
		}
	}

	/**
	 * 不存在的serviceId进行getclusterId操作
	 */
	@Test
	public void testGetClusterNotExistSer() {
		try {
			VintageNamingWebUtils.getCluster(serviceId);
			fail("ERROR IN testGetClusterNotExistSer");
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e
					.getFactor().getErrorCode());
		}
	}

	/**
	 * 测试：getserviceId重复多次 getclusterId重复多次；空的serviceId返回null； *
	 */
	@Test
	public void testRepeatGet() {
		try {
			int serNum = 0;
			if (VintageNamingWebUtils.getServiceInfoSet() != null) {
				serNum = VintageNamingWebUtils.getServiceInfoSet().size();
			}
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			for (int i = 0; i < 10; i++) {
				assertTrue(VintageNamingWebUtils.existsService(serviceId));
			}

			if (!VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
				VintageNamingWebUtils.addCluster(serviceId, clusterId);
			}
			for (int i = 0; i < 10; i++) {
				assertEquals(1, VintageNamingWebUtils.getCluster(serviceId).size());
			}
		} catch (VintageException e) {
			System.out.print(e.getMessage());
			fail("Error in testRepeatGet");
		} finally {
			try {
				VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
				VintageNamingWebUtils.deleteService(serviceId);
			} catch (Exception e2) {
				// TODO: handle exception
				e2.printStackTrace();
			}

		}
	}

	/**
	 * 非法 clusterId add get delete
	 */
	@Test
	public void testInvalidCluster() {
		String clusterId = "%%%";
		try {
			VintageNamingWebUtils.addService(serviceId);
			VintageNamingWebUtils.addCluster(serviceId, clusterId);

			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
			fail("ERROR -- should throw exception");
		} catch (VintageException e) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, e.getFactor());
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);

		}
	}

	/*
	 * getserviceId(serviceId)
	 */
	@Test
	public void testGetSingleService() {
		// add serviceId
		List<String> serviceIdList = new ArrayList<String>();
		String serviceId = "test1";
		String serviceId2 = "test2";
		String serviceId3 = "test1_normal_yf";

		serviceIdList.add(serviceId);
		serviceIdList.add(serviceId2);
		serviceIdList.add(serviceId3);

		try {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.addService(string);
			}

			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId);
			assertEquals(1, infoSet.size());
			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertEquals(serviceId, namingServiceInfo.getName());
				assertEquals(NamingServiceType.statics,
						namingServiceInfo.getType());
			}

			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId + "&useSmart=false");
			assertEquals(1, infoSet.size());
			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertEquals(serviceId, namingServiceInfo.getName());
				assertEquals(NamingServiceType.statics,
						namingServiceInfo.getType());
			}

			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet("test");
			assertTrue(infoSet == null || infoSet.isEmpty());
		} finally {
			for (String string : serviceIdList) {
				if (VintageNamingWebUtils.existsService(string)) {
					VintageNamingWebUtils.deleteService(string);
				}
			}
		}
	}

	/*
	 * getserviceId test if getserviceId is related to smartserviceId
	 */
	@Test
	public void testGetSingleServiceSmartService() {
		// add serviceId
		List<String> serviceIdList = new ArrayList<String>();
		String serviceId = "test1";
		String serviceId2 = "test2";
		String serviceId3 = "test1_normal_yf";

		// don't add serviceId
		serviceIdList.add(serviceId2);
		serviceIdList.add(serviceId3);

		try {
			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId);
			assertTrue(infoSet == null || infoSet.isEmpty());

			for (String string : serviceIdList) {
				VintageNamingWebUtils.addService(string);
			}

			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId);
			// fail,size is 1 and the result is serviceId3, processed by smart
			// serviceId
			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertEquals(serviceId3, namingServiceInfo.getName());
				assertEquals(NamingServiceType.statics,
						namingServiceInfo.getType());
			}

			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId + "&useSmart=false");
			// fail,size is 1 and the result is serviceId3, processed by smart
			// serviceId
			assertTrue(infoSet == null || infoSet.isEmpty());

		} finally {
			for (String string : serviceIdList) {
				if (VintageNamingWebUtils.existsService(string)) {
					VintageNamingWebUtils.deleteService(string);
				}
			}
		}
	}

	/*
	 * invoke exception of smart serviceId
	 */
	@Test
	public void testGetSingInvalidSmartService() {

		Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId
				+ "&useSmart=false");
		assertTrue(infoSet == null || infoSet.isEmpty());

		try {
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId);
			assertTrue(infoSet == null || infoSet.isEmpty());

		} catch (VintageException ex) {
			// TODO: handle exception
			assertEquals(ExcepFactor.E_INVALID_NAMING_SERVICE_PRIORITY,
					ex.getFactor());
		}

	}

	/*
	 * getserviceId(serviceId);serviceId is null or " "
	 */
	@Test
	public void testGetSingleServiceInvalidService() {
		List<String> serviceIdList = new ArrayList<String>();

		
		serviceIdList.add(serviceId);
		serviceIdList.add(serviceId2);

		try {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.addService(string);
			}

			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getSingleServiceInfoSet("");
			assertTrue(infoSet.toString().contains(serviceId));
			assertTrue(infoSet.toString().contains(serviceId2));
		} catch (VintageException ex) {
			ex.printStackTrace();
			fail();
		} finally {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.deleteService(string);
			}
		}
	}

	@Test
	public void testGetServiceFuzzyTrue() {
		String fuzzyString = "&fuzzy=true";
		List<String> serviceIdList = new ArrayList<String>();
		
		String serviceIdSuffix = "_normal_yf";
		String serviceIdSuffix2 = "_high_yf";

		serviceIdList.add(serviceId);
		serviceIdList.add(serviceId + serviceIdSuffix);
		serviceIdList.add(serviceId + serviceIdSuffix2);
		serviceIdList.add(serviceId2);
		serviceIdList.add(serviceId2 + serviceIdSuffix);
		serviceIdList.add(serviceId2 + serviceIdSuffix2);

		try {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.addService(string);
			}
			// subString
			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId
					+ fuzzyString);
			assertTrue(infoSet.toString().contains(serviceId));
			assertTrue(infoSet.toString().contains(serviceId+serviceIdSuffix));
			assertTrue(infoSet.toString().contains(serviceId+serviceIdSuffix2));

			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertTrue(namingServiceInfo.getName().contains(serviceId));
			}

			String searchService = "testGetServiceFuzzyTrue";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertEquals(6, infoSet.size());
			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertTrue(namingServiceInfo.getName().contains(searchService));
			}

			searchService = "tttttt";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "testGetServiceFuzzyTrue1.*";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertEquals(3, infoSet.size());
			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertTrue(namingServiceInfo.getName().contains("testGetServiceFuzzyTrue1"));
			}

			searchService = "testGetServiceFuzzyTrue1.*normal.*yf";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertEquals(1, infoSet.size());

			searchService = "testGetServiceFuzzyTrue1.yf";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "tttttttt.*";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());
		} finally {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.deleteService(string);
			}
		}
	}

	/*
	 * fuzzy is false
	 */
	@Test
	public void testGetServiceFuzzyFalse() {
		String fuzzyString = "&fuzzy=false";
		getServiceFuzzyFalse(fuzzyString);
	}

	/*
	 * fuzzy is blank
	 */
	@Test
	public void testGetServiceFuzzyBlank() {
		String fuzzyString = "&fuzzy=false";
		getServiceFuzzyFalse(fuzzyString);
	}

	/*
	 * fuzzy is null
	 */
	@Test
	public void testGetServiceFuzzyNull() {
		String fuzzyString = "&fuzzy=null";
		getServiceFuzzyFalse(fuzzyString);
	}

	private void getServiceFuzzyFalse(String fuzzyString) {
		List<String> serviceIdList = new ArrayList<String>();
		String serviceId = "test1";
		String serviceId2 = "test2";
		String serviceIdSuffix = "_normal_yf";
		String serviceIdSuffix2 = "_high_yf";

		serviceIdList.add(serviceId);
		serviceIdList.add(serviceId + serviceIdSuffix);
		serviceIdList.add(serviceId + serviceIdSuffix2);
		serviceIdList.add(serviceId2);
		serviceIdList.add(serviceId2 + serviceIdSuffix);
		serviceIdList.add(serviceId2 + serviceIdSuffix2);

		try {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.addService(string);
			}

			// subString
			Set<NamingServiceInfo> infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(serviceId
					+ fuzzyString);
			assertEquals(1, infoSet.size());
			for (NamingServiceInfo namingServiceInfo : infoSet) {
				assertEquals(serviceId, namingServiceInfo.getName());
			}

			String searchService = "test";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "test3";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "test_R";
			try {
				infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
				assertTrue(infoSet == null || infoSet.isEmpty());
				fail();
			} catch (VintageException ex) {
				assertEquals(ExcepFactor.E_INVALID_NAMING_SERVICE_PRIORITY,
						ex.getFactor());
				// fail();
			}

			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString
					+ "&useSmart=false");
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "test1.*";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "test1.*normal.*yf";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "test1.yf";
			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
			assertTrue(infoSet == null || infoSet.isEmpty());

			searchService = "test.*_R";
			try {
				infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString);
				assertTrue(infoSet == null || infoSet.isEmpty());
				fail();
			} catch (VintageException ex) {
				assertEquals(ExcepFactor.E_INVALID_NAMING_SERVICE_PRIORITY,
						ex.getFactor());
				// fail();
			}

			infoSet = VintageNamingWebUtils.getSingleServiceInfoSet(searchService + fuzzyString
					+ "&useSmart=false");
			assertTrue(infoSet == null || infoSet.isEmpty());
		} finally {
			for (String string : serviceIdList) {
				VintageNamingWebUtils.deleteService(string);
			}
		}
	}

	// parameters exception check
	@Test
	public void testParamsUpdateService() {
		try {
			VintageNamingWebUtils.updateService("", "statics");
			fail();
		} catch (VintageException ex) {
			// TODO: handle exception
			System.out.println("===testUpdateService===" + "errorCode:"
					+ ex.getFactor().getErrorCode() + ", detail message:"
					+ ex.getMessage());

		}
	}

	@Test
	public void testParamsUpdateServiceType() {
		try {
			if (!VintageNamingWebUtils.existsService(serviceId + 1)) {
				VintageNamingWebUtils.addService(serviceId + 1);
			}
			VintageNamingWebUtils.updateService(serviceId + 1, "dynamics");
			fail();
		} catch (VintageException ex) {
//			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		} finally {
			VintageNamingWebUtils.deleteService(serviceId + 1);
		}
	}

	@Test
	public void testParamsAddService() {
		try {
			VintageNamingWebUtils.addService("");
			fail();
		} catch (VintageException ex) {
			System.out.println("===testAddService===" + "errorCode:"
					+ ex.getFactor().getErrorCode() + ", detail message:"
					+ ex.getMessage());
		}

	}

	@Test
	public void testParamsAddServiceWithType() {
		try {
			VintageNamingWebUtils.addService(serviceId + 3, "dynamics", cached);
			fail();
		} catch (VintageException ex) {
			assertEquals(ExcepFactor.E_PARAM_INVALID_ERROR, ex.getFactor());
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId + 3)) {
				VintageNamingWebUtils.deleteService(serviceId + 3);
			}
		}
	}

	@Test
	public void testParamsDeleteService() {
		try {
			VintageNamingWebUtils.deleteService(null);
		} catch (VintageException ex) {
			System.out.println("===testDeleteService===" + "errorCode:"
					+ ex.getFactor().getErrorCode() + ", detail message:"
					+ ex.getMessage());
		}
	}

	@Test
	public void testParamsAddClusterWithSerNull() {
		try {
			VintageNamingWebUtils.addCluster(null, this.clusterId);
		} catch (VintageException ex) {
			System.out.println("===testAddClusterWithSerNull===" + "errorCode:"
					+ ex.getFactor().getErrorCode() + ", detail message:"
					+ ex.getMessage());
		}
	}

	@Test
	public void testParamsAddClusterWithCluNull() {
		try {
			VintageNamingWebUtils.addCluster(this.serviceId, null);
		} catch (VintageException ex) {
			System.out.println("===testAddClusterWithCluNull===" + "errorCode:"
					+ ex.getFactor().getErrorCode() + ", detail message:"
					+ ex.getMessage());
		}
	}

	@Test
	public void testParamsGetCluster() {
		try {
			Set<NamingServiceCluster> clusterIds = VintageNamingWebUtils.getCluster(null);
			assertTrue(clusterIds.isEmpty() || clusterIds.size() == 0);
		} catch (VintageException ex) {
			System.out.println("===testGetCluster===" + "errorCode:"
					+ ex.getFactor().getErrorCode() + ", detail message:"
					+ ex.getMessage());
		}
	}
	
	public void sleep(int seconds){
		try {
			Thread.sleep(seconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
