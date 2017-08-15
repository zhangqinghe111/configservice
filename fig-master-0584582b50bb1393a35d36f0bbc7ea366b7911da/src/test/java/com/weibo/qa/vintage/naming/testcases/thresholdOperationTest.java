package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.text.DecimalFormat;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 1. add接口直接写redis、然后周期时间后回写到内存
 * 2. update首先判断redis里有没有这个key，如果没有操作失败，如果有才更新
 * 3. get接口首先读内存，如果内存为空且redis有数据，此时返回空，只从内存读
 * 4. delete接口删除只redis，内存定期更新后随之删除
 * 注：存在一个默认ratio=0.4的问题：
 * 默认的值  只是在内存和redis中都没有的时候   才会起作用   这个值是写在逻辑中的
 * 前提：redis部署为主从同步
 * */
public class thresholdOperationTest extends BaseTest{

	private String serviceId;
	private String clusterId;
	private String prefix;
	private double ratio = 0.6;  // 默认值
	private double threshold = 0.0;
	private DecimalFormat df = new DecimalFormat("0.0");
	Random random = new Random();

	@Before
	public void setUp() throws Exception {
		prefix = getRandomString(10);
		serviceId = prefix+"Service";
		clusterId = prefix+"Cluster";
		
		super.setUp();
	
		threshold = Double.valueOf(df.format(random.nextDouble()));
		ServerWebUtils.HeartbeatProtection("on");
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * 
	 * */
	@Test
	public void addOneThreshold(){
		try {
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));

		} finally {
			VintageNamingWebUtils.delThreshold(serviceId);
		}
	}
	
	@Test
	public void testRepeatAddThreshold() {
		try {
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
//			assertNull(VintageWebUtils.getThreshold(serviceId));
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			fail("error in testRepeatAddThreshold");
		} catch (VintageException e) {
			//assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS.getErrorCode(), e.getFactor().getErrorCode());
		}

		// there are data in service cache;then add the same service repeatedly
		try {
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			fail("error in testRepeatAddThreshold");
		} catch (VintageException e2) {
			//assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS.getErrorCode(), e2.getFactor().getErrorCode());
		} finally {
			VintageNamingWebUtils.delThreshold(serviceId);
		}
	}
	
	/**
	 * 反复增加/删除一个 threshold 10次,下行接口调用结果，主从同步时间
	 */
	@Test
	public void testSwitchService() {

		for (int i = 0; i < 2; i++) {
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
//			assertNull(VintageWebUtils.getThreshold(serviceId));
			VintageNamingWebUtils.delThreshold(serviceId);
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));

			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		}

		// cache exists
		for (int i = 0; i < 2; i++) {
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			VintageNamingWebUtils.delThreshold(serviceId);
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		}

		try {
			for (int i = 0; i < 2; i++) {
				VintageNamingWebUtils.addThreshold(serviceId, threshold);
				assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
				VintageNamingWebUtils.delThreshold(serviceId);
			}
		} catch (VintageException ex) {
			//assertEquals(ExcepFactor.E_SERVICE_ID_EXISTS, ex.getFactor());
		}
	}
	
	/**
	 * 连续添加 100 个服务， 连续删除 99个服务,删除最后一个服务
	 */
	@Test
	public void testContinueControllService() {
		try {

			for (int i = 0; i < 10; i++) {
				VintageNamingWebUtils.addThreshold(serviceId+i, threshold);
			}
//			assertNull(VintageWebUtils.getThreshold(serviceId+0));
			for (int i = 0; i < 10; i++) {
				assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId+i));
			}

			for (int i = 0; i < 9; i++) {
				VintageNamingWebUtils.delThreshold(serviceId + i);
			}

//			for (int i = 0; i < 10; i++) {
//				assertEquals(String.valueOf(threshold), VintageWebUtils.getThreshold(serviceId+i));
//			}
			for (int i = 0; i < 9; i++) {
				assertNull(VintageNamingWebUtils.getThreshold(serviceId+i));
			}
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId+9));
		} finally {

		}
	}
	
	@Test
	public void testAddthresholdWithStaticService() {
		try {
			VintageNamingWebUtils.addThreshold(serviceId, "statics", threshold);

			fail("ERROR in testAddthresholdWithStaticService()");
			
		} catch (Exception e) {
			System.out.println("Success: testAddthresholdWithStaticService");
		} finally {
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		}
	}
	
	@Test
	public void testRepeatDelThreshold() {

		try {
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
			// 从来就没有缓存时
			VintageNamingWebUtils.delThreshold(serviceId);
			fail("ERROR in testRepeatDelThreshold");
		} catch (VintageException e) {
			System.out.println("Success: testRepeatDelThreshold");
		}

		try {
			// 第二种情况 没有缓存：有缓存，缓存被更新
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));

			VintageNamingWebUtils.delThreshold(serviceId);
//			assertEquals(String.valueOf(threshold), VintageWebUtils.getThreshold(serviceId));
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
			VintageNamingWebUtils.delThreshold(serviceId);
			fail("ERROR in testRepeatDelThreshold2");
		} catch (VintageException e) {
			System.out.println("Success: testRepeatDelThreshold");
		}

		try {
			// 缓存仍然存在时，不会报异常
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));

			VintageNamingWebUtils.delThreshold(serviceId);
			VintageNamingWebUtils.delThreshold(serviceId);
			System.out.println("Success: testRepeatDelThreshold");
