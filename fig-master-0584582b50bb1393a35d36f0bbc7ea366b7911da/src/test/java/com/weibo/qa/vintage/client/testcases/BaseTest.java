package com.weibo.qa.vintage.client.testcases;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import com.weibo.vintage.model.VintageConfig;
import com.weibo.vintage.utils.VintageNamingClientUtils;
import com.weibo.vintage.utils.VintageConstantsTest;
import com.weibo.vintage.utils.VintageNamingWebUtils;
import com.weibo.vintage.utils.VintageTestLogger;

/**
 * 此类为测试用例的基类
 * 
 * @author lingling6
 * 
 */
public class BaseTest {

	protected static VintageConfig config = new VintageConfig();

	protected String serviceId;
	protected String clusterId;
	protected String serviceId2;
	protected String clusterId2;
	protected String localIP;
	protected int HEARTBEATINTERVAL = VintageConstantsTest.HEARTBEATINTERVAL;
	protected int serviceCacheInterval = VintageConstantsTest.serviceCacheInterval;
    protected int snapInterval = VintageConstantsTest.snapInterval;

	protected Set<String> localNodes = new HashSet<String>();
	protected static String snapshotDir;

	@BeforeClass
	public static void start() {
	}

	@AfterClass
	public static void stop() {

	}

	@Before
	public void setUp() throws Exception {
		init();
	}

	@After
	public void tearDown() throws Exception {
	}

	/**
	 * 将本机ip加入白名单，创建配置服务
	 * @throws UnknownHostException 
	 */
	private void init() throws UnknownHostException {

		config = getConfig();
		localIP = InetAddress.getLocalHost().getHostAddress();
		localNodes.add(localIP);

		// 每次测试开始时，清空已存数据
		VintageNamingClientUtils.currentNodes.clear();
		VintageNamingClientUtils.currentUnreachableNodes.clear();
		VintageNamingClientUtils.currentWorkingNodes.clear();
		VintageNamingClientUtils.nodeMap.clear();
		VintageNamingClientUtils.nodeUnreachableMap.clear();
		VintageNamingClientUtils.currentWorkingNodes.clear();
		VintageTestLogger.info("add the localip "+localIP + " to the localNodes, and clear all data.");
	}

	protected VintageConfig getConfig() {
		VintageConfig config = new VintageConfig();
		config.setHeartbeatInterval(HEARTBEATINTERVAL);
		config.setPullServerAddress(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL);
		config.setServiceId(serviceId);
        config.setNamingDumpInterval(snapInterval);
        snapshotDir = System.getProperty("user.dir");
        config.setSnapshotDir(snapshotDir + "/configserver/naming");
		return config;
	}
	
	protected void addService(String serviceId, String type){
		if (!VintageNamingWebUtils.existsService(serviceId)) {
			VintageNamingWebUtils.addService(serviceId, type);
		}
	}
	
	protected void addCluster(String serviceId, String clusterId){
		if (VintageNamingWebUtils.existsService(serviceId) && !VintageNamingWebUtils.existCluster(serviceId, clusterId)){
			VintageNamingWebUtils.addCluster(serviceId, clusterId);
		}
	}
	
	protected void addWhiteList(String service, Set<String> localNodes2){
		if (VintageNamingWebUtils.existsService(service)){
			VintageNamingWebUtils.addWhitelist(service, localNodes2);
		}
	}
	
	protected void addWhiteList(String service, String node){
		if (VintageNamingWebUtils.existsService(service)){
			VintageNamingWebUtils.addWhitelist(service, node);
		}
	}
	
	protected void delWhiteList(String service, Set<String> localNodes2){
		if (VintageNamingWebUtils.existsService(service)){
			VintageNamingWebUtils.deleteWhitelist(service, localNodes2);
		}
	}
	
	protected void delWhiteList(String service, String node){
		if (VintageNamingWebUtils.existsService(service)){
			VintageNamingWebUtils.addWhitelist(service, node);
		}
	}
	
	protected void delCluster(String serviceId, String clusterId){
		if (VintageNamingWebUtils.existsService(serviceId) && VintageNamingWebUtils.existCluster(serviceId, clusterId)){
			VintageNamingWebUtils.deleteCluster(serviceId, clusterId);
		}
	}
	
	protected void delService(String serviceId){
		if (VintageNamingWebUtils.existsService(serviceId)) {
			VintageNamingWebUtils.deleteService(serviceId);
		}
	}
	
	public static String getRandomString(int length){  
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";  
        Random random = new Random();  
        StringBuffer sb = new StringBuffer();  
          
        for(int i = 0 ; i < length; ++i){  
            int number = random.nextInt(62);//[0,62)  
              
            sb.append(str.charAt(number));  
        }  
        return sb.toString();  
    }  
}
