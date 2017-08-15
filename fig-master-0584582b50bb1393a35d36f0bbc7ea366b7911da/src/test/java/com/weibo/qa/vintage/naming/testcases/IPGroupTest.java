package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 获取已添加ip白名单的service列表
 * */
public class IPGroupTest extends BaseTest{
	
	private int num = 10;
	private String nodeIp = "";
	private Random random = new Random();
	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		nodeIp = "10.236.23." + random.nextInt(255); 
		VintageNamingWebUtils.addService(serviceId);
	}

	@After
	public void tearDown() throws Exception {
		VintageNamingWebUtils.deleteService(serviceId);
	}
	
	/**
	 * configserver只有一个服务
	 *
	 */
	@Test
	public void IPGroupTest1() {
		try {
			VintageNamingWebUtils.addWhitelist(serviceId, nodeIp);
			assertTrue(VintageNamingWebUtils.existsWhitelist(serviceId, nodeIp));
			assertEquals(1, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} finally {
			VintageNamingWebUtils.deleteWhitelist(serviceId, nodeIp);
			assertTrue(!VintageNamingWebUtils.existsWhitelist(serviceId, nodeIp));
		}
	}
	
	/**
	 * configserver有多个服务
	 * @throws InterruptedException 
	 *
	 */
	@Test
	public void IPGroupTest2() throws InterruptedException {
		try{
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);
			VintageNamingWebUtils.batchaddWhitelist(serviceId, nodeIp, num);
			
			assertEquals(num, VintageNamingWebUtils.getnodeservice(nodeIp).size());
			
		} finally{
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
		}
		
	}
	
	/**
	 * configserver没有任何注册服务
	 *
	 */
	@Test
	public void IPGroupTest3() {
		try {
//			assertEquals(0, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
		}
	}
	
	/**
	 * configserver有一个服务，但未添加白名单
	 *
	 */
	@Test
	public void IPGroupTest4() {
		try {
//			assertEquals(1, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail ("Something error");
		}
	}
	
	/**
	 * configserver有一个服务，但未添加白名单
	 *
	 */
	@Test
	public void IPGroupTest5() {
		try {
//			assertEquals(1, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail ("Something error");
		}
	}
	
	/**
	 * configserver有多个服务，但未添加白名单
	 *
	 */
	@Test
	public void IPGroupTest6() {
		try {
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);

//			assertEquals(num+1, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail ("Something error");
		} finally {
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
		}
	}
	
	/**
	 * ip注册部分group，查询ip可注册的服务
	 *
	 */
	@Test
	public void IPGroupTest7() {
		int port = 12345;
		try {
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);

//			System.out.println(VintageNamingWebUtils.getServiceInfoSet().size());
//			assertEquals(num+1, VintageNamingWebUtils.getServiceInfoSet().size());
			VintageNamingWebUtils.batchaddWhitelist(serviceId, nodeIp, num/2);
			
			assertEquals(num/2, VintageNamingWebUtils.getnodeservice(nodeIp).size());
			VintageNamingWebUtils.batchaddCluster(serviceId+0, clusterId, num/2);
			
			VintageNamingWebUtils.batchregister(serviceId+0, clusterId+0, nodeIp, port, port+num/2, "");
			
			assertEquals(num/2, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			fail ("Something error");
		} finally {
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num/2);
			VintageNamingWebUtils.batchunregister(serviceId+0, clusterId+0, nodeIp, port, port+num/2);
			VintageNamingWebUtils.batchdeleteCluster(serviceId+0, clusterId, num/2);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
		}
	}
	
	/**
	 * ip注册部分group，删除该group对ip的白名单，查询ip可注册的服务，以白名单为准
	 * 
	 */
	@Test
	public void IPGroupTest8() {
		int port = 12345;
		try {
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);

//			assertEquals(num+1, VintageNamingWebUtils.getServiceInfoSet().size());
			VintageNamingWebUtils.batchaddWhitelist(serviceId, nodeIp, num/2);
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num/2);
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, nodeIp, port, num/2);
			
			assertEquals(num/2, VintageNamingWebUtils.getnodeservice(nodeIp).size());
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num/2);
			
			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			fail ("Something error");
		} finally {
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, nodeIp, port, num/2);
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num/2);
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num/2);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
			
		}
	}
	
	/**
	 * 添加1000个服务，添加1000个白名单，查看ip可注册的服务，取消
	 * 
	 */
	@Test
	public void IPGroupTest9() {
		int num = 1000;
		try {
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);
			VintageNamingWebUtils.batchaddWhitelist(serviceId, nodeIp, num);

			assertEquals(num, VintageNamingWebUtils.getnodeservice(nodeIp).size());
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num/2);

			assertEquals(num/2, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			fail("Something error");
		} finally{
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
		}
	}
	
	/**
	 * redis flush, 10s内缓存有
	 * 
	 */
	@Test
	public void IPGroupTest10() {
		try {
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);
			VintageNamingWebUtils.batchaddWhitelist(serviceId, nodeIp, num);

			assertEquals(num, VintageNamingWebUtils.getnodeservice(nodeIp).size());
//			RedisWebUtils.Flushall();
//			System.out.println(VintageNamingWebUtils.getnodeservice(nodeIp).size());
//			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			fail("Something error");
		} finally{
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
		}
	}
	
	/**
	 * redis挂掉
	 * 
	 */
	@Ignore
	@Test
	public void IPGroupTest11() {
		try {
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);
			VintageNamingWebUtils.batchaddWhitelist(serviceId, nodeIp, num);
	
			assertEquals(num, VintageNamingWebUtils.getnodeservice(nodeIp).size());
			RedisWebUtils.StopRedis();
			assertEquals(0, VintageNamingWebUtils.getnodeservice(nodeIp).size());
		} catch (Exception e) {
			fail("Something error");
		} finally{
			RedisWebUtils.StartRedis();
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, nodeIp, num);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
		}
	}
	
	/**
	 * ip为空
	 * 
	 */
	@Test
	public void IPGroupTest12() {
		try {
			String ip = "";
			System.out.println(VintageNamingWebUtils.getnodeservice(ip).size());
			assertEquals(0, VintageNamingWebUtils.getnodeservice(ip).size());
//			assertNull(VintageNamingWebUtils.getnodeservice(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ip为null
	 * 
	 */
	@Test
	public void IPGroupTest13() {
		try {
			String ip = null;
			assertEquals(0, VintageNamingWebUtils.getnodeservice(ip).size());
			//assertNull(VintageNamingWebUtils.getnodeservice(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ip为str
	 * 
	 */
	@Test
	public void IPGroupTest14() {
		try {
			String ip = "helloworld";
			assertEquals(0, VintageNamingWebUtils.getnodeservice(ip).size());
			//fail("Something wrong");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
