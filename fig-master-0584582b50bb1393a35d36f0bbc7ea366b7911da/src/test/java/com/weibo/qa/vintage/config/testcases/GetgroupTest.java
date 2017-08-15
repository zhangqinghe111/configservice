package com.weibo.qa.vintage.config.testcases;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.weibo.vintage.utils.VintageConfigWebUtils;


/*
 * test getgroup
 */
public class GetgroupTest extends BaseTest {

	/**
	 * group注册后就取消不掉，因此取消注册后，getgroup仍为1
	 **/
	@Test
	public void testGetgroup() {
		String group = getRandomString(8);
		try {
			// no group
			List<String> groupList = VintageConfigWebUtils.getgroup();
			int groupnum = groupList.size();
//			assertTrue(groupList.isEmpty());
			VintageConfigWebUtils.register(group, keyString, valueString);
			List<String> groupsList=VintageConfigWebUtils.getgroup();
			assertEquals(groupnum+1, groupsList.size());			
//			for (String string : groupsList) {
//				assertEquals(groupString, string);
//			}			

			VintageConfigWebUtils.unregister(group, keyString);
			assertEquals(groupnum+1, VintageConfigWebUtils.getgroup().size());
		} finally {
			VintageConfigWebUtils.unregister(group, keyString);
		}

	}

}
