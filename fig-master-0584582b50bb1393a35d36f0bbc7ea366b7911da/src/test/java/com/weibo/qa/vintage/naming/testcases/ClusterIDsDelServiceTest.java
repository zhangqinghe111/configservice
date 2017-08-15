package com.weibo.qa.vintage.naming.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class ClusterIDsDelServiceTest extends BaseTest{
	private int num = 10;
	private int port = 1234;
	@Before
	public void setUp() throws Exception {
		super.setUp();
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
	public void batchunregister1() {
		String serviceId = "";
		try {
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
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
	public void batchunregister2() {
		String serviceId = null;
		try {
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			fail("Something wrong");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * ip为null
	 * 
	 */
	@Test
	public void batchunregister3() {
		String localIP = null;
		try {
			if(!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			fail("Something wrong");
		} catch (Exception e) {
			e.printStackTrace();
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
	public void batchunregister4() {
		String localIP = "";
		try {
			if(!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			fail("Something wrong");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * ip为字符串
	 * 
	 */
	@Test
	public void batchunregister5() {
		String localIP = "helloworld";
		try {
			if(!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			fail("Something wrong");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 已添加serviceIdId，注册ip，批量取消调用
	 * 
	 */
	@Test
	public void batchunregister6() {
		try {
			if(!VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.addService(serviceId);
			}
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)) {
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			for (int i = 0; i < num; i++){
				VintageNamingWebUtils.register(serviceId, clusterId+i, localIP, port);
			}
			assertEquals(num, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
		} catch (Exception e) {
			fail("Something wrong");
			e.printStackTrace();
		} finally {
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)) {
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 未添加serviceIdId，未注册ip，批量取消调用
	 * 
	 */
	@Test
	public void batchunregister7() {
		try{
			VintageNamingWebUtils.batchunregister(serviceId, localIP, port);
			fail("Something wrong");
		}catch(Exception e){
			System.out.println(e.getMessage());
		}finally{}
	}
	
	/**
	 * 添加serviceIdId，批量取消调用
	 * 
	 */
	@Test
	public void batchunregister8() {
		try{
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			//fail("Something wrong");
		}catch(Exception e){
			e.printStackTrace();
			fail("Something wrong");
		}finally{
			if (VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdId，添加白名单，批量取消调用
	 * 
	 */
	@Test
	public void batchunregister9() {
		try{
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			//fail("Something wrong");
		}catch(Exception e){
			fail("Something wrong");
			e.printStackTrace();
		}finally{
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdId，添加HEARTBEATINTERVAL个clusterId，向HEARTBEATINTERVAL个clusterId注册ip，批量取消注册调用
	 * 
	 */
	@Test
	public void batchunregister10() {
		int num = HEARTBEATINTERVAL;
		try{
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num);
			
			assertEquals(num, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			//fail("Something wrong");
		}catch(Exception e){
			fail("Something wrong");
			e.printStackTrace();
		}finally{
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdId，添加1000个clusterId，向500个clusterId注册ip，批量取消注册调用
	 * 
	 */
	@Test
	public void batchunregister11() {
		int num = 1000;
		try{
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num/2);
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num/2);
			
			assertEquals(num/2, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num/2, num);
			VintageNamingWebUtils.batchregistercluster(serviceId, clusterId, localIP, port, num/2, num);
		
			assertEquals(num/2, VintageNamingWebUtils.batchunregister(serviceId, localIP, port).size());
			//fail("Something wrong");
		}catch(Exception e){
			fail("Something wrong");
			e.printStackTrace();
		}finally{
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加多个serviceIdId，每个serviceIdId添加多个clusterId，
	 * 向每个serviceIdI的、每个clusterId注册ip，指定其中一个serviceIdId，批量取消注册
	 * 
	 */
	@Test
	public void batchunregister12() {
		int num = 2;
		try{
			VintageNamingWebUtils.batchaddService(serviceId, "statics", num);
			
			VintageNamingWebUtils.batchaddWhitelist(serviceId, localIP, num);
			
			VintageNamingWebUtils.batchregisterservice(serviceId, clusterId, localIP, port, num);
			
			assertEquals(1, VintageNamingWebUtils.batchunregister(serviceId+0, localIP, port).size());
			VintageNamingWebUtils.deleteCluster(serviceId+0, clusterId);
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId+0, localIP, port).size());
			VintageNamingWebUtils.deleteWhitelist(serviceId+0, localIP);
			assertEquals(0, VintageNamingWebUtils.batchunregister(serviceId+0, localIP, port).size());
			VintageNamingWebUtils.deleteService(serviceId+0);
			assertEquals(1, VintageNamingWebUtils.batchunregister(serviceId+1, localIP, port).size());
			//fail("Something wrong");
		}catch(Exception e){
			fail("Something wrong");
			e.printStackTrace();
		}finally{
			if (VintageNamingWebUtils.existsWhitelist(serviceId+1, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId+1, localIP);
			}
			if (VintageNamingWebUtils.existCluster(serviceId+1, clusterId)){
				VintageNamingWebUtils.deleteCluster(serviceId+1, clusterId);
			}
			
			if (VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
	
	/**
	 * 添加serviceIdId，添加10个clusterId，分别注册10个ip，批量取消注册其中一个ip
	 * 
	 */
	@Test
	public void batchunregister13() {
		try{
			if (!VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.addService(serviceId);
			}
			if (!VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.addWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchaddCluster(serviceId, clusterId, num);
			for (int i = 0; i < num; i++){
				VintageNamingWebUtils.batchregister(serviceId, clusterId+i, localIP, port, port+num, "");
			}
			for (int i = 0; i < num; i++){
				assertEquals(num, VintageNamingWebUtils.lookup_set(serviceId, clusterId+i).size());
			}
			for (int i = 0; i < num; i++){
				assertEquals(num, VintageNamingWebUtils.batchunregister(serviceId, localIP, port+i).size());
				for (int j = 0; j < num; j++){
					assertEquals(num-1-i, VintageNamingWebUtils.lookup_set(serviceId, clusterId+j).size());
				}
			}
		}catch(Exception e){
			fail("Something wrong");
			e.printStackTrace();
		}finally{
			if (VintageNamingWebUtils.existsWhitelist(serviceId, localIP)){
				VintageNamingWebUtils.deleteWhitelist(serviceId, localIP);
			}
			
			VintageNamingWebUtils.batchdeleteCluster(serviceId, clusterId, num);
			
			if (VintageNamingWebUtils.existsService(serviceId)){
				VintageNamingWebUtils.deleteService(serviceId);
			}
		}
	}
}
