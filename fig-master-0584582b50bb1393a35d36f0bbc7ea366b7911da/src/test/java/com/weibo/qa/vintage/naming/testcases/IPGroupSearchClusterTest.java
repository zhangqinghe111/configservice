package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.weibo.vintage.utils.RedisWebUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 查找service下为ip的所有节点注册信息
 * */
public class IPGroupSearchClusterTest extends BaseTest{

//	private Set<String> nodes = new HashSet<String>();
//	private String localIP = "10.209.73.147";
	private int num = 10;
	@Before
	public void setUp() throws Exception {
		super.setUp();
		//RedisWebUtils.Flushall();
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * serviceIdID为空
	 * 
	 */
	@Test
	public void nodeInfoTest1() {
		String serviceId = "";
		try {
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP));
			fail("Something wrong");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * serviceIdID为null
	 * 
	 */
	@Test
	public void nodeInfoTest2() {
		String serviceId = null;
		try {
			System.out.println(VintageNamingWebUtils.getnodeinfo(serviceId, localIP));
			fail("Something wrong");
		} catch (Exception e) {
			System.out.println("Success");
		}
	}
	
	/**
	 * ip为null
	 * 
	 */
	@Test
	public void nodeInfoTest3() {
		String localIP = null;
		try {
			VintageNamingWebUtils.addService(serviceId);
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * ip为空
	 * 
	 */
	@Test
	public void nodeInfoTestIPnull() {
		String localIP = "";
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * ip为null，serviceId有clusterId
	 * 
	 */
	@Test
	public void nodeInfoTest5() {
		String localIP = null;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Something wrong");
		} finally {
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * ip为""，serviceId有clusterId
	 * 
	 */
	@Test
	public void nodeInfoTest6() {
		String localIP = "";
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			VintageNamingWebUtils.getnodeinfo(serviceId, localIP);
			fail("Something wrong");
		} catch (Exception e) {
			
		} finally {
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * serviceIdid为不存在的id
	 * 
	 */
	@Test
	public void nodeInfoTest7() {
		try {
			VintageNamingWebUtils.getnodeinfo(serviceId, localIP);
			fail("Something wrong");
		} catch (Exception e) {
			System.out.println("Success");
		}
	}
	
	/**
	 * ip未注册过，查询结果为null
	 * 
	 */
	@Test
	public void nodeInfoTest8() {
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);

			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch(Exception e) {
			fail("Something wrong");
		} finally{
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdid，添加clusterId，添加白名单，注册ip，查询
	 * 
	 */
	@Test
	public void nodeInfoTest9() {
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);

			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num/2);
			
			assertEquals(num/2, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num/2, num);

			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdid，添加HEARTBEATINTERVAL个clusterId，注册HEARTBEATINTERVAL个，查询
	 * 
	 */
	@Test
	public void nodeInfoTest10() {
		int num = HEARTBEATINTERVAL;
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num/2);
			
			assertEquals(num/2, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num/2, num);

			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加s1、s2，各添加10个clusterId，注册s1的clusterId，查询s2
	 * 
	 */
	@Test
	public void nodeInfoTest11() {
		int port = 1234;
		int num = 2;
		try{
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);
			
			VintageNamingWebUtils.batchaddClusterService(serviceId, clusterId, num);
			
			VintageNamingWebUtils.batchaddWhitelist(serviceId, localIP, num);
			
			VintageNamingWebUtils.register(serviceId+0, clusterId, localIP, port);
			assertEquals(1, VintageNamingWebUtils.getnodeinfo(serviceId+0, localIP).size());
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId+1, localIP).size());
			VintageNamingWebUtils.unregister(serviceId+0, clusterId, localIP, port);
			VintageNamingWebUtils.register(serviceId+1, clusterId, localIP, port);
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId+0, localIP).size());
			assertEquals(1, VintageNamingWebUtils.getnodeinfo(serviceId+1, localIP).size());
			VintageNamingWebUtils.unregister(serviceId+1, clusterId, localIP, port);
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId+0, localIP).size());
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId+1, localIP).size());
		}catch(Exception e){
			fail("Something wrong");
		}finally{
			VintageNamingWebUtils.batchdeleteWhitelist(serviceId, localIP, num);
			VintageNamingWebUtils.batchdeleteClusterService(serviceId, clusterId, num);
			VintageNamingWebUtils.batchdeleteService(serviceId, num);
			
		}
	}
	
	/**
	 * 添加serviceIdid，添加clusterId，添加白名单，注册ip，删除白名单，查询
	 * 
	 */
	@Test
	public void nodeInfoTest12() {
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num);
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdid，添加clusterId，添加白名单，注册ip，取消注册，删除白名单，查询
	 * 
	 */
	@Test
	public void nodeInfoTest13() {
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num);

			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);

			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * redis被flush
	 * 
	 */
	@Test
	public void nodeInfoTest14() {
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);

			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num);
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
		}
	}
	
	/**
	 * redis挂掉
	 * 
	 */
	@Ignore
	@Test
	public void nodeInfoTest15() {
		int port = 1234;
		try {
			if (!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);

			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);

			assertEquals(num, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			RedisWebUtils.StopRedis();
			assertEquals(num, VintageNamingWebUtils.getServiceInfoSet().size());
			assertEquals(0, VintageNamingWebUtils.getnodeinfo(serviceId, localIP).size());
			RedisWebUtils.StartRedis();
			assertNull(VintageNamingWebUtils.getServiceInfoSet());
		} catch (Exception e) {
			fail("Something wrong");
		} finally {
			VintageNamingWebUtils.batchunregistercluster(serviceId, clusterId, localIP, port, num);
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
		}
	}
}
