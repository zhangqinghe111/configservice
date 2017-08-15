package com.weibo.qa.vintage.config.testcases;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

import com.weibo.vintage.utils.SwitcherUtils;

import org.codehaus.jackson.JsonNode;
import org.junit.BeforeClass;
import com.weibo.vintage.client.StaticsConfigServiceClient;
import com.weibo.vintage.listener.ConfigServiceChangeListener;
import com.weibo.vintage.listener.ConfigServiceKeyChangeListener;
import com.weibo.vintage.model.NodeAction;
import com.weibo.vintage.model.NotifyAction;
import com.weibo.vintage.model.VintageConfig;
import com.weibo.vintage.utils.JsonHelper;
import com.weibo.vintage.utils.VintageConfigWebUtils;
import com.weibo.vintage.utils.VintageConstantsTest;

public class BaseTest {
	// protected
	protected static String groupString;
	protected static String keyString;
	protected static String valueString;
	protected static StaticsConfigServiceClient configClient = null; // Config
	protected static ConfigServiceChangeListener configListener = null; // Client
	protected static ConfigServiceKeyChangeListener configKeyListener = null;// subscribe
																				// config
																				// key
																			// listener
	protected static int changeDataSize = 0;
	protected static int HEARTBEATINTERVAL = VintageConstantsTest.HEARTBEATINTERVAL;
    protected static int SNAPSHOTINTERVAL=VintageConstantsTest.snapInterval;

	protected static String snapshotDir;
    /**
     * 配置服务默认cache dump snapshot周期
     *
    *public static final long STATICS_CONFIG_DEFAULT_DUMP_INTERVAL = 3600;
	*/
    protected static String keyNodeAction;
	protected static HashMap<String, Integer> groupSizeMap = new HashMap<String, Integer>();
	protected static HashMap<String, Map<String, String>> groupDataMap = new HashMap<String, Map<String, String>>();

	protected static int changeKeyDataSize = 0;
	protected static HashMap<String, Map<String, Object>> groupKeyDataMap = new HashMap<String, Map<String, Object>>();

	// private
	private static VintageConfig config;

	// get value from the jsonString
	protected String getValueFromConfigNode(String nodeString) {
		return GetValueFromString(nodeString, "value");
	}

	protected String getGroupFromConfigNode(String nodeString) {
		return GetValueFromString(nodeString, "groupId");
	}

	protected String getkeyFromConfigNode(String nodeString) {
		return GetValueFromString(nodeString, "key");
	}

	protected String getmd5FromConfigNode(String nodeString) {
		return GetValueFromString(nodeString, "md5");
	}

	private String GetValueFromString(String nodeString, String fieldString) {
		JsonNode fieldNode = JsonHelper.parserStringToJsonNode(nodeString)
				.getFieldValue(fieldString);
		return fieldNode == null ? "" : fieldNode.getTextValue();
	}

	@BeforeClass
	public static void Initialize() {
		System.out.print("beforeClass");
		InitializeClient(); // initialize the client
		InitializeListenner();
		InitializeConfigKeyListenner();
		
		groupString = "qatestgroup";
		keyString = "qatestkey";
		valueString = "qatest";
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_PROCESSOR_SWITCHER, false);
        SwitcherUtils.setSwitcher(SwitcherUtils.SNAPSHOT_DISASTER_TOLERANT_SWITCHER,false);
	}

	/*
	 * client intialization
	 */
	private static void InitializeClient() {
		config = getVintageConfig();
		configClient = new StaticsConfigServiceClient(config);

		configClient.start();
	}

	public static VintageConfig getVintageConfig() {
		VintageConfig vintageConfig = new VintageConfig();
		vintageConfig.setHeartbeatInterval(HEARTBEATINTERVAL);
		vintageConfig.setStaticsConfigDumpInterval(SNAPSHOTINTERVAL);
        vintageConfig.setConnectionTimeout(3000);
		vintageConfig.setSocketTimeout(3000);
		vintageConfig
				.setPullServerAddress(VintageConfigWebUtils.VINTAGE_CONFIG_SERVICE_URL);
		snapshotDir = System.getProperty("user.dir");
        vintageConfig.setSnapshotDir(snapshotDir + "/configserver/config");

		return vintageConfig;
	}

	/*
	 * listenner redefinition and initialization
	 */
	private static void InitializeListenner() {
		configListener = new ConfigServiceChangeListener() {
			@Override
			public void handleDataChange(String dataId, Object data,
					NotifyAction notifyAction) throws Exception {
				// TODO Auto-generated method stub
				System.out.print("groupId:" + dataId);
				System.out.print("notify action" + notifyAction.toString()
						+ "\n");

				Map<String, String> dataMap = (Map<String, String>) data;
				Iterator iterator = dataMap.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next().toString();
					System.out.println("key:  " + key + "\n");
					System.out.println("value:  " + dataMap.get(key) + "\n");
				}

				changeDataSize = dataMap.size();
				groupSizeMap.put(dataId, changeDataSize); // data size for
															// different groups
				groupDataMap.put(dataId, dataMap);
			}

			@Override
			public Executor getExecutor() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	/*
	 * Config Key listenner redefinition and initialization
	 */
	private static void InitializeConfigKeyListenner() {
		configKeyListener = new ConfigServiceKeyChangeListener() {
			@Override
			public void handleKeyChange(String groupId, String key,
					Object value, NotifyAction notifyAction,
					NodeAction nodeAction) throws Exception {
				// TODO Auto-generated method stub
				System.out.print("groupId:" + groupId + " ");
				System.out.print("key:" + key + " ");
				System.out.print("value:" + value + " ");
				System.out.print("notify action : " + notifyAction.toString() + "  ");
				System.out.print("node action : " + nodeAction.toString() + "\n");

				keyNodeAction = nodeAction.toString();
				Map<String, Object> keyvalue = groupKeyDataMap.get(groupId);
				if (keyvalue == null) {
					keyvalue = new HashMap<String, Object>();
				}
				keyvalue.put(key, value);
				changeKeyDataSize = keyvalue.size();
				groupKeyDataMap.put(groupId, keyvalue);
			}

			@Override
			public Executor getExecutor() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	protected void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (Exception e) {
			System.out.print(time);
		}
	}


    /**
     * md5或者sha-1加密
     *
     * @param inputText
     *            要加密的内容
     * @param algorithmName
     *            加密算法名称：md5或者sha-1，不区分大小写
     * @return
     */
    private static String encrypt(String inputText, String algorithmName) {
        if (inputText == null) {
            throw new IllegalArgumentException("请输入要加密的内容");
        }
        if (algorithmName == null || "".equals(algorithmName.trim())) {
            algorithmName = "md5";
        }
        String encryptText = null;
        try {
            MessageDigest m = MessageDigest.getInstance(algorithmName);
            m.update(inputText.getBytes("UTF8"));
            byte s[] = m.digest();
            // m.digest(inputText.getBytes("UTF8"));
            return hex(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encryptText;
    }

    // 返回十六进制字符串
    private static String hex(byte[] arr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; ++i) {
            sb.append(Integer.toHexString((arr[i] & 0xFF) | 0x100).substring(1,
                    3));
        }
        return sb.toString();
    }

    public static void main(String args[]) {
        String s =encrypt("","md5");
        System.out.println(s);
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
