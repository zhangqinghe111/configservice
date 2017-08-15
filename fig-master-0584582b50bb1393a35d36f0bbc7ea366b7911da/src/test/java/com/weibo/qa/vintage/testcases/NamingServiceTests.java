package com.weibo.qa.vintage.testcases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.weibo.qa.vintage.client.testcases.ClientNamingSubscribeTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingUnsubscribeAllTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingUnsubscribeTest;
import com.weibo.qa.vintage.naming.testcases.AdminTest;
import com.weibo.qa.vintage.naming.testcases.ClusterIDsDelServiceTest;
import com.weibo.qa.vintage.naming.testcases.GetSignTest;
import com.weibo.qa.vintage.naming.testcases.HeartBeatProtectionTest;
import com.weibo.qa.vintage.naming.testcases.IPGroupSearchClusterTest;
import com.weibo.qa.vintage.naming.testcases.IPGroupTest;
import com.weibo.qa.vintage.naming.testcases.LookupforupdateTest;
import com.weibo.qa.vintage.naming.testcases.RegisterTest;
import com.weibo.qa.vintage.naming.testcases.ServerNodeCacheTest;
import com.weibo.qa.vintage.naming.testcases.WhitelistTest;
import com.weibo.qa.vintage.naming.testcases.thresholdOperationTest;

@RunWith(Suite.class)
// AdminTest.class, 
@SuiteClasses({ GetSignTest.class,
		LookupforupdateTest.class, RegisterTest.class,WhitelistTest.class,
		ClusterIDsDelServiceTest.class, IPGroupSearchClusterTest.class,
		IPGroupTest.class, thresholdOperationTest.class, HeartBeatProtectionTest.class
		})
public class NamingServiceTests {

}