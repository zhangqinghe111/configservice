package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

/*
 * test getgroup
 */
public class GetKeysTest extends BaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
		keyString = getRandomString(5);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetKeys() {
		int keysize = 10;
		try {
			// no keys
			List<String> keyList = VintageConfigWebUtils.getkeys(groupString);
			assertTrue(keyList.isEmpty());

			VintageConfigWebUtils.register(groupString, keyString, valueString);
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(1, keyList.size());

			VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 0, keysize);
			
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(keysize + 1, keyList.size());

			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, keysize);
			
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(1, keyList.size());

			VintageConfigWebUtils.unregister(groupString, keyString);
			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertTrue(keyList.isEmpty());

			VintageConfigWebUtils.register(groupString, keyString, valueString);
			VintageConfigWebUtils.register(groupString, keyString + 1,
					valueString);
			VintageConfigWebUtils.unregister(groupString, keyString);
			VintageConfigWebUtils.register(groupString, keyString + 2,
					valueString);

			keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(2, keyList.size());
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
		String groupSecond = "getKeysGroupSec";
		try {
			int size_1 = VintageConfigWebUtils.getkeys(groupString).size();
			int size_2 = VintageConfigWebUtils.getkeys(groupSecond).size();

			VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 0, keysize);
			VintageConfigWebUtils.batchregister(groupSecond, keyString, valueString, 0, keysize);

			List<String> keyList = VintageConfigWebUtils.getkeys(groupString);
			assertEquals(keysize + size_1, keyList.size());

			keyList = VintageConfigWebUtils.getkeys(groupSecond);
			assertEquals(keysize + size_2, keyList.size());
		} finally {
			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, keysize);
			VintageConfigWebUtils.batchunregister(groupSecond, keyString, 0, keysize);
		}
	}

	@Test
	public void testGetKesGroupExcep() {
		try {
			VintageConfigWebUtils.register(groupString, keyString, valueString);

			try {
				VintageConfigWebUtils.getkeys(null);
			} catch (VintageException ex) {
				fail();
			}

			try {
				VintageConfigWebUtils.getkeys("");
				fail();
			} catch (VintageException ex) {
			}
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}
}
