package com.weibo.qa.vintage.testcases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.weibo.qa.vintage.client.testcases.ClientConfigConfigureTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigLookupTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigRegisterTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigSnapshotReadTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigSnapshotWriteTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigSubscribeTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigUnregisterTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigUnsubscribeAllTest;
import com.weibo.qa.vintage.client.testcases.ClientConfigUnsubscribeTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingRegisterTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingReportTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingSnapshotWriteTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingSubscribeTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingUnsubscribeAllTest;
import com.weibo.qa.vintage.client.testcases.ClientNamingUnsubscribeTest;

@RunWith(Suite.class)
@SuiteClasses({ ClientNamingReportTest.class,
		ClientNamingSnapshotWriteTest.class, ClientNamingSnapshotWriteTest.class,
		ClientNamingRegisterTest.class, ClientNamingSubscribeTest.class, 
		ClientNamingUnsubscribeTest.class, ClientNamingUnsubscribeAllTest.class})
public class ClientNamingTests {

}
