package com.weibo.qa.vintage.online.naming.testcases;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.weibo.qa.vintage.liuyu.testcases.BaseTest;
import com.weibo.vintage.model.NamingServiceCluster;
import com.weibo.vintage.model.NamingServiceInfo;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;

public class GetMethodTest extends BaseTest{

	@Test
	public void testGetService() {
		Set<NamingServiceInfo> services = VintageNamingWebUtils.getServiceInfoSet();
		for(NamingServiceInfo service:services){
			Set<String> whitelists = VintageNamingWebUtils.getWhiteList(service.getName());
			System.out.println("service="+service.getName()+" whitelists="+whitelists);
			Set<NamingServiceCluster> clusters = VintageNamingWebUtils.getCluster(service.getName());
			for (NamingServiceCluster cluster:clusters){
				String sign = VintageNamingWebUtils.getsign(service.getName(), cluster.getClusterId());
				String nodes = VintageNamingWebUtils.lookup(service.getName(), cluster.getClusterId());
				System.out.println("service="+service.getName()+" cluster="+cluster.getClusterId()+" sign="+sign+" lookup="+nodes);
			}
		}
	}
	
	@Test
	public void testGetGroup() {
		List<String> groups = VintageConfigWebUtils.getgroup();
		for (int i = 0; i < groups.size(); i++) {
			String group = groups.get(i);
			String sign = VintageConfigWebUtils.getsign(group);
			List<String> keys = VintageConfigWebUtils.getkeys(group);
			System.out.println("group="+group+" sign="+sign+" keys="+keys);
			for (int j = 0; j < keys.size(); j++) {
				String key = keys.get(j);
				String value = VintageConfigWebUtils.lookup(group, key).get(0);
				System.out.println("group="+group+" key="+key+" value="+value);
			}
		}
	}
}
