package com.weibo.qa.vintage.liuyu.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.client.NamingServiceClient;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.SwitcherUtils;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class GetWorkingNodeAllUnreachableTest2 extends BaseTest{

	private String prefix = getRandomString(10);
	private String serviceId = prefix + "Service";
	private String clusterId = prefix + "Cluster";
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
		super.setUp();
		client = new NamingServiceClient(config);
		client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
		client.start();	
		
		VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
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

	@Test
	public void testServiceHeartBeatOffAllUnreachable() {
		
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
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
			ServerWebUtils.HeartbeatProtection("off");
			VintageNamingClientUtils.sleep(10 * HEARTBEATINTERVAL);
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
	public void testThresholdFromBtoS() {
		int clientSize = 10;		
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();	
		double threshold = 1;
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
			
			for (int i = 9; i > 0; i--){
				String ratio = "0."+i;
				VintageNamingWebUtils.updateThreshold(serviceId, ratio);
				VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
				assertEquals(ratio, VintageNamingWebUtils.getThreshold(serviceId));
				VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
				workingSize = (int)Math.ceil(clientSize*Double.valueOf(ratio));
				assertEquals(workingSize, VintageNamingClientUtils.getWorkingNodeList(clientList.get(0),
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
	
	@Test
	public void testThresholdFromStoB() {
		int clientSize = 10;		
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();	
		double threshold = 0.2;
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
			for (int i = 3; i < 10; i++){
				String temp = "0."+i;
				VintageNamingWebUtils.updateThreshold(serviceId, temp);
				VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
				assertEquals(temp, VintageNamingWebUtils.getThreshold(serviceId));
				VintageNamingClientUtils.sleep(20*HEARTBEATINTERVAL);
				workingSize = (int)Math.ceil(clientSize*threshold);
				assertEquals(workingSize, VintageNamingClientUtils.getWorkingNodeList(clientList.get(0),
								serviceId, clusterId).size());
			}
			
		} catch(Exception e){
			fail("testThresholdFromStoB()");
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
	 * 测试点：测试默认配置，如果没有设置时，查看ratio的比例
	 * operation:
	 * 同一个service添加多个cluster，校验每个cluster的ratio
	 * 
	 * */
	@Test
	public void testServiceRatio() {
		String serId = "testServiceRatioSer";
		init(serId, clusterId, threshold);
		double tratio1 = 0.0;
		double tratio2 = 0.0;
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();

		try {
			String ttt1 = df.format(random.nextDouble());
			tratio1 = Double.parseDouble(ttt1);	
			VintageNamingWebUtils.updateThreshold(serviceId, tratio1);
			String ttt2 = df.format(random.nextDouble());
			tratio2 = Double.parseDouble(ttt2);	
			VintageNamingWebUtils.updateThreshold(serId, tratio2);
			
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
				VintageNamingClientUtils.register(namingServiceClient, serId, clusterId, 
						localIP, port++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serId, clusterId);
			}
			
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
				
			closeSwitch();
				
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
				
			int workingSize1 = (int) Math.ceil(clientSize * tratio1);
			int workingSize2 = (int) Math.ceil(clientSize * tratio2);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize1,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
				assertEquals(
						workingSize2,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serId, clusterId).size());
			}
		} finally {
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);	
				VintageNamingClientUtils.unregister(namingServiceClient, serId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serId,
						clusterId);	
			}
			delWhiteList(serId, localNodes);
			VintageNamingWebUtils.deleteCluster(serId, clusterId);
			VintageNamingClientUtils.sleep(5000);
			VintageNamingWebUtils.deleteService(serId);
		}
		
	}
	
	/**
	 * 测试点：测试默认配置，如果没有设置时，查看ratio的比例
	 * operation:
	 * 添加多个service，每个service添加多个cluster
	 * 校验各个cluster的ratio
	 * 删除其中一个service的所有cluster后，添加一个新的cluster
	 * 校验该新的cluster的ratio
	 * 
	 * */
	@Test
	public void testRatioAfterDelOneService() {
		String serId = "testServiceRatioSer";
		double tratio1 = 0.0;
		double tratio2 = 0.0;
		int clientSize = 10;
		List<NamingServiceClient> clientList = new ArrayList<NamingServiceClient>();

		try {
			String ttt1 = df.format(random.nextDouble());
			tratio1 = Double.parseDouble(ttt1);	
			VintageNamingWebUtils.updateThreshold(serviceId, tratio1);
			String ttt2 = df.format(random.nextDouble());
			tratio2 = Double.parseDouble(ttt2);	
			init(serId, clusterId, tratio2);

			
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);
			
			assertEquals(String.valueOf(Double.valueOf(tratio1)), VintageNamingWebUtils.getThreshold(serviceId));
			assertEquals(String.valueOf(Double.valueOf(tratio2)), VintageNamingWebUtils.getThreshold(serId));

			
			for (int i = 0; i < clientSize; i++) {
				NamingServiceClient client = new NamingServiceClient(config);
				client.setNodeExciseStrategy(new NodeExciseStrategy.Statics());
				client.start();
				clientList.add(client);
			}
				
			openSwitch();
			
			int port1 = 1234;
			int port2 = 1234;
			// 注册10个节点
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.register(namingServiceClient, serviceId, clusterId,
						localIP, port1++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serviceId, clusterId);
				VintageNamingClientUtils.register(namingServiceClient, serId, clusterId, 
						localIP, port2++, extinfo);
				VintageNamingClientUtils.subscribeNode(namingServiceClient, serId, clusterId);
			}
			
			
			VintageNamingClientUtils.sleep(20 * HEARTBEATINTERVAL);
				
			closeSwitch();
				
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
				
			int workingSize1 = (int) Math.ceil(clientSize * tratio1);
			int workingSize2 = (int) Math.ceil(clientSize * tratio2);
//			int workingSize1 = clientSize
//					- (int) Math.ceil(clientSize * tratio1);
//			int workingSize2 = clientSize
//					- (int) Math.ceil(clientSize * tratio2);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize1,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
				assertEquals(
						workingSize2,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serId, clusterId).size());
			}
			port1 = 1234;
			port2 = 1234;
			
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port1++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);	
				VintageNamingClientUtils.unregister(namingServiceClient, serId, clusterId,
						localIP, port2++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serId,
						clusterId);	
			}
			delWhiteList(serId, localNodes);
			delCluster(serId, clusterId);
			VintageNamingClientUtils.sleep(5000);
			delService(serId);
			
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
	public void testNormalSettingRatio1(){
		
		
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
						
			for (NamingServiceClient namingServiceClient : clientList) {
			assertEquals(clientSize,
				VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
					serviceId, clusterId).size());
			}			

			System.out.println("===threshold : "+threshold);
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);

			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
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
	public void testNormalSettingRatio2(){
		
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
						
			for (NamingServiceClient namingServiceClient : clientList) {
			assertEquals(clientSize,
				VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
					serviceId, clusterId).size());
			}			
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
			VintageNamingWebUtils.updateThreshold(serviceId, threshold);
			VintageNamingClientUtils.sleep(10*HEARTBEATINTERVAL);

			assertEquals(String.valueOf(threshold), VintageNamingWebUtils.getThreshold(serviceId));
			
			VintageNamingClientUtils.sleep(30 * HEARTBEATINTERVAL);
			workingSize = (int) Math.ceil(clientSize * threshold);
			
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
	 * 测试点：测试设置ratio后，删除该cluster、后重新添加该cluster
	 * check：之前设置的ratio是否仍然有效
	 * 
	 * */
	@Test
	public void testSettingRatioDelAdd(){
		
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
			
			int workingSize = (int) Math.ceil(clientSize * threshold);
			for (NamingServiceClient namingServiceClient : clientList) {
				assertEquals(
						workingSize,
						VintageNamingClientUtils.getWorkingNodeList(namingServiceClient,
								serviceId, clusterId).size());
			}
			
			port = 1234;
			for (NamingServiceClient namingServiceClient : clientList) {
				VintageNamingClientUtils.unregister(namingServiceClient, serviceId, clusterId,
						localIP, port++);
				VintageNamingClientUtils.unsubscribeChange(namingServiceClient, serviceId,
						clusterId);
			}
			
			clean(serviceId, clusterId);
			assertNull(VintageNamingWebUtils.getThreshold(serviceId));
			init(serviceId, clusterId, threshold);
			
			openSwitch();
			
			port = 1234;
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
			
			closeSwitch();
			
			VintageNamingClientUtils.sleep(40 * HEARTBEATINTERVAL);
			
			workingSize = (int) Math.ceil(clientSize * threshold);
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
	 * 测试点：本用例用于测试给一个不存在的cluster设置ratio的结果；
	 * 以及设置完后，添加该cluster，之前ratio设置是否生效
	 *
	 * 操作：
	 * 设置不存在的service
	 * 查询其他service下的ratio是否正常
	 * 添加该service，添加node，查看ratio
	 * 
	 * */
	@Test
	public void testSettingRatioForNoexist(){
		String testser = "testSettingRatioForNoexistClusterSer";
		try{
		VintageNamingWebUtils.updateThreshold(testser, ratio);
		fail("ERROR in testSettingRatioForNoexist()");
		} catch (Exception e) {
			System.out.println("Success: testSettingRatioForNoexist");	
		}	
		
		testNormalSettingRatio2();
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
