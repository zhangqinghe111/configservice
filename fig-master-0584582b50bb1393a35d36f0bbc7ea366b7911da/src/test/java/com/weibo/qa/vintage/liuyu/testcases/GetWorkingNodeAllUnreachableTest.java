package com.weibo.qa.vintage.liuyu.testcases;

import static org.junit.Assert.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

/**
 * 此类为测试获取可用节点策略
 * 原逻辑，当所有节点都不可用时，采取设置40%节点保持为working状态，此策略是全局的
 * 新逻辑，不同group百分比不同，百分比可配
 * 
 * 服务端心跳保护策略开关
 * 关闭：实时显示node节点状态
 * workingSize = clientSize - (int) Math.ceil(clientSize * ratio)
 * 
 * @author liuyu9
 * 
 * 经常出现一种现象：设置完threshold后，getthreshold验证成功
 * 但是getworkinglist后，所得结果非设置的threshold阈值，而是5
 * 设想：由于需要经过时间周期才更新内存(更新时，直接写redis，时间周期后从redis读入内存)
 * 节点摘除时首先读入未更新threshold，也就是默认的0.4，经过时间周期后，阈值变更，节点摘除
 * 因此出现结果不一致现象？？？等待验证
 */
public class GetWorkingNodeAllUnreachableTest extends BaseTest{

	private int port = 1234;
	private String extinfo = "extinfo";
	private String dType = "dynamic";
	private NamingServiceClient client = null;
	private double ratio = 0.6;  // 默认值
	private double threshold = 0.0;
	private DecimalFormat df = new DecimalFormat("0.0");
	Random random = new Random();

	@Before
	public void setUp() throws Exception {
		
		serviceId = getRandomString(10);
		clusterId = getRandomString(20);
		super.setUp();
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();	
		
		threshold = Double.valueOf(df.format(random.nextDouble()));
		
		init(serviceId, clusterId, threshold);
		
		ServerWebUtils.HeartbeatProtection("on");
		VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
	}

	@After
	public void tearDown() throws Exception {
		clean(serviceId, clusterId);
		super.tearDown();
	}
	
