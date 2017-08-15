package com.weibo.qa.vintage3.Exception.testcases;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.model.NamingServiceCluster;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;
import com.weibo.vintage.utils.VintageConstantsTest;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class NodeExceptionTests extends BaseTest{

	private int num = 10;
	String groupString;
	String keyString;
	String valueString;
	
	@Before
	public void init(){
		serviceId = "vintage-test-qa-liuyu9-test";
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService/service";
		groupString = "vintage.test.qa.liuyu9";
		keyString = getRandomString(10);
		valueString = getRandomString(30);
		ServerWebUtils.setHeartbeatHost(VintageConstantsTest.IP+":"+VintageConstantsTest.PORT);
	}
	
	//  10个service，每个service有10个cluster，每个cluster下10个node
	@Test
	public void NodeExceptiontententenTest() {
		try {
		for(int i = 0; i < num; i ++ ){
			if (! VintageNamingWebUtils.existsService(serviceId+i)){
				VintageNamingWebUtils.addService(serviceId+i, NamingServiceType.dynamic.toString());
				VintageNamingClientUtils.sleep(serviceCacheInterval);
			}
			for (int j = 0; j < num; j++) {
				if (! VintageNamingWebUtils.existCluster(serviceId+i, clusterId+j)) {
					VintageNamingWebUtils.addCluster(serviceId+i, clusterId+j);
				}
				String startPort = String.valueOf(10000 + i*100 + j*10);
				ServerWebUtils.HeartBeatStatus(serviceId+i, clusterId+j, startPort, "10", "heartbeatshake");
				VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			}
		}
		VintageNamingClientUtils.sleep(600 * HEARTBEATINTERVAL);
		} catch (Exception e){
			
		} finally {
			for(int i = 0; i < num; i ++ ){
				for (int j = 0; j < num; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.stopHeartBeat(startPort);
				}
			}
		}
	}
	
	/**
	 * 用于暴力测试，模拟server端网络抖动导致节点状态变更
	 * */
	@Test
	public void NodeExceptionthousandsTest() {
		try{
		if (! VintageNamingWebUtils.existsService(serviceId)){
			VintageNamingWebUtils.addService(serviceId, NamingServiceType.dynamic.toString());
			VintageNamingClientUtils.sleep(serviceCacheInterval);
		}
		if (! VintageNamingWebUtils.existCluster(serviceId, clusterId)) {
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
		
		for (int i = 0; i < num; i++) {
			for (int j = 0; j < num; j++) {
				String startPort = String.valueOf(10000 + i*100 + j*10);
				ServerWebUtils.HeartBeatStatus(serviceId, clusterId, startPort, "10", "heartbeatshake");
				VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			}
		}
		VintageNamingClientUtils.sleep(600 * HEARTBEATINTERVAL);
		} catch(Exception e){
			
		} finally {
			for (int i = 0; i < num; i++) {
				for (int j = 0; j < num; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.stopHeartBeat(startPort);
				}
			}
		}
	}
	
	/**
	 * 用于测试config的数据一致性
	 * */
	@Test
	public void WriteConfigTest() {
		for (int i = 0; i < num/100; i++){
			for (int j = 0; j < num; j++){
				assertTrue(VintageConfigWebUtils.register(groupString+i, keyString+j, valueString+j));
//				assertTrue(VintageConfigWebUtils.register(groupString+i, keyString+j, valueString+"_new1"+j));
//				assertTrue(VintageConfigWebUtils.register(groupString+i, keyString+j, valueString+"_new2"+j));
//				assertTrue(VintageConfigWebUtils.register(groupString+i, keyString+j, valueString+"_new3"+j));
//				assertTrue(VintageConfigWebUtils.register(groupString+i, keyString+j, valueString+"_new4"+j));

//				List<String>  nodeList = VintageConfigWebUtils.lookup(groupString+i, keyString+j);
//				assertEquals(VintageConfigWebUtils.getConfigMap(nodeList).get(keyString+j), valueString+j);		
//
//				List<String> nodesList = VintageConfigWebUtils.lookup(groupString+i);
//				Map<String, String> nodesMap = VintageConfigWebUtils.getConfigMap(nodesList);
//				assertEquals(nodesMap.get(keyString+j), valueString+j);		
			}
		}
		
		for (int i = 0; i < num/100; i++){
			for (int j = 0; j < num; j++){
				assertTrue(VintageConfigWebUtils.unregister(groupString+i, keyString+j));
				List<String>  unregnodeList = VintageConfigWebUtils.lookup(groupString+i, keyString+j);
				assertTrue(unregnodeList==null || unregnodeList.size() == 0 || unregnodeList.isEmpty());
			}
		}
	}
	
	/**
	 * 用于测试naming的数据一致性
	 * */
	@Test
	public void WriteNamingTest() throws InterruptedException {
		String nodesPrefix = "10.2.3.";
		int port = 10000;
		for (int i = 0; i < num/100; i++){
			if (!VintageNamingWebUtils.existsService(serviceId)){
				assertTrue(VintageNamingWebUtils.addService(serviceId+i, "dynamic"));
			}
			Map<String, NamingServiceInfo> serviceMap = VintageNamingWebUtils.getServiceInfoMap();
			assertEquals(serviceMap.get(serviceId+i).getType().toString(), "dynamic");
			assertEquals(serviceMap.get(serviceId+i).getProtectThreshold(), 0.6, 0.1);
			for (int j = 0; j < num/100; j++){
				assertTrue(VintageNamingWebUtils.addWhitelist(serviceId+i, nodesPrefix+j));
				Set<String> WhitelistnodeSet = VintageNamingWebUtils.getWhiteList(serviceId+i);
				assertTrue(WhitelistnodeSet.toString().contains(nodesPrefix+j));
			}
			for (int j = 0; j < num; j++){
				assertTrue(VintageNamingWebUtils.addCluster(serviceId+i, clusterId+j));
				Set<NamingServiceCluster> clusterSet = VintageNamingWebUtils.getCluster(serviceId+i);
				assertTrue(clusterSet.toString().contains(clusterId+j));
				for (int k = 0; k < num/100; k++){
					for (int z = 0; z < num/100; z++){
						VintageNamingWebUtils.register(serviceId+i, clusterId+j, nodesPrefix+k, port+z);
						Set<String> nodesSet = VintageNamingWebUtils.lookup_set(serviceId+i, clusterId+j, "unreachable");
						assertEquals(nodesSet.size(), (k+1)*(z+1));
					}
				}
			}
		}
		
		for (int i = 0; i < num/100; i++){
			for (int j = 0; j < num; j++){
				for (int k = 0; k < num/100; k++){
					for (int z = 0; z < num/100; z++){
						VintageNamingWebUtils.unregister(serviceId+i, clusterId+j, nodesPrefix+k, port+z);
						Set<String> nodesSet = VintageNamingWebUtils.lookup_set(serviceId+i, clusterId+j, "unreachable");
						assertEquals(nodesSet.size(), (num*num)/10000-(k+1)*(z+1));
					}
				}
			}
		}
		
		for (int i = 0; i < num/100; i++){
			for (int j = 0; j < num; j++){
				assertTrue(VintageNamingWebUtils.deleteCluster(serviceId+i, clusterId+j));
				assertEquals(VintageNamingWebUtils.getCluster(serviceId+i).size(), num-1-j);
			}
			assertTrue(VintageNamingWebUtils.deleteService(serviceId+i));
			assertEquals(VintageNamingWebUtils.getServiceInfoSet().size(), num/100-1-i);
		}
	}
	
	/**
	 * 用于测试流量风暴：IDC网络分区+server端网络抖动
	 * */
	@Test
	public void registerNodeTest() {
//		int num = 10;
		String prefix = "http://10.77.9.51:8888/naming/service";
		
		String prefix1 = "http://10.210.130.47:7000/naming/service";
		String prefix2 = "http://10.210.130.46:7000/naming/service";
		String prefix3 = "http://10.77.9.162:7000/naming/service";

		String prefix4 = "http://10.77.9.51:8080/naming/service";
		String prefix5 = "http://10.77.9.52:8080/naming/service";
		String prefix6 = "http://10.13.216.128:8080/naming/service";

		for(int i = 0; i < num; i ++ ){
			if (! VintageNamingWebUtils.existsService(serviceId+i)){
				VintageNamingWebUtils.addService(serviceId+i, NamingServiceType.dynamic.toString());
				VintageNamingClientUtils.sleep(serviceCacheInterval);
			}
			for (int j = 0; j < num; j++) {
				if (! VintageNamingWebUtils.existCluster(serviceId+i, clusterId+j)) {
					VintageNamingWebUtils.addCluster(serviceId+i, clusterId+j);
				}
				String startPort = String.valueOf(10000 + i*100 + j*10);
				ServerWebUtils.HeartBeatStatus(serviceId+i, clusterId+j, startPort, "10", "allworking");
				VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
			}
		}
	}
	
	@Test
	public void lookupforupdateDivideTest() {
		String prefix = "http://10.77.9.51:8888/naming/service";
		
		String prefix1 = "http://10.210.130.47:7000/naming/service";
		String prefix2 = "http://10.210.130.46:7000/naming/service";
		String prefix3 = "http://10.77.9.162:7000/naming/service";

//		String prefix4 = "http://10.77.9.51:8080/naming/service";
		String prefix5 = "http://10.77.9.52:8080/naming/service";
		String prefix6 = "http://10.13.216.128:8080/naming/service";
		
		String[][] signs = new String[num][num];
		for(int i = 0; i < num; i ++ ){
			for (int j = 0; j < num; j++){
				signs[i][j] = VintageNamingWebUtils.getsign(prefix1, serviceId+i, clusterId+j);
			}
		}
		
		while(true){
			try {
				for(int i = 0; i < num; i ++ ){
					for (int j = 0; j < num; j++){
						String tmpsign = signs[i][j];
						NamingServiceCluster lfu_result1 = VintageNamingWebUtils.lookupforupdate(prefix1, serviceId+i, clusterId+j, tmpsign);
						if (lfu_result1 != null) {
							signs[i][j] = lfu_result1.getSign();
						}
						NamingServiceCluster lfu_result2 = VintageNamingWebUtils.lookupforupdate(prefix2, serviceId+i, clusterId+j, tmpsign);
						if (lfu_result2 != null) {
							signs[i][j] = lfu_result2.getSign();
						}
						NamingServiceCluster lfu_result3 = VintageNamingWebUtils.lookupforupdate(prefix3, serviceId+i, clusterId+j, tmpsign);
						if (lfu_result3 != null) {
							signs[i][j] = lfu_result3.getSign();
						}
//						NamingServiceCluster lfu_result4 = VintageNamingWebUtils.lookupforupdate(prefix4, serviceId+i, clusterId+j, tmpsign);
//						if (lfu_result4 != null) {
//							signs[i][j] = lfu_result4.getSign();
//						}
						NamingServiceCluster lfu_result5 = VintageNamingWebUtils.lookupforupdate(prefix5, serviceId+i, clusterId+j, tmpsign);
						if (lfu_result5 != null) {
							signs[i][j] = lfu_result5.getSign();
						}
						NamingServiceCluster lfu_result6 = VintageNamingWebUtils.lookupforupdate(prefix6, serviceId+i, clusterId+j, tmpsign);
						if (lfu_result6 != null) {
							signs[i][j] = lfu_result6.getSign();
						}
						Thread.sleep(HEARTBEATINTERVAL);
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}