//			fail("ERROR in testRepeatDelService3");
		} catch (VintageException e) {
			fail("ERROR in testRepeatDelService3");
		}
	}
	
	@Test
	public void testDeleteThresholdServiceNotExist() {
		try {
			if (VintageNamingWebUtils.getServiceInfoSet().toString().contains(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
			VintageNamingWebUtils.delThreshold(serviceId);
			fail("ERROR in testDeleteServiceNotExist");
		} catch (VintageException e) {
			//assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e.getFactor().getErrorCode());
			System.out.println("Success: testDeleteThresholdServiceNotExist");
		}
	}
	
	/**
	 * @bug  内存无数据时，更新阈值报错，内存无数据时，更新服务不报错
	 * */
	@Test
	public void testUpdateThreshold() {

		try {
			// 没有缓存
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			double ratio = Double.valueOf(df.format(random.nextDouble()));
			VintageNamingWebUtils.updateThreshold(serviceId, ratio);
//			assertNull(VintageWebUtils.getThreshold(serviceId));
			
			// 有缓存
			assertEquals(String.valueOf(ratio), VintageNamingWebUtils.getThreshold(serviceId));
			ratio = Double.valueOf(df.format(random.nextDouble()));
			VintageNamingWebUtils.updateThreshold(serviceId, ratio);
			assertEquals(String.valueOf(ratio), VintageNamingWebUtils.getThreshold(serviceId));
		} finally {
			VintageNamingWebUtils.delThreshold(serviceId);
		}
	}
	
	@Test
	public void testUpdateThresholdBeforeAdd() {
		try {
			// 没有缓存
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			fail("ERROR in testUpdateThresholdBeforeAdd()");
		} catch(Exception e){
			System.out.println("Success: testUpdateThresholdBeforeAdd");
		}finally {
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		}
		
	}
	
	@Test
	public void testUpdateThresholdServiceStatics() {

		try {
			VintageNamingWebUtils.addService(serviceId, "statics");
			assertEquals(NamingServiceType.statics.toString(), 
					VintageNamingWebUtils.getService(serviceId).getType().toString());
			
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			fail("Error in testUpdateThresholdServiceStatics()");
//			assertNull(VintageWebUtils.getThreshold(ser));
//			assertNull(VintageWebUtils.getThreshold(ser));

		} catch (Exception e) {
			System.out.println("Success: testUpdateThresholdServiceStatics");
		} finally {
			VintageNamingWebUtils.deleteService(serviceId);
		}

	}

	@Test
	public void testUpdateThresholdNotExistsSer() {
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			fail("ERROR in testUpdateNotExistsSer");
		} catch (VintageException e) {
			//assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e.getFactor().getErrorCode());
			System.out.println("Success: testUpdateThresholdNotExistsSer");
		}
	}
	
	@Test
	public void testServiceFromDtoS() {
		try {
			VintageNamingWebUtils.addThreshold(serviceId, threshold);
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			VintageNamingWebUtils.updateService(serviceId, "statics");
			assertEquals(NamingServiceType.statics.toString(), 
					VintageNamingWebUtils.getService(serviceId).getType().toString());
			//fail("ERROR in testUpdateNotExistsSer");
		} catch (VintageException e) {
			//assertEquals(ExcepFactor.E_SERVICE_ID_NOT_EXISTS.getErrorCode(), e.getFactor().getErrorCode());
			//System.out.println("Success: testUpdateThresholdNotExistsSer");
			fail("ERROR in testUpdateNotExistsSer");
		} finally{
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}
	
	
	@Test
	public void testThresholdOperation(){
		try{
		//assertNull(VintageWebUtils.getThreshold(serviceId));
		VintageNamingWebUtils.addThreshold(serviceId, threshold);
		assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
		double ratio1 = Double.valueOf(df.format(random.nextDouble()));
		VintageNamingWebUtils.updateThreshold(serviceId, ratio1);
		assertEquals(String.valueOf(ratio1), VintageNamingWebUtils.getThreshold(serviceId));
		VintageNamingWebUtils.delThreshold(serviceId);
		assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		} catch(Exception e){
			fail("ERROR in testThresholdOperation()");
		} finally {
			
		}
	}
	
	@Test
	public void testThresholdOperationRepeat(){
		int count = 10;
		assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		VintageNamingWebUtils.addThreshold(serviceId, threshold);
		
		assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
		for (int i = 0; i < count; i++){
			double tmp_threshold = Double.valueOf(df.format(random.nextDouble()));
			if (tmp_threshold < 1.0){
				threshold = tmp_threshold;
				VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			}
		}
		assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
		
		VintageNamingWebUtils.delThreshold(serviceId);
//		VintageWebUtils.delThreshold(serviceId);
		assertNull(VintageNamingWebUtils.getThreshold(serviceId));
	}
	
	@Test
	public void testupdateThreshold(){
		int count = 10;
		assertNull(VintageNamingWebUtils.getThreshold(serviceId));
		VintageNamingWebUtils.addThreshold(serviceId, threshold);
		
		assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
		for (int i = 0; i < count; i++){
			double tmp_threshold = Double.valueOf(df.format(random.nextDouble()));
			if (tmp_threshold < 1.0){
				threshold = tmp_threshold;
				VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			}
		}
		assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
		
		VintageNamingWebUtils.delThreshold(serviceId);
//		VintageWebUtils.delThreshold(serviceId);
		assertNull(VintageNamingWebUtils.getThreshold(serviceId));
	}
	
	protected void init(String service, String cluster, double threshold) {
		VintageNamingWebUtils.addThreshold(service, threshold);
		addCluster(service, cluster);
		addWhiteList(service, localNodes);
	}

	protected void clean(String service, String cluster) {
		delWhiteList(service, localNodes);
		delCluster(service, clusterId);
		VintageNamingWebUtils.delThreshold(service);
	}

	private int getWorkingNodeSize(int totalsize) {
		return totalsize - (int) Math.floor(totalsize * ratio + 1);
	}
}
