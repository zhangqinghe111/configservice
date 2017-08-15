package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.weibo.vintage.model.StaticsConfigMap;
import com.weibo.vintage.utils.JsonHelper;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.qa.vintage.naming.testcases.BaseTest;
import com.weibo.vintage.utils.VintageConfigWebUtils;

	public class LookupForUpdateTest extends BaseTest {
		private String groupId;
		private String noexistgro;
		private String key;
		private String value;
		@Before
		public void setUp() throws Exception {
			super.setUp();
			groupId = getRandomString(10);
			noexistgro=getRandomString(10);
			key = getRandomString(20);
			value = getRandomString(20);
			
			
			init();
	}
		private void init() {
			if (VintageConfigWebUtils.lookupForAll(groupId)==null) {
				VintageConfigWebUtils.register(groupId, key, value);
				
			}
			if (VintageConfigWebUtils.getkeys(groupId)==null) {
				VintageConfigWebUtils.register(groupId, key, value);
			}
		}

		private void clear() {
			VintageConfigWebUtils.unregister(groupId, key);
		}
		@After
		public void tearDown() throws Exception {
			clear();

		}
		@Test
		//注册一个节点
		public void testRegsiterOneNode() {
			try {
				String oldsign = VintageConfigWebUtils.getsign(groupId);
				
				VintageConfigWebUtils.register(groupId, key, value);
				
				String newsign = VintageConfigWebUtils.getsign(groupId);
				
				StaticsConfigMap staticConfig =VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
				
				assertEquals(1, staticConfig.getMaps().size());
				
				staticConfig = VintageConfigWebUtils.lookupForUpdate(groupId, newsign);
				
				assertNull(staticConfig);
				
			
				
			} finally {
				VintageConfigWebUtils.unregister(groupId, key);
				// TODO: handle finally clause
			}
		
		}
		//注册10个节点
		@Test
		public void testRegsiterMultiNode() {
			int num =10;
			try {
				String oldsign=VintageConfigWebUtils.getsign(groupId);
				
				VintageConfigWebUtils.batchregister(groupId, key, value, 1, num);
				
				String newsign =VintageConfigWebUtils.getsign(groupId);
				
				StaticsConfigMap staticConfig =VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
				assertEquals(num, staticConfig.getMaps().size());
				staticConfig=VintageConfigWebUtils.lookupForUpdate(groupId, newsign);
				assertNull(staticConfig);
				
			} finally {
				VintageConfigWebUtils.batchunregister(groupId, key, 0, num);
				// TODO: handle finally clause
			}
			
		}
		//取消注册10个节点
		@Test
		public void testUnRegsiterMultiNode() {
			int num=10;
			try {
				VintageConfigWebUtils.batchregister(groupId, key, value, 1, num);
				String oldsign =VintageConfigWebUtils.getsign(groupId);
				for (int i = 0; i < num-1; i++) {
					VintageConfigWebUtils.unregister(groupId, key+i);
					String newsign = VintageConfigWebUtils.getsign(groupId);
					StaticsConfigMap staticConfig =VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
					assertEquals(num-i-1, staticConfig.getMaps().size());
					staticConfig=VintageConfigWebUtils.lookupForUpdate(groupId, newsign);
					assertNull(staticConfig);
					oldsign=newsign;
					
				}
				
			} finally {
				VintageConfigWebUtils.batchunregister(groupId, key, 0, num);
				// TODO: handle finally clause
			}
		}
		//修改一个配置项
		@Test
		public void testModifyNode() {
			String extinfo = "extInfoAnother";
			try {
				VintageConfigWebUtils.register(groupId, key, value);
				String oldsign=VintageConfigWebUtils.getsign(groupId);
				VintageConfigWebUtils.register(groupId, key, value+extinfo);
				String newsign=VintageConfigWebUtils.getsign(groupId);
				StaticsConfigMap staticConfig =VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
				assertEquals(1, staticConfig.getMaps().size());
				staticConfig=VintageConfigWebUtils.lookupForUpdate(groupId, newsign);
				assertNull(staticConfig);
				
				
			} finally {
				VintageConfigWebUtils.unregister(groupId, key);
				// TODO: handle finally clause
			}
		}
		//没有配置项的情况
		@Test
		public void testNoConf() {
			String sign=VintageConfigWebUtils.getsign(groupId);
			StaticsConfigMap staticsConfig =VintageConfigWebUtils.lookupForUpdate(groupId, sign);
			assertNull(staticsConfig);
			
		}
		//配置项注册又取消注册
		@Test
		public void testRegUnreg() {
			String sign=VintageConfigWebUtils.getsign(groupId);
			VintageConfigWebUtils.register(groupId, key, value);
			VintageConfigWebUtils.unregister(groupId, key);
			StaticsConfigMap staticsConfig = VintageConfigWebUtils.lookupForUpdate(groupId, sign);
			assertNull(staticsConfig);
			
		}
		//注册group1下的配置文件，然后查group2的lookupforupdate
		@Test
		public void testdiffgroup() {
			String groupId2=groupId+"123";
			try {
				String oldsign=VintageConfigWebUtils.getsign(groupId2);
				VintageConfigWebUtils.register(groupId, key, value);
				StaticsConfigMap staticsConfig =VintageConfigWebUtils.lookupForUpdate(groupId2, oldsign);
				assertNull(staticsConfig);
				
				
				
			} finally {
				VintageConfigWebUtils.unregister(groupId, key);
				// TODO: handle finally clause
			}
			
		}
		//测试不存在的group
		@Test
		public void testNoExist() {
			String sign = VintageConfigWebUtils.getsign(noexistgro);
			try {
				StaticsConfigMap staticsConfig =VintageConfigWebUtils.lookupForUpdate(noexistgro, sign);
				System.out.println(staticsConfig);
				assertNull(staticsConfig);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		//重复操作，测试接口稳定性
		@Test
		public void test304lookupforupdateRepeatRatio() {
			String groupId2=groupId+"123";
			try {
				VintageConfigWebUtils.register(groupId, key, value);
				String oldsign=VintageConfigWebUtils.getsign(groupId);
				StaticsConfigMap staticsConfig =VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
				assertNull(staticsConfig);
				for(int i=0;i<20;i++) {
					staticsConfig=VintageConfigWebUtils.lookupForUpdate(groupId, oldsign);
					assertNull(staticsConfig);
				}
				VintageConfigWebUtils.register(groupId2, key, value);
				String newsign =VintageConfigWebUtils.getsign(groupId2);
				
				for(int i=1;i<=20;i++) {
					staticsConfig=VintageConfigWebUtils.lookupForUpdate(groupId2, newsign);
					System.out.println("========lookupforupdate repeat: " + i+" times");
					assertEquals(1, staticsConfig.getMaps().size());
				}
			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				VintageConfigWebUtils.unregister(groupId, key);
				VintageConfigWebUtils.unregister(groupId2, key);
			}
		}
		
}
