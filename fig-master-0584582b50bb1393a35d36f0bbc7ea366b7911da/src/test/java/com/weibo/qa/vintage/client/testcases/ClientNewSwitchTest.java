//package com.weibo.qa.vintage.client.testcases;
//
//import static org.junit.Assert.*;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import com.weibo.qa.vintage.naming.testcases.BaseTest;
//import com.weibo.vintage.client.NamingServiceClient;
//import com.weibo.vintage.client.StaticsConfigServiceClient;
//import com.weibo.vintage.failover.NodeExciseStrategy;
//import com.weibo.vintage.model.NamingServiceNode;
//import com.weibo.vintage.naming.util.Utils;
//import com.weibo.vintage.utils.SwitcherUtils;
//import com.weibo.vintage.utils.VintageWebUtils;
//
///**
// * 新增开关测试
// * 开关功能：控制lookup、lookupforupdate的消息体大小最大不超过1M
// * 开：原逻辑不变
// * 关：多大消息体都可传输成功
// * 查看对订阅是否的影响，若消息体超过1M，在开关后，订阅是否能收到变更通知
// * @time 2017-01-22
// * @author liuyu9
// * */
//public class ClientNewSwitchTest extends BaseTest{
//
//	private NamingServiceClient client;
//	private StaticsConfigServiceClient gclient;
//	private String serviceId;
//	private String clusterId;
//	private int port = 1111;	
//	private String switcher = SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION;
//	
//	@Before
//	public void setUp() throws Exception {
//		super.setUp();
//		serviceId = getRandomString(10);
//		clusterId = getRandomString(20);
//		
//		config.setServiceId(serviceId);
//		client = new NamingServiceClient(config);
//		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
//		client.start();
//		SwitcherUtils.setSwitcher(switcher, true);
//		Utils.sleep(10000);
//		init();
//	}
//
//	private void init() {
//		if (!Utils.existService(serviceId)) {
//			Utils.addService(serviceId, "dynamic");
//		}
//		Utils.sleep(10000);
//		if (!Utils.existCluster(serviceId, clusterId)) {
//			Utils.addCluster(serviceId, clusterId);
//		}
//		Utils.sleep(1000);
//		Utils.addWhiltelist(serviceId, localNodes);
//	}
//
//	private void clear() {
//		Utils.deleteWhiltelist(serviceId, this.localNodes);
//		Utils.sleep(1000);
//		Utils.deleteCluster(serviceId, clusterId);
//		Utils.sleep(1000);
//		Utils.deleteService(serviceId);
//		Utils.sleep(serviceCacheInterval);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		clear();
//	}
//	
//	@Test
//	public void testSwitchOpenClose(){
//		int num = 100;
//		assertFalse(SwitcherUtils.switcherIsOpen(switcher));
//		SwitcherUtils.setSwitcher(switcher, true);
//		assertTrue(SwitcherUtils.switcherIsOpen(switcher));
//		for (int i = 0; i < num; i++){
//			if (i % 2 == 0){
//				SwitcherUtils.setSwitcher(switcher, true);
//			} else {
//				SwitcherUtils.setSwitcher(switcher, false);
//			}
//		}
//		assertFalse(SwitcherUtils.switcherIsOpen(switcher));
//		for (int i = 0; i < num; i++){
//			if (i % 2 == 0){
//				SwitcherUtils.setSwitcher(switcher, false);
//			} else {
//				SwitcherUtils.setSwitcher(switcher, true);
//			}
//		}
//		assertTrue(SwitcherUtils.switcherIsOpen(switcher));
//	}
//	
//	/**
//	 * msg >= 1m
//	 * 开关打开，lookup返回消息体超过1M，则不返回
//	 * 实际抛异常
//	 * */
//	@Test
//	public void testSwitchOpenMsgBiggerThan1M(){
//		String extinfo = getRandomString(10000);
//		int num = 200;
//		try{
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//			Set<NamingServiceNode> nodes = client.lookup(serviceId, clusterId);
//			fail("Error: should throws an exception in testSwitchOpenMsgBiggerThan1M");
//		} catch(Exception e){
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//		}finally{
//			for(int j = 0; j < num; j++){
//				Utils.unregister(client, clusterId, localIP, j+1234);
//			}
//		}
//	}
//	
//	/**
//	 * msg < 1m
//	 * 开关打开，查看返回结果，正常返回
//	 * */
//	@Test
//	public void testSwitchOpenMsgSmallerThan1M(){
//		String extinfo = getRandomString(10000);
//		int num = 10;
//		try{
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//			Set<NamingServiceNode> nodes = client.lookup(serviceId, clusterId);
//			assertEquals(nodes.size(), num);
//			
//			SwitcherUtils.setSwitcher(switcher, false);
//			
//			nodes = client.lookup(serviceId, clusterId);
//			assertEquals(nodes.size(), num);
//		} catch(Exception e){
//			e.printStackTrace();
//			fail("Error: failed run in testSwitchOpenMsgSmallerThan1M");
//		}finally{
//			for(int j = 0; j < num; j++){
//				Utils.unregister(client, clusterId, localIP, j+1234);
//			}
//		}
//		
//	}
//	
//	/**
//	 * msg > 1m
//	 * 开关关闭，查看返回结果，正常返回
//	 * */
//	@Test
//	public void testSwitchClosMsgBetween1and2m(){
//		SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION, false);
//		String extinfo = getRandomString(10000);
//		int num = 200;
//		try{
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//			Set<NamingServiceNode> nodes = client.lookup(serviceId, clusterId);
//			System.out.println(nodes.size());
//			assertEquals(nodes.size(), num);
//		} catch(Exception e){
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//			fail("Error: error in testSwitchOpenMsgBiggerThan1M");
//		}finally{
//			for(int j = 0; j < num; j++){
//				Utils.unregister(client, clusterId, localIP, j+1234);
//			}
//		}
//	}
//	
//	/**
//	 * msg > 10m
//	 * 开关关闭，查看返回结果，正常返回
//	 * ？？？？？
//	 * */
//	@Test
//	public void testSwitchClosMsgBiggerThan10m(){
//		SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION, false);
//		String extinfo = getRandomString(10000);
//		int num = 2000;
//		try{
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//			Set<NamingServiceNode> nodes = client.lookup(serviceId, clusterId);
//			System.out.println(nodes.size());
//			assertEquals(nodes.size(), num);
//		} catch(Exception e){
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//			fail("Error: error in testSwitchOpenMsgBiggerThan1M");
//		}finally{
//			for(int j = 0; j < num; j++){
//				Utils.unregister(client, clusterId, localIP, j+1234);
//			}
//		}
//	}
//	
//	
//	// 以下用例是订阅相关用例
//	
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关关闭，msg无限制，查看返回结果和lookup是否一致
//	 */
//	@Test
//	public void testSubSwitchCloseMsgBiggerThan1M() throws InterruptedException {
//		Set<NamingServiceNode> services = new HashSet();
//		Set<NamingServiceNode> nodes;
//		Set<String> wsnodes = new HashSet<String>();
//		String extinfo = getRandomString(10000);
//		int num = 200;
//		try {
//			// 设置开关无限制
//			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION, false);
//			// 订阅空的cluster
//			Utils.subscribeNode(client, serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			/**
//			 * 检查client是否收到节点变更通知
//			 */
//			nodes = Utils.currentNodes;
//			assertEquals(num, nodes.size());
//			Set<NamingServiceNode> nServices = client.lookup(clusterId);
//			assertEquals(nServices.size(), num);
//			assertEquals(nodes, nServices);
//
//		} catch (Exception e) {
//			fail("error in testSubSwitchCloseMsgBiggerThan1M");
//		} finally {
//			for (int i = 0; i < num; i++){
//				Utils.unregister(client, clusterId, localIP, 1234+i);
//			}
//			Utils.unsubscribeChange(client, serviceId, clusterId);
//		}
//	}
//
//	
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关关闭，msg无限制，查看返回结果和lookup是否一致
//	 */
//	@Test
//	public void testlookupforupdateSCMsgBiggerThan10M() throws InterruptedException {
//		
//		String extinfo = getRandomString(10000);
//		int num = 1000;
//		try {
//			// 设置开关无限制
//			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION, false);
//			
//			String oldSign = VintageWebUtils.getsign(serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			Set<NamingServiceNode> nodeSet = VintageWebUtils.lookupforupdate(
//					serviceId, clusterId, oldSign);
//			System.out.println(nodeSet.size());
//			assertEquals(nodeSet.size(), num);
//
//		} catch (Exception e) {
//			fail("error in testSubSwitchCloseMsgBiggerThan10M");
//		} finally {
//			
//		}
//	}
//	
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关关闭，msg无限制，查看返回结果和lookup是否一致
//	 * ？？？？？后遗症
//	 */
//	@Test
//	public void testSubSwitchCloseMsgBiggerThan10M() throws InterruptedException {
//		Set<NamingServiceNode> services = new HashSet();
//		Set<NamingServiceNode> nodes;
//		Set<String> wsnodes = new HashSet<String>();
//		String extinfo = getRandomString(10000);
//		int num = 1000;
//		try {
//			// 设置开关无限制
//			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION, false);
//			// 订阅空的cluster
//			Utils.subscribeNode(client, serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			/**
//			 * 检查client是否收到节点变更通知
//			 */
//			nodes = Utils.currentNodes;
//			assertEquals(num, nodes.size());
//			Set<NamingServiceNode> nServices = client.lookup(clusterId);
//			assertEquals(nServices.size(), num);
//			assertEquals(nodes, nServices);
//
//		} catch (Exception e) {
//			fail("error in testSubSwitchCloseMsgBiggerThan10M");
//		} finally {
//			for (int i = 0; i < num; i++){
//				Utils.unregister(client, clusterId, localIP, 1234+i);
//			}
//			Utils.unsubscribeChange(client, serviceId, clusterId);
//		}
//	}
//
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关关闭，msg无限制，查看返回结果和lookup是否一致
//	 */
//	@Test
//	public void testSubSwitchCloseMsgSmallerThan1M() throws InterruptedException {
//		Set<NamingServiceNode> services = new HashSet();
//		Set<NamingServiceNode> nodes;
//		Set<String> wsnodes = new HashSet<String>();
//		String extinfo = getRandomString(10000);
//		int num = 10;
//		try {
//			// 设置开关无限制
//			SwitcherUtils.setSwitcher(SwitcherUtils.NAMING_CLIENT_MAX_SIZE_PROTECTION, false);
//			// 订阅空的cluster
//			Utils.subscribeNode(client, serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			/**
//			 * 检查client是否收到节点变更通知
//			 */
//			nodes = Utils.currentNodes;
//			assertEquals(num, nodes.size());
//			Set<NamingServiceNode> nServices = client.lookup(clusterId);
//			assertEquals(nServices.size(), num);
//			assertEquals(nodes, nServices);
//
//		} catch (Exception e) {
//			fail("error in testSubSwitchCloseMsgSmallerThan1M");
//		} finally {
//			for (int i = 0; i < num; i++){
//				Utils.unregister(client, clusterId, localIP, 1234+i);
//			}
//			Utils.unsubscribeChange(client, serviceId, clusterId);
//		}
//	}
//
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关打开，msg<1M，查看返回结果和lookup是否一致
//	 */
//	@Test
//	public void testSubSwitchOpenMsgSmallerThan1M() throws InterruptedException {
//		Set<NamingServiceNode> services = new HashSet();
//		Set<NamingServiceNode> nodes;
//		Set<String> wsnodes = new HashSet<String>();
//		String extinfo = getRandomString(10000);
//		int num = 10;
//		try {
//			// 订阅空的cluster
//			Utils.subscribeNode(client, serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			/**
//			 * 检查client是否收到节点变更通知
//			 */
//			nodes = Utils.currentNodes;
//			assertEquals(num, nodes.size());
//			Set<NamingServiceNode> nServices = client.lookup(clusterId);
//			assertEquals(nServices.size(), num);
//			assertEquals(nodes, nServices);
//
//		} catch (Exception e) {
//			fail("error in testSubSwitchOpenMsgSmallerThan1M");
//		} finally {
//			for (int i = 0; i < num; i++){
//				Utils.unregister(client, clusterId, localIP, 1234+i);
//			}
//			Utils.unsubscribeChange(client, serviceId, clusterId);
//		}
//	}
//	
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关打开，msg<1M，查看返回结果和lookup是否一致
//	 * lookupforupdate抛异常，订阅什么反应？？？
//	 * 
//	 */
//	@Test
//	public void testSubSwitchOpenMsgBiggerThan1M() throws InterruptedException {
//		Set<NamingServiceNode> services = new HashSet();
//		Set<NamingServiceNode> nodes;
//		Set<String> wsnodes = new HashSet<String>();
//		String extinfo = getRandomString(10000);
//		int num = 200;
//		try {
//			// 订阅空的cluster
//			Utils.subscribeNode(client, serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			/**
//			 * 检查client是否收到节点变更通知
//			 */
//			nodes = Utils.currentNodes;
//			assertTrue(nodes.size() < num);
//
//		} catch (Exception e) {
//			fail("error in testSubSwitchOpenMsgBiggerThan1M");
//		} finally {
//			for (int i = 0; i < num; i++){
//				Utils.unregister(client, clusterId, localIP, 1234+i);
//			}
//			Utils.unsubscribeChange(client, serviceId, clusterId);
//		}
//	}
//	
//	/**
//	 * 订阅功能调用lookupforupdate接口
//	 * 测试点：开关关闭，msg无限制，查看返回结果和lookup是否一致
//	 */
//	@Test
//	public void testlookupforupdateSwitchOpenMsgBiggerThan1M() throws InterruptedException {
//		
//		String extinfo = getRandomString(10000);
//		int num = 200;
//		try {
//			
//			String oldSign = VintageWebUtils.getsign(serviceId, clusterId);
//			
//			//节点注册
//			for(int j = 0; j < num; j++){
//				Utils.register(client, serviceId, clusterId, localIP, j+1234, extinfo);
//			}
//
//			Thread.sleep(2 * HEARTBEATINTERVAL);
//
//			Set<NamingServiceNode> nodeSet = VintageWebUtils.lookupforupdate(
//					serviceId, clusterId, oldSign);
//			fail("error in testlookupforupdateSwitchOpenMsgBiggerThan1M");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println(e.getMessage());
//		} finally {
//			for (int i = 0; i < num; i++){
//				Utils.unregister(client, clusterId, localIP, 1234+i);
//			}
//		}
//	}
//	
//}
