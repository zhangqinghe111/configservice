package com.weibo.qa.vintage.online.config.testcases;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

/*
 * test cases for register
 */
public class ConfigRegisterTest extends BaseTest {
	
	
	@BeforeClass
	public static void setUp() throws Exception {
		groupString = "vintage.test.qa.liuyu9";
		keyString = getRandomString(10);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
		VintageConfigWebUtils.unregister(groupString, keyString);
	}

	/**
	 * repeat test,contains three points: register with same group ,key and
	 * values for many times; register with same group, key and different values
	 * for many times; unregister many times.
	 */
	@Test
	public void testRepeatRegister() {
		int num = 100;
		// the first step
		// with the same group ,key and values
		int oldsize = VintageConfigWebUtils.lookup(groupString).size();
		VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 2, 100);
				
		List<String> nodesList = VintageConfigWebUtils.lookup(groupString);
		System.out.print(nodesList.toString());
		int newsize = nodesList.size();
		assertEquals(oldsize + 1, newsize);

		// the second step
		// with the same group ,key and different values
		nodesList = VintageConfigWebUtils.lookup(groupString);
		Map<String, String> nodesMap = VintageConfigWebUtils.getConfigMap(nodesList);
		assertEquals(valueString, nodesMap.get(keyString));

		// modify the value
		String valueString2 = "anothervalue";
		VintageConfigWebUtils.register(groupString, keyString, valueString2);
		assertEquals(newsize, VintageConfigWebUtils.lookup(groupString).size());

		nodesList = VintageConfigWebUtils.lookup(groupString);
		nodesMap = VintageConfigWebUtils.getConfigMap(nodesList);
		assertEquals(valueString2, nodesMap.get(keyString));

		// test unregister for many times
		VintageConfigWebUtils.batchunregister(groupString, keyString, 1, 10);
		assertEquals(oldsize, VintageConfigWebUtils.lookup(groupString).size());
	}

	/**
	 * repeat with different group,contains two points: register with different
	 * group, same multi keys for each group unregister with different group
	 */
	@Test
	public void testMultiRegister() {

		// get old key number and register
		int[] expSizeArray = new int[3];
		for (int i = 0; i < 3; i++) {
			expSizeArray[i] = VintageConfigWebUtils.lookup(groupString+i).size();
			expSizeArray[i] = expSizeArray[i] + 4;

			VintageConfigWebUtils.batchregister(groupString+i, keyString, valueString, 1, 4);
		}

		// get new key number
		int[] newsizeArray = new int[3];
		for (int i = 0; i < 3; i++) {
			newsizeArray[i] = VintageConfigWebUtils.lookup(groupString+i).size();
		}

		// check the key number
		assertArrayEquals(expSizeArray, newsizeArray);

		// check the value
		for (int i = 0; i < 3; i++) {
			List<String> nodesList = VintageConfigWebUtils.lookup(groupString+i);
			Map<String, String> nodeMap = VintageConfigWebUtils.getConfigMap(nodesList);
			for (int j = 0; j < 4; j++){
				assertEquals(nodeMap.get(keyString+j), valueString+j);
			}
		}

		// unregister the keys
		for (int i = 0; i < 3; i++) {
			VintageConfigWebUtils.batchunregister(groupString+i, keyString, 0, 4);
		}

		for (int i = 0; i < 3; i++) {
			expSizeArray[i] = expSizeArray[i] - 4;
			newsizeArray[i] = VintageConfigWebUtils.lookup(groupString+i).size();
		}
		// check the key number
		assertArrayEquals(expSizeArray, newsizeArray);
	}
}
