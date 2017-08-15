package com.weibo.qa.vintage3.Exception.testcases;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.model.NamingServiceType;
import com.weibo.vintage.utils.PrefixUtils;
import com.weibo.vintage.utils.ServerWebUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;
import com.weibo.vintage.utils.VintageConstantsTest;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class MultiIDCDataConsistentTest extends BaseTest{

	private int num = 100;
	String groupString;
	String keyString;
	String valueString;
	String host = "10.77.9.51:8888";
	
	String host1 = "10.210.130.46:7000";
	String host2 = "10.210.130.47:7000";
	String host3 = "10.210.130.47:7001";
	
	String host4 = "10.77.9.51:8080";
	String host5 = "10.77.9.52:8080";
	String host6 = "10.13.216.128:8080";
	
	String host7 = "10.13.1.135:7000";
	String host8 = "10.77.9.162:7000";
	String host9 = "10.77.9.162:7001";
		
	static String keyTemp = "";
	String serviceId, clusterId;
	int serviceCacheInterval = VintageConstantsTest.serviceCacheInterval;

	
	@Before
	public void init(){
		groupString = "vintage.test.qa.liuyu9";
		keyString = getRandomString(10);
		valueString = getRandomString(30);
		serviceId = "vintage-test-qa-liuyu9-test-"+keyString;
		clusterId = "com.weibo.vintage.test.qa.liuyu9.RandomService."+valueString;
		ServerWebUtils.setHeartbeatHost(VintageConstantsTest.IP+":"+VintageConstantsTest.PORT);
	}
	
	@Test
	public void AddOneIDCwhenOIDCChangeMasterTest(){
		int num = 1000;
		String tempGroup = groupString;
		for (int i = 0; i < num; i++){
			if (i%10==0){
				tempGroup = groupString+i;
			}
			VintageConfigWebUtils.register(tempGroup, keyString+i, valueString+i);
		}
	}
	
	@Test
	public void WriteThreeIDCDataforOneGroupTest() {
		for (int i = 0; i < num; i++){
			if (i%3 == 0){
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host1, "config"), groupString, keyString+i, valueString+i);
			} else if (i%3 == 1){
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host4, "config"), groupString, keyString+i, valueString+i);
			} else {
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host7, "config"), groupString, keyString+i, valueString+i);
			}
		}
		
		String result2 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host2, "config"), groupString);
		String result5 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host5, "config"), groupString);
		String result8 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host8, "config"), groupString);
		
		assertEquals(result2, result5);
		assertEquals(result2, result8);
		keyTemp = keyString;
	}
	
	@Test
	public void WriteThreeIDCDataforMultiGroupTest() {
		for (int i = 0; i < num; i++){
			if (i%3 == 0){
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host1, "config"), groupString+i, keyString+i, valueString+i);
			} else if (i%3 == 1){
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host4, "config"), groupString+i, keyString+i, valueString+i);
			} else {
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host7, "config"), groupString+i, keyString+i, valueString+i);
			}
		}
		
		for (int i = 0; i < num; i++) {
			String result2 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host2, "config"), groupString+i);
			String result5 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host5, "config"), groupString+i);
			String result8 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host8, "config"), groupString+i);
			assertEquals(result2, result5);
			assertEquals(result2, result8);
		}
		keyTemp = keyString;
	}
	
	@Test
	public void DeleteOneIDCDataforOneGroupTest() {
		for (int i = 0; i < num; i++){
			if (i%3 == 0){
				VintageConfigWebUtils.unregister(PrefixUtils.getUrlPrefix(host1, "config"), groupString, keyTemp+i);
			}
		}
		
		String result2 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host2, "config"), groupString);
		String result5 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host5, "config"), groupString);
		String result8 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host8, "config"), groupString);
		assertEquals(result2, result5);
		assertEquals(result2, result8);
	}
	
	@Test
	public void AddOneIDCDataforOneGroupTest() {
		for (int i = 0; i < num; i++){
			if (i%3 == 0){
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host1, "config"), groupString, keyString+i, valueString+i);
			}
		}
		
		String result2 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host2, "config"), groupString);
		String result5 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host5, "config"), groupString);
		String result8 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host8, "config"), groupString);
		assertEquals(result2, result5);
		assertEquals(result2, result8);
	}
	
	@Test
	public void DeleteOtherIDCAddDataTest() {
		for (int i = 0; i < num; i++){
			if (i%3 == 0){
				VintageConfigWebUtils.register(PrefixUtils.getUrlPrefix(host1, "config"), groupString, keyString+i, valueString+i);
			}
		}
		
		for (int i = 0; i < num; i++) {
			if (i%3 == 0){
				VintageConfigWebUtils.unregister(PrefixUtils.getUrlPrefix(host4, "config"), groupString, keyString+i);
			}
		}
		
		String result2 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host2, "config"), groupString);
		String result5 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host5, "config"), groupString);
		String result8 = VintageConfigWebUtils.lookupForAll(PrefixUtils.getUrlPrefix(host8, "config"), groupString);
		assertEquals(result2, result5);
		assertEquals(result2, result8);
	}
	
	@Test
	public void NodeStatusChangeTest() {
		try {
			for(int i = 0; i < num/10; i ++ ){
				if (! VintageNamingWebUtils.existsService(serviceId+i)){
					VintageNamingWebUtils.addService(serviceId+i, NamingServiceType.dynamic.toString(), false);
					VintageNamingClientUtils.sleep(serviceCacheInterval);
				}
				for (int j = 0; j < num/10; j++) {
					if (! VintageNamingWebUtils.existCluster(serviceId+i, clusterId+j)) {
						VintageNamingWebUtils.addCluster(serviceId+i, clusterId+j);
					}
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.HeartBeatStatus(serviceId+i, clusterId+j, startPort, "10", "allunreachable");
					VintageNamingClientUtils.sleep(HEARTBEATINTERVAL);
				}
			}
			VintageNamingClientUtils.sleep(60 * HEARTBEATINTERVAL);
			for(int i = 0; i < num/10; i ++ ){
				for (int j = 0; j < num/10; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.stopHeartBeat(startPort);
				}
			}
			for(int i = 0; i < num/10; i ++ ){
				for (int j = 0; j < num/10; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.HeartBeatStatus(serviceId+i, clusterId+j, startPort, "10", "allworking");
				}
			}
			VintageNamingClientUtils.sleep(60 * HEARTBEATINTERVAL);
			for(int i = 0; i < num/10; i ++ ){
				for (int j = 0; j < num/10; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.stopHeartBeat(startPort);
				}
			}
			
			VintageNamingClientUtils.sleep(60 * HEARTBEATINTERVAL);
			for(int i = 0; i < num/10; i ++ ){
				for (int j = 0; j < num/10; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.HeartBeatStatus(serviceId+i, clusterId+j, startPort, "10", "allworking");
				}
			}
		} catch (Exception e){
			
		} finally {
			for(int i = 0; i < num/10; i ++ ){
				for (int j = 0; j < num/10; j++) {
					String startPort = String.valueOf(10000 + i*100 + j*10);
					ServerWebUtils.stopHeartBeat(startPort);
				}
			}
		}
	}
}


