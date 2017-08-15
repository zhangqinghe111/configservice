package com.weibo.qa.vintage.online.config.testcases;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

/*
 * test getgroup
 */
public class ConfigGetKeysTest extends BaseTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		groupString = "vintage.test.qa.liuyu9";
		keyString = getRandomString(10);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetKeys() {
		int keysize = 10;
		try {
			List<String> keyList = VintageConfigWebUtils.getkeys(groupString);
			int init_env = keyList.size();

			VintageConfigWebUtils.register(groupString, keyString, valueString);
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(init_env+1, keyList.size());

			VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 1, keysize);
			
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(init_env + keysize + 1, keyList.size());

			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, keysize);
			
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(init_env+1, keyList.size());

			VintageConfigWebUtils.unregister(groupString, keyString);
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(keyList.size(), init_env);

			VintageConfigWebUtils.register(groupString, keyString, valueString);
			VintageConfigWebUtils.register(groupString, keyString + 1,
					valueString);
			VintageConfigWebUtils.unregister(groupString, keyString);
			VintageConfigWebUtils.register(groupString, keyString + 2,
					valueString);

			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(init_env+2, keyList.size());
			System.out.print(keyList);
			assertTrue(keyList.contains(keyString + 1));
			assertTrue(keyList.contains(keyString + 2));
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString + 1);
			VintageConfigWebUtils.unregister(groupString, keyString + 2);
		}

	}

	@Test
	public void testGetKeysMultiGroup() {
		int keysize = 10;
		String groupSecond = groupString + "1";
		try {
			int init_env1 = VintageConfigWebUtils.getkeys(groupString).size();
			int init_env2 = VintageConfigWebUtils.getkeys(groupSecond).size();
			VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 0, keysize);
			VintageConfigWebUtils.batchregister(groupSecond, keyString, valueString, 0, keysize);

			List<String> keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(init_env1 + keysize, keyList.size());

			keyList = VintageConfigWebUtils.getkeys(groupSecond);
			assertEquals(init_env2 + keysize, keyList.size());
		} finally {
			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, keysize);
			VintageConfigWebUtils.batchunregister(groupSecond, keyString, 0, keysize);

		}
	}
}
