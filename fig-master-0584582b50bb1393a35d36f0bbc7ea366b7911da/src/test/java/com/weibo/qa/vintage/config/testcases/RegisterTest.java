package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConfigWebUtils;

/*
 * test cases for register
 */
public class RegisterTest extends BaseTest {
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

	/**
	 * repeat test,contains three points: register with same group ,key and
	 * values for many times; register with same group, key and different values
	 * for many times; unregister many times.
	 */
	@Test
	public void testRepeatRegister() {
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
		String nodevalue = getValueFromConfigNode(nodesList.get(0));
		assertEquals(nodevalue, valueString);

		// modify the value
		String valueString2 = "anothervalue";
		VintageConfigWebUtils.register(groupString, keyString, valueString2);
		
		nodesList = VintageConfigWebUtils.lookup(groupString);
		assertEquals(newsize, nodesList.size());
		nodevalue = getValueFromConfigNode(nodesList.get(0));
		assertEquals(nodevalue, valueString2);

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

	/**
	 * register group naming a and a/b/c, check the group a/b
	 */
	@Test
	public void testGroupName() {
		String groupStr1 = "a";
		String groupStr2 = "a/b/c";

		int oldsize1 = VintageConfigWebUtils.lookup(groupStr1).size();
		int oldsize2 = VintageConfigWebUtils.lookup(groupStr2).size();

		VintageConfigWebUtils.register(groupStr1, keyString, valueString);
		VintageConfigWebUtils.register(groupStr2, keyString, valueString);

		int newsize1 = VintageConfigWebUtils.lookup(groupStr1).size();
		int newsize2 = VintageConfigWebUtils.lookup(groupStr2).size();

		assertEquals(oldsize1 + 1, newsize1);
		assertEquals(oldsize2 + 1, newsize2);

		List<String> nodesList = VintageConfigWebUtils.lookup("a/b");

		VintageConfigWebUtils.unregister(groupStr1, keyString);
		VintageConfigWebUtils.unregister(groupStr2, keyString);

		// check the key number of group "a/b"
		System.out.print(nodesList.size());
		assertEquals(0, nodesList.size());
	}	

	/*
	 * register large amount of keys within one group
	 */
	@Test
	public void testGroupNumerousKeys() {
		int max = 1000;
		int oldsize = 0;
		int newsize = 0;
		try {
			oldsize = VintageConfigWebUtils.lookup(groupString).size();
			VintageConfigWebUtils.batchregister(groupString, keyString, valueString, 1, max);
			
			newsize = VintageConfigWebUtils.lookup(groupString).size();
			assertEquals(oldsize + max, newsize);

			for (int i = 0; i < max; i++) {
				List<String> nodesList = VintageConfigWebUtils.lookup(groupString, keyString+i);
				String nodevalue = getValueFromConfigNode(nodesList.get(0));
				assertEquals(nodevalue, valueString+i);
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.print(e.getMessage());
			fail("error in testGroupNumerousKeys");
		} finally {
			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, max);
			assertEquals(oldsize, VintageConfigWebUtils.lookup(groupString).size());
		}
	}

	/*
	 * register large amount of keys and long value within one group
	 */
	@Test
	public void testGroupNumerousKeysValues() {
		int max = 1000;
		int oldsize = 0;
		int newsize = 0;
		String valueTemp = "";
		StringBuilder sBuilder = new StringBuilder();

		for (int i = 0; i < 2000; i++) {
			sBuilder = sBuilder.append('a');
		}

		valueTemp = sBuilder.toString();

		try {
			oldsize = VintageConfigWebUtils.lookup(groupString).size();
			VintageConfigWebUtils.batchregister(groupString, keyString, valueTemp, 1, max);

			newsize = VintageConfigWebUtils.lookup(groupString).size();
			System.out.print(oldsize + max);
			System.out.print("newsize:" + newsize);
			assertEquals(oldsize + max, newsize);

			for (int i = 0; i < max; i++) {
				List<String> nodesList = VintageConfigWebUtils.lookup(groupString, keyString+i);
				String nodevalue = getValueFromConfigNode(nodesList.get(0));
				assertEquals(nodevalue, valueTemp+i);
			}
		} catch (Exception e) {
			// TODO: handle exception
			System.out.print(e.getMessage());
			fail("error in testGroupNumerousKeys");
		} finally {
			VintageConfigWebUtils.batchunregister(groupString, keyString, 0, max);
			assertEquals(oldsize, VintageConfigWebUtils.lookup(groupString).size());
		}
	}

	/**
	 * parameters null return E_SERVICE_PACKET_EMPTY, but expect
	 * E_PARAM_INVALID_ERROR
	 */
	@Ignore
	@Test
	public void testParamNull() {
		registerParamException(null, keyString, valueString);
		registerParamException(groupString, null, valueString);
		registerParamException(groupString, keyString, null);
	}

	@Test
	public void testParamBlank() {
		registerParamException("", keyString, valueString);
		registerParamException(groupString, "", valueString);
		registerParamException(groupString, keyString, "");
	}
	
	@Test
	public void testParamSpectial()
	{
		String[] groupStrings = new String[]{"[[","]]"};
		for (String group : groupStrings) {
			VintageConfigWebUtils.register(group, group, group);
			System.out.println(VintageConfigWebUtils.lookup(group, group));
			VintageConfigWebUtils.unregister(group, group);
			System.out.println(VintageConfigWebUtils.lookup(group, group));
		}
	}

	/*
	 * set group、key、value chinese
	 */
	@Ignore
	@Test
	public void testParamChinese() {
		String groupStr = "测试组名huqian";
		String keyStr = "测试键2";
		String valueStr = "测试值2";
		try {
		
			VintageConfigWebUtils.register(groupStr, keyString, valueString);
			List<String> nodeMap = VintageConfigWebUtils.lookup(groupStr);
			assertEquals(1, nodeMap.size());

			String sign1 = VintageConfigWebUtils.getsign(groupString);

			VintageConfigWebUtils.register(groupString, keyStr, valueString);
			nodeMap = VintageConfigWebUtils.lookup(groupString);
			assertEquals(1, nodeMap.size());

			String sign2 = VintageConfigWebUtils.getsign(groupString);

			VintageConfigWebUtils.register(groupString, keyString, valueStr);
			nodeMap = VintageConfigWebUtils.lookup(groupString);
			assertEquals(2, nodeMap.size());

			String sign3 = VintageConfigWebUtils.getsign(groupString);
			System.out.print(sign1 + " " + sign2 + " " + sign3);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			VintageConfigWebUtils.unregister(groupStr, keyString);
			VintageConfigWebUtils.unregister(groupString, keyStr);
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}

	/*
	 * test parameters with null
	 */
	private void registerParamException(String groupStr, String keyStr,
			String valueStr) {
		try {
			VintageConfigWebUtils.register(groupStr, keyStr, valueStr);
			fail("error in registerParamNull");
		} catch (Exception e) {
		}
	}

}