	/**
	 * 测试点：本用例用于测试默认ratio，
	 * 以及设置ratio后，对后续添加cluster等操作是否有影响
	 * 操作：
	 * 校验ratio比例
	 * 设置ratio
	 * 校验ratio
	 * 添加cluster、查看ratio比例
	 * 设置新设置cluster的ratio，查看俩cluster各自的ratio
	 * 
	 * */
	@Test
	public void testDefaultSettingRatio(){
		String ser = "testDefaultSettingRatio";
		
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.addService(ser, dType);
			VintageNamingClientUtils.sleep(serviceCacheInterval);
			VintageNamingWebUtils.addCluster(ser, clusterId);
			VintageNamingWebUtils.addWhitelist(ser, localNodes);
			
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, ser, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, ser, clusterId);
			}
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						clientSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								ser, clusterId).size());
			}
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(60 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * ratio);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								ser, clusterId).size());
			}
			
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, ser, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, ser,
						clusterId);
			}
			clean(ser, clusterId);
		}		
	}
	

	@Test
	public void testAddRatio(){
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

			closeSwitch();
			
			VintageNamingClientUtils.sleep(60 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}			
		} catch(Exception e){
			fail("ERROR in testAddRatio()");
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为0、负数、非数字字符、越界、为空等异常测试
	 * 
	 * */
	@Test
	public void testUpdateRatioIgnore1(){
		int clientSize = 10;
		double threshold= 0;	
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}			
			
		} catch (Exception e){
			fail("ERROR in testUpdateRatioIgnore1()");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为0.9
	 * 
	 * @bug threshold设置为0.9，期望workingnodelist=0，实际为1
	 * 
	 * */
	@Test
	public void testUpdateRatioIgnore2(){
		int clientSize = 10;
		double threshold= 0.9;	
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}			
			
		} catch (Exception e){
			fail("");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为1
	 * 
	 * */
	@Test
	public void testUpdateRatioIgnore3(){
		int clientSize = 10;
		double threshold= 1;	
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}			
		} catch (Exception e){
			fail("ERROR in testUpdateRatioIgnore3()");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为大于1  
	 * 大于0.9就是百分百下掉node
	 * 
	 * */
	@Test
	public void testUpdateRatioIgnore4(){
		int clientSize = 10;
		double threshold= 10;	
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			fail("ERROR in testUpdateRatioIgnore4()");
		} catch (Exception e){
			System.out.println("Success: testUpdateRatioIgnore4");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为负数，小于0就是百分百不下
	 * 
	 * */
	@Test
	public void testUpdateRatioIgnore5(){
		int clientSize = 10;
		double threshold= -10;	
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			fail("ERROR in testUpdateRatioIgnore5()");
		} catch (Exception e){
			System.out.println("Success: testUpdateRatioIgnore4");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为非数字字符，设置失败，使用默认参数
	 * 
	 * */
	@Test
	public void testUpdateRatioIgnore6(){
		int clientSize = 10;
		String t = "lala";
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			VintageNamingWebUtils.updateThreshold(serviceId, t);
			fail("ERROR in testUpdateRatioIgnore6()");
		} catch (Exception e){
			System.out.println("Success: testUpdateRatioIgnore6");
		}
		
		try{
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
		} catch(Exception e){
			fail("ERROR in testUpdateRatioIgnore6()");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	/**
	 * 测试点：测试setRatio接口
	 * 操作：
	 * ration设置为非数字字符，设置失败，使用默认参数
	 * 
	 * */
	@Test
	public void testUpdateRatioGetThreshold(){
		for (int i = 0; i < 10; i ++){
			String tratio = df.format(random.nextDouble());
			VintageNamingWebUtils.updateThreshold(serviceId, tratio);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
			System.out.println(VintageNamingWebUtils.getThreshold(serviceId));
			assertEquals(String.valueOf(Double.valueOf(tratio)), VintageNamingWebUtils.getThreshold(serviceId));
		}
	}
	
	/**
	 * 测试点：重复设置ratio
	 * 操作：
	 * 给一个cluster重复设置ratio10次
	 * 校验：
	 * 查看ratio是否满足最后一次设置
	 * 
	 * */
	@Test
	public void testSettingRepeat(){
		Random random = new Random();
		double tratio = 0.0;
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();

		try {
			for (int i = 0; i < 5; i ++){
				String ttt = df.format(random.nextDouble());
				tratio = Double.parseDouble(ttt);
				VintageNamingWebUtils.updateThreshold(serviceId, tratio);
				VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
				System.out.println(VintageNamingWebUtils.getThreshold(serviceId));
				assertEquals(String.valueOf(tratio), VintageNamingWebUtils.getThreshold(serviceId));
			}
		
			
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
				
			openSwitch();
				
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
							
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
				
			closeSwitch();
				
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
				
			int workingSize = (int) Math.ceil(clientSize * tratio);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
	} finally {
		port = 1234;
		for (NamingServiceClient namingServiceClient : clientList) {
			VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
					localIP, port++);
			VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
					clusterId);
		}
	}
		
		
	}
	
	/**
	 * 测试点：测试默认配置，如果没有设置时，查看ratio的比例
	 * operation:
	 * 同一个service添加多个cluster，校验每个cluster的ratio
	 * 
	 * */
	@Test
	public void testClusterRatio() {
		String cluId = "testClusterRatioclu";
		VintageNamingWebUtils.addCluster(serviceId, cluId);
		
		Random random = new Random();
		double tratio = 0.0;
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();

		try {
			String ttt = df.format(random.nextDouble());
			tratio = Double.parseDouble(ttt);
			VintageNamingWebUtils.updateThreshold(serviceId, tratio);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);

			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
				
			openSwitch();
				
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				VintageNamingClientUtils.register(namingServiceClient, serviceId, cluId, 
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, cluId);
			}
							
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
				
			closeSwitch();
				
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
				
			int workingSize = (int) Math.ceil(clientSize * tratio);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, cluId).size());
			}
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);	
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, cluId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						cluId);	
			}
			
			VintageNamingWebUtils.deleteCluster(serviceId, cluId);
		}
		
	}
	

	
	
	/**
	 * 测试点：测试默认配置，如果没有设置时，查看ratio的比例
	 * operation:
	 * 同一个service添加多个cluster，校验每个cluster的ratio
	 * 设置其中一个cluster的ratio，校验该cluster以及其他cluster的ratio
	 * 设置其他cluster的ratio，校验各个cluster的ratio
	 * 删除其中一个cluster，校验其他cluster的ratio
	 * 
	 * */
	@Test
	public void testRatioAfterDelOneCluster() {
		String cluId = "testClusterRatioclu";
		VintageNamingWebUtils.addCluster(serviceId, cluId);
		
		Random random = new Random();
		double tratio = 0.0;
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();

		try {
			String ttt = df.format(random.nextDouble());
			tratio = Double.parseDouble(ttt);
			VintageNamingWebUtils.updateThreshold(serviceId, tratio);
			VintageNamingClientUtils.sleep(30 * HEARTBEATINTERVAL);
			assertEquals(ttt, VintageNamingWebUtils.getThreshold(serviceId));
						
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
				
			openSwitch();
				
			// 注册10个节点
			int port1 = 1234;
			int port2 = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port1++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				VintageNamingClientUtils.register(namingServiceClient, serviceId, cluId, 
						localIP, port2++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, cluId);
			}
							
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
				
			closeSwitch();
				
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
				
			int workingSize = (int) Math.ceil(clientSize * tratio);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, cluId).size());
			}
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingWebUtils.unregister(serviceId, cluId, localIP,  port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						cluId);	
			}
			VintageNamingClientUtils.sleep(1000);
			VintageNamingWebUtils.deleteCluster(serviceId, cluId);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingWebUtils.unregister(serviceId, clusterId, localIP,  port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);	
				
			}
		}
	}
	

	

	
	@Test
	public void testServiceHeartBeatOffAllUnreachable2() {
		
		int clientSize = 10;
		
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			ServerWebUtils.HeartbeatProtection("off");
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
						
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
						
			for (NamingServiceClient namingServiceClient : clientList) {
			assertEquals(clientSize,
				VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
					serviceId, clusterId).size());
			}			

			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);

			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);

			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						0,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	
	@Test
	public void testServiceHeartBeatOfftoOnAllUnreachable1() {
		
		int clientSize = 10;
		
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			ServerWebUtils.HeartbeatProtection("off");
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
						
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
						
			for (NamingServiceClient namingServiceClient : clientList) {
			assertEquals(clientSize,
				VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
					serviceId, clusterId).size());
			}			

			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);

			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);

			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						0,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
			ServerWebUtils.HeartbeatProtection("on");
			VintageNamingClientUtils.sleep(30 * HEARTBEATINTERVAL);
			
			// 因为全都是unreachable状态，同初始状态相符
			int workingSize = (int) Math.ceil(clientSize * threshold);		
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						0,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
			openSwitch();
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	

	
	@Test
	public void testbug() {
		int clientSize = 10;		
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();	
		double threshold = 0.7;
		try{
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();

				clientList.add(client);
			}
			
			openSwitch();
			
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
			}
						
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
			
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));

			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int)Math.ceil(clientSize*threshold);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
		} catch(Exception e){
			fail("testThresholdFromBtoS()");
		} finally{
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
		}
	}
	

	protected void init(String service, String cluster, double threshold) {
		VintageNamingWebUtils.addThreshold(service, threshold);
		VintageNamingClientUtils.sleep(5000);
		addCluster(service, cluster);
		VintageNamingClientUtils.sleep(1000);
		addWhiteList(service, localNodes);
	}

	protected void clean(String service, String cluster) {
		delWhiteList(service, localNodes);
		delCluster(service, clusterId);
		VintageNamingClientUtils.sleep(5000);
		VintageNamingWebUtils.delThreshold(service);
		VintageNamingClientUtils.sleep(serviceCacheInterval);
	}

	private int getWorkingNodeSize(int totalsize) {
		return totalsize - (int) Math.ceil(totalsize * ratio + 1);
	}
	

	// 以下方法难为控制node状态的方法
	private void working(int startPort, int size, String extInformation) {
		port = startPort;
		extinfo = extInformation;
		int clientSize = size;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
		try {
			for (int i = 0; i < clientSize; i++) {
					NamingServiceClient client = new NamingServiceClient(config);
					client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
					client.start();

					clientList.add(client);
				}

				openSwitch();

				for (NamingServiceClient namingServiceClient : clientList) {
					VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
							localIP, port++, extinfo);
					VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				}

			} catch (VintageException ex) {
				ex.printStackTrace();
			}
		}

		private void unreachable(int startPort, int size, String extInformation) {
			port = startPort;
			extinfo = extInformation;
			int clientSize = size;
			List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
			try {
				for (int i = 0; i < clientSize; i++) {
					NamingServiceClient client = new NamingServiceClient(config);
					client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
					client.start();

					clientList.add(client);
				}

				closeSwitch();

				for (NamingServiceClient namingServiceClient : clientList) {
					VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
							localIP, port++, extinfo);
					VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				}

				VintageNamingClientUtils.sleep(30 * HEARTBEATINTERVAL);
			} catch (VintageException ex) {
				ex.printStackTrace();
			}
		}

		private void WorkingToUnreachable(int startPort, int size, String extInformation) {
			extinfo = extInformation;
			port = startPort;
			int clientSize = size;
			List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
			try {
				for (int i = 0; i < clientSize; i++) {
					NamingServiceClient client = new NamingServiceClient(config);
					client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
					client.start();

					clientList.add(client);
				}

				openSwitch();

				for (NamingServiceClient namingServiceClient : clientList) {
					VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
							localIP, port++, extinfo);
					VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				}

				VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

				closeSwitch();

				VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

			} catch (VintageException ex) {
				ex.printStackTrace();
			}
		}

		private void WorkingToUnreachableToWorking(int startPort, int size,
				String extInformation) {
			extinfo = extInformation;
			port = startPort;
			int clientSize = size;
			List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
			try {
				for (int i = 0; i < clientSize; i++) {
					NamingServiceClient client = new NamingServiceClient(config);
					client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
					client.start();

					clientList.add(client);
				}

				openSwitch();

				for (NamingServiceClient namingServiceClient : clientList) {
					VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
							localIP, port++, extinfo);
					VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				}

				VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

				closeSwitch();

				VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

				openSwitch();

				VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

			} catch (VintageException ex) {
				ex.printStackTrace();
			}
		}

		private void WorkingToUnreachableToWorkingToUnreachable(int startPort,
				int size, String extInformation) {
			extinfo = extInformation;
			port = startPort;
			int clientSize = size;
			List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();
			try {
				for (int i = 0; i < clientSize; i++) {
					NamingServiceClient client = new NamingServiceClient(config);
					client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
					client.start();

					clientList.add(client);
				}

				openSwitch();

				for (NamingServiceClient namingServiceClient : clientList) {
					VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
							localIP, port++, extinfo);
					VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				}

				VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);

				closeSwitch();

				VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);

				openSwitch();

				VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);

				closeSwitch();

				VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);

			} catch (VintageException ex) {
				ex.printStackTrace();
			}
		}
		
		/*
		 * 以下方法为私有方法
		 */
		private void closeSwitch() {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, false);
		}

		private void openSwitch() {
			SwitcherUtils.setSwitcher(
					SwitcherUtils.NAMING_PROCESS_HEARTBEAT_SWITCHER, true);
		}

}
