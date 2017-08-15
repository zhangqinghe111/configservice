package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import java.util.*;

import cn.sina.api.commons.redis.jedis.JedisPort;
import com.weibo.vintage.model.ConfigInfo;
import com.weibo.vintage.utils.JsonHelper;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageNamingWebUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.vintage.exception.ExcepFactor;
import com.weibo.vintage.exception.VintageException;
import com.weibo.vintage.utils.VintageConfigWebUtils;


/**
 * 由于redis主从同步时间较久，每次group更新后需要sleep一会儿，主从的sign值才会同步更新；
 * 校验sign值的地方，在代码里增加了sleep 10 ms
 */
public class GetSignTest extends BaseTest {

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
	public void testRepeatGetsign() {
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 3; j++){
				VintageConfigWebUtils.getsign(groupString+j);
			}
		}

		for (int j = 0; j < 3; j++) {
			VintageConfigWebUtils.batchregister(groupString+j, keyString, valueString, 1, 4);
		}

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < 3; j++){
				VintageConfigWebUtils.getsign(groupString+j);
			}
		}

		for (int j = 0; j < 3; j++) {
			VintageConfigWebUtils.batchunregister(groupString+j, keyString, 0, 4);
		}
	}

	/*
	 * check the sign of one group when configure registered ,modified and
	 * unregistered
	 */
	@Test
	public void testOneGroup() {
		try {
			// register new configure information
			String oldsign = VintageConfigWebUtils.getsign(groupString);

			String theInitialSign = oldsign;
			String newsign = "";

			for (int i = 0; i < 4; i++) {
				VintageConfigWebUtils.register(groupString, keyString+i, valueString+i);
				newsign = VintageConfigWebUtils.getsign(groupString);
				System.out.println("oldsign = " + oldsign + " newsign = " + newsign);
				assertFalse(oldsign.equals(newsign));
                //校验下sign的md5值
				oldsign = newsign;
			}

			// repeat register the configure registered for 3 times
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 4; j++) {
					VintageConfigWebUtils.register(groupString, keyString+j, valueString+j);
					newsign = VintageConfigWebUtils.getsign(groupString);
					assertEquals(oldsign, newsign);
				}
			}

			// repeat modify the configure registered with new value
			for (int j = 0; j < 4; j++) {
				VintageConfigWebUtils.register(groupString, keyString+j,
						valueString+"_new"+j);
				newsign = VintageConfigWebUtils.getsign(groupString);
				assertFalse(oldsign.equals(newsign));
				oldsign = newsign;
			}
		

			// unregister the configure information registered
			for (int j = 0; j < 4; j++) {
				VintageConfigWebUtils.unregister(groupString, keyString+j);
				newsign = VintageConfigWebUtils.getsign(groupString);
				assertFalse(oldsign.equals(newsign));
				oldsign = newsign;
			}

			// repeat unregister and register,check the sign
			for (int j = 0; j < 4; j++) {
				String sign1 = VintageConfigWebUtils.getsign(groupString);

				VintageConfigWebUtils.register(groupString, keyString+j, valueString);
				String sign2 = VintageConfigWebUtils.getsign(groupString);

				VintageConfigWebUtils.unregister(groupString, keyString+j);
                String sign3 = VintageConfigWebUtils.getsign(groupString);

				VintageConfigWebUtils.register(groupString, keyString+j,
						valueString);
                String sign4 = VintageConfigWebUtils.getsign(groupString);

				assertEquals(sign1, sign3);
				assertEquals(sign2, sign4);
				assertFalse(sign1.equals(sign2));
			}

			// unregister all
			for (int j = 0; j < 4; j++) {
				VintageConfigWebUtils.unregister(groupString, keyString+j);
			}
            assertEquals(theInitialSign,
					VintageConfigWebUtils.getsign(groupString));
		} finally {
			for (int j = 0; j < 4; j++) {
				VintageConfigWebUtils.unregister(groupString, keyString+j);
			}
		}
	}

	/*
	 * check the signs of two different groups
	 */
	@Test
	public void testTwoGroup() {
		String groupTemp1 = groupString+"1";
		String keyTemp1 = "keyTemp1";
		String valueTemp1 = "valueTemp1";
		String groupTemp2 = groupString+"2";
		String keyTemp2 = "keyTemp2";
		String valueTemp2 = "valueTemp2";

		String sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		String sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertEquals(sign1, sign2);

		// register the same key and value
		VintageConfigWebUtils.register(groupTemp1, keyTemp1, valueTemp1);
		VintageConfigWebUtils.register(groupTemp2, keyTemp1, valueTemp1);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		System.out.print(sign1 + " " + sign2);
		assertEquals(sign1, sign2);
		assertTrue(sign1.equals(sign2));

		VintageConfigWebUtils.unregister(groupTemp1, keyTemp1);
		VintageConfigWebUtils.unregister(groupTemp2, keyTemp1);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertEquals(sign1, sign2);

		// register the same key and different value
		VintageConfigWebUtils.register(groupTemp1, keyTemp1, valueTemp1);
		VintageConfigWebUtils.register(groupTemp2, keyTemp1, valueTemp2);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertFalse(sign1.equals(sign2));

		VintageConfigWebUtils.unregister(groupTemp1, keyTemp1);
		VintageConfigWebUtils.unregister(groupTemp2, keyTemp1);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertEquals(sign1, sign2);

		// register the different key and same value
		VintageConfigWebUtils.register(groupTemp1, keyTemp1, valueTemp1);
		VintageConfigWebUtils.register(groupTemp2, keyTemp2, valueTemp1);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertFalse(sign1.equals(sign2));

		VintageConfigWebUtils.unregister(groupTemp1, keyTemp1);
		VintageConfigWebUtils.unregister(groupTemp2, keyTemp2);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertEquals(sign1, sign2);

		// register the different key and same value
		VintageConfigWebUtils.register(groupTemp1, keyTemp1, valueTemp1);
		VintageConfigWebUtils.register(groupTemp2, keyTemp2, valueTemp2);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertFalse(sign1.equals(sign2));

		VintageConfigWebUtils.unregister(groupTemp1, keyTemp1);
		VintageConfigWebUtils.unregister(groupTemp2, keyTemp2);
		sign1 = VintageConfigWebUtils.getsign(groupTemp1);
		sign2 = VintageConfigWebUtils.getsign(groupTemp2);
		assertEquals(sign1, sign2);
	}

	/*
	 * check whether sign of the parent group would change modifying the child
	 * configure information
	 */
	@Test
	public void testIteration() {
		String groupTemp1 = groupString+"Parent";
		String groupTemp2 = groupString+"Parent/groupChild";
		String keyTemp = "keyTemp";
		String valueTemp = "valueTemp";

		String oldSignParent = VintageConfigWebUtils.getsign(groupTemp1);
		String oldSignChild = VintageConfigWebUtils.getsign(groupTemp2);
		System.out.print("/n" + oldSignParent + " " + oldSignChild);

		VintageConfigWebUtils.register(groupTemp2, keyTemp, valueTemp);

		String newSignParent = VintageConfigWebUtils.getsign(groupTemp1);
		String newSignChild = VintageConfigWebUtils.getsign(groupTemp2);
		System.out.print(newSignParent + " " + newSignChild);

		VintageConfigWebUtils.unregister(groupTemp2, keyTemp);

		assertTrue(oldSignParent.equals(newSignParent));
		assertFalse(oldSignChild.equals(newSignChild));
	}

	/*
	 * parameters null null as group name, not an exception
	 */
	@Test
	public void testParamBlank() {
		try {
			VintageConfigWebUtils.getsign(null);
		} catch (Exception e) {
			fail("Error in getsignParamNull");
		}
		
		try {
			VintageConfigWebUtils.getsign("");
			fail("Error in getsignParamNull");
		} catch (Exception e) {
			System.out.print(e.getMessage());		
		}
	}


	/*
	 * test parameters with null or other
	 */
	private void getsignParamNull(String groupStr) {
		
	}
}
