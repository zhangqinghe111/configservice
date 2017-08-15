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

@RunWith(Suite.class)
@SuiteClasses({ ClientConfigConfigureTest.class, ClientConfigRegisterTest.class,
		ClientConfigSnapshotReadTest.class, ClientConfigSnapshotWriteTest.class, 
		ClientConfigUnregisterTest.class, ClientConfigSubscribeTest.class, 
		ClientConfigUnsubscribeAllTest.class, ClientConfigUnsubscribeTest.class, 
		ClientConfigLookupTest.class})
public class ClientConfigTests {

}
