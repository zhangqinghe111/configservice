package com.weibo.qa.vintage.online.config.testcases;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.springframework.jdbc.config.InitializeDatabaseBeanDefinitionParser;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class ConfigLookupTest extends BaseTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		groupString = "vintage.test.qa.liuyu9";
		keyString = getRandomString(10);
		valueString = getRandomString(20);
		cleangroup();
	}

	@After
	public void tearDown() throws Exception {
		VintageConfigWebUtils.unregister(groupString, keyString);
	}
	
	public static void cleangroup() {
		for (int i = 0; i < 4; i++) {
			String group = groupString;
			if (i != 0){
				group = group+i;
			}
			List<String> keys = VintageConfigWebUtils.getkeys(group);
			for (String key : keys){
				VintageConfigWebUtils.unregister(group, key);
			}
		}
		
	}

	/*
	 * repeat lookup for many times
	 */
	@Test
	public void testRepeatLookup() {
		int init_env = VintageConfigWebUtils.lookup(groupString).size();
		for (int i = 0; i < 100; i++) {
			assertEquals(init_env, VintageConfigWebUtils.lookup(groupString).size());
		}

		int newsize = 0;
		VintageConfigWebUtils.register(groupString, keyString, valueString);
		for (int i = 0; i < 100; i++) {
			newsize = VintageConfigWebUtils.lookup(groupString).size();
			System.out.print(i);
			assertEquals(init_env + 1, newsize);
		}

		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/*
	 * lookup for many groups and keys
	 */
	@Test
	public void testMultiLookup() {

		try {
			for (int i = 0; i < 3; i++) {
				assertEquals(0, VintageConfigWebUtils.lookup(groupString+i).size());
			}

			for (int i = 0; i < 3; i++) {
				VintageConfigWebUtils.batchregister(groupString+i, keyString, valueString, 1, 4);
			}
						
			for (int i = 0; i < 3; i++) {
				assertEquals(4, VintageConfigWebUtils.lookup(groupString+i).size());
			}
			
		} finally {
			for (int i = 0; i < 3; i++) {
				VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 4);
			}
		}

	}	

	/*
	 * check the result when appointing group only need to know the original
	 * result of the lookup interface, so use the function lookup of class
	 * VintageConfigWebUtils, not the class StaticsConfigServiceClient
	 */
	@Test
	public void testLookupResultGroup() {
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		List<String> nodesList = VintageConfigWebUtils.lookup(groupString);
		String groupStr = getGroupFromConfigNode(nodesList.get(0));
		String md5Str = getmd5FromConfigNode(nodesList.get(0));
		String keyStr = getkeyFromConfigNode(nodesList.get(0));
		String valueStr = getValueFromConfigNode(nodesList.get(0));

		assertTrue(groupStr.equals(""));
		assertTrue(md5Str.equals(""));
		assertFalse(keyStr.equals(""));
		assertFalse(valueStr.equals(""));

		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/*
	 * check the result appointing group and key, the same above
	 */
	@Test
	public void testLookupResult() {
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		List<String> nodesList = VintageConfigWebUtils.lookup(groupString,
				keyString);
		String groupStr = getGroupFromConfigNode(nodesList.get(0));
		String md5Str = getmd5FromConfigNode(nodesList.get(0));
		String keyStr = getkeyFromConfigNode(nodesList.get(0));
		String valueStr = getValueFromConfigNode(nodesList.get(0));

		assertTrue(groupStr.equals(""));
		assertTrue(md5Str.equals(""));
		assertFalse(keyStr.equals(""));
		assertFalse(valueStr.equals(""));

		VintageConfigWebUtils.unregister(groupString, keyString);
	}

}
