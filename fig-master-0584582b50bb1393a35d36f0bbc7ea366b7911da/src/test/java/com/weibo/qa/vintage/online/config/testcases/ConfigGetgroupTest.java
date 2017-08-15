package com.weibo.qa.vintage.online.config.testcases;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.weibo.qa.vintage.config.testcases.BaseTest;
import com.weibo.vintage.utils.VintageConfigWebUtils;


/*
 * test getgroup
 */
public class ConfigGetgroupTest extends BaseTest {
	private static String group;
	
	@BeforeClass
	public static void setUp() throws Exception {
		groupString = "vintage.test.qa.liuyu9";
		keyString = getRandomString(10);
		valueString = getRandomString(20);
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * group注册后就取消不掉，因此取消注册后，getgroup仍为1
	 **/
	@Test
	public void testGetgroup() {
		try {
			List<String> groupList = VintageConfigWebUtils.getgroup();
			if(!groupList.contains(groupString)){
				VintageConfigWebUtils.register(groupString, keyString, valueString);
			}
			List<String> groupsList=VintageConfigWebUtils.getgroup();
			assertTrue(groupsList.contains(groupString));
			VintageConfigWebUtils.unregister(groupString, keyString);
		} finally {
			VintageConfigWebUtils.unregister(groupString, keyString);
		}
	}
}
