package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
//import org.springframework.jdbc.config.InitializeDatabaseBeanDefinitionParser;

import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;

public class LookupTest extends BaseTest {
	@Before
	public void setUp() throws Exception {
		groupString = getRandomString(10);
		keyString = getRandomString(5);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/*
	 * repeat lookup for many times
	 */
	@Test
	public void testRepeatLookup() {
		for (int i = 0; i < 100; i++) {
			assertEquals(0, VintageConfigWebUtils.lookup(groupString).size());
		}

		int newsize = 0;
		VintageConfigWebUtils.register(groupString, keyString, valueString);
		for (int i = 0; i < 100; i++) {
			newsize = VintageConfigWebUtils.lookup(groupString).size();
			System.out.print(i);
			assertEquals(1, newsize);
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
				for (int j = 0; j < 4; j++) {
					assertEquals(0, VintageConfigWebUtils.lookup(groupString+i, keyString+j).size());
				}
			}

			for (int i = 0; i < 3; i++) {
				VintageConfigWebUtils.batchregister(groupString+i, keyString, valueString, 1, 4);
			}

			for (int i = 0; i < 3; i++) {
				assertEquals(4, VintageConfigWebUtils.lookup(groupString+i).size());
				for (int j = 0; j < 4; j++) {
					assertEquals(1,
							VintageConfigWebUtils.lookup(groupString+i, keyString+j)
									.size());
				}
			}

		} finally {
			for (int i = 0; i < 3; i++) {
				VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 4);
			}
		}

	}

	@Test
	public void testNumerousKeysLookup() {
		int max = 1000;
		int oldsize = 0;
		int newsize = 0;
		try {
			oldsize = VintageConfigWebUtils.lookup(groupString).size();
			VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 1, max);

			newsize = VintageConfigWebUtils.lookup(groupString).size();
			assertEquals(oldsize + max, newsize);

			List<String> nodesList = VintageConfigWebUtils.lookup(groupString);
			Map<String, String> nodesMap = VintageConfigWebUtils
					.getConfigMap(nodesList);
			for (int i = 0; i < max; i++) {
				String nodeValueString = nodesMap.get(keyString + i);
				assertEquals(valueString + i, nodeValueString);
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.print(e.getMessage());
			fail("error in testGroupNumerousKeys");
		} finally {
			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, max);
			assertEquals(oldsize, VintageConfigWebUtils.lookup(groupString)
					.size());
		}
	}

	/**
	 * lookup group and key which didn't register，不抛异常
	 */
	@Test
	public void testLookupNoExists() {
		String groupTemp = "hqgroup";
		String keyTemp = "hqkey";
		VintageConfigWebUtils.register(groupString, keyString, valueString);

		try {
			List<String> nodeList = VintageConfigWebUtils.lookup(groupString);
			assertEquals(1, nodeList.size());

			nodeList = VintageConfigWebUtils.lookup(groupTemp);
			assertEquals(0, nodeList.size());

			nodeList = VintageConfigWebUtils.lookup(groupTemp, keyTemp);
			assertEquals(0, nodeList.size());

			//fail("error in testLookupNoExists");
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/*
	 * parameters blank
	 */
	@Test
	public void testParamBlank() {
		try {
			VintageConfigWebUtils.lookup(null);
		} catch (VintageException e) {
			fail("error in testParamBlank");
		}
		
		try {
			VintageConfigWebUtils.lookup("");
			fail("error in testParamBlank");
		} catch (VintageException e) {
			
		}
		
		try {
			VintageConfigWebUtils.lookup(null, keyString);
		} catch (VintageException e) {
			fail("error in testParamBlank");
		}
		
		try {
			VintageConfigWebUtils.lookup("", keyString);
			fail("error in testParamBlank");
		} catch (VintageException e) {
		}
	}
}
