package com.weibo.dorteam.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;

import com.weibo.dorteam.Bean.NamingInfo;
import com.weibo.dorteam.Bean.NamingNodeInfo;
import com.weibo.dorteam.Bean.NamingServiceInfo;
import com.weibo.dorteam.Common.NamingCommon;
import com.weibo.vintage.json.JsonUtil;
import com.weibo.vintage.model.ResponsePacket;
import com.weibo.vintage.utils.JsonHelper;
import com.weibo.vintage.utils.VintageConstants;
import com.weibo.vintage.utils.VintageUtils;

public class VintageNamingUtils {

	private static PrefixUtils pu = new PrefixUtils();

	public static List<String> getWhitelistNodeList(String host, String serviceId) {
		String action = "get";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_whitelist"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("nodes"));
			return nodeList;
		}
		return null;
	}

	public static boolean addWhiteList(String host, String serviceId, String node) {
		String action = "add";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_whitelist"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static boolean deleteWhiteList(String host, String serviceId, String node) {
		String action = "delete";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_whitelist"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static boolean existsWhitelist(String host, String serviceId, String node) {
		String action = "exists";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_whitelist"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			return packet.getBody().equals("true");
		}
		return false;
	}

	public static boolean addService(String host, String params){
		String action = "addservice";
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_admin"),
					"action="+action+"&"+params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean updateService(String host, String params) {
		String action = "updateservice";
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_admin"),
					"action="+action+"&"+params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static Set<NamingServiceInfo> getServiceList(String host, String serviceId){
		String action = "getservice";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		Set<NamingServiceInfo> rset = null;
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_admin"),
					params);
			rset = NamingCommon.getServiceInfos(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rset;
	}

	public static Set<NamingServiceInfo> getServiceList(String host){
		String action = "getservice";
		String params = "action="+action;
		String result = "";
		Set<NamingServiceInfo> rset = null;
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_admin"),
					params);
			rset = NamingCommon.getServiceInfos(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rset;
	}

	public static boolean deleteService(String host, String serviceId) {
		String action = "deleteservice";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_admin"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static boolean addCluster(String host, String serviceId, String cluserId) {
		String action = "addcluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_admin"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static boolean deleteCluster(String host, String serviceId, String cluserId) {
		String action = "deletecluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_admin"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	private static boolean validateResponse(ResponsePacket packet, String action) {
		return VintageUtils.validateResponse(packet, action);
	}


	public static Set<String> getCluster(String host, String serviceId){
		String action = "getcluster";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		Set<String> rset = null;
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_admin"),
					params);
			rset = NamingCommon.getClusterInfos(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rset;
	}


	/*
	 * get the results directly
	 */
	public static NamingInfo lookup(String host, String service, String cluster){
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		NamingInfo rni = null;
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_service"),
					params);
			rni = NamingCommon.getNamingInfos(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rni;
	}

	public static Map<String, Set<NamingNodeInfo>> lookupNodes(String host, String service, String cluster){
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		Map<String, Set<NamingNodeInfo>> rmap = null;
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_service"),
					params);
			rmap = NamingCommon.getNamingNodesInfos(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rmap;
	}
	
	/*
	 * 返回的是lookup节点信息
	 */
	public static NamingInfo lookupforupdate(String host, String service,
			String cluster, String sign) throws Exception {
		
		String action = VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE;
		String params = "action="+action+"&service="+service+"&cluster="+cluster+"&sign="+sign;
		String result = "";
		NamingInfo rni = null;
		try {
			result = httpClient.lookupforupdate(pu.getUrlPrefix(host, "naming_service"),
					params);
			if (result == null) {
				return null;
			}
			rni = NamingCommon.getNamingInfos(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rni;
	}

	public static String getsign(String host, String service, String cluster) {
		String action = "getsign";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_service"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			String signValue = packet.getBody();
			return signValue;
		}
		return null;
	}

	public static String register(String host, String serviceId, String clusterId, String ip, int port) {
		String node = ip + ":" + port;
		String action = "register";
		String params = "action="+action+"&service="+serviceId+"&cluster="+clusterId+"&node="+node;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_service"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
	}
	
    public static boolean unregister(String host, String serviceId, String cluserId,String ip,int port) {
        String node = ip + ":" + port;
        String action = "unregister";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId+"&node="+node;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_service"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
        return validateResponse(packet, action);
    }
    
    public static List<String> batchunregister(String host, String serviceId, String ip, int port) {
    	String node = ip+":"+port;
    	String action = "batchunregister";
    	String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = httpClient.doPost(pu.getUrlPrefix(host, "naming_service"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("results"));
			return serviceList;
		}
		return null;
    }
    
    public static List<String> getnodeinfo(String host, String serviceId, String ip) {
    	String action = "getnodeinfo";
    	String params = "action="+action+"&service="+serviceId+"&ip="+ip;
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_service"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("nodeinfo"));
			return serviceList;
		}
		return null;
    }

    public static List<String> getnodeservice(String host, String ip) {
		String action = "getnodeservice";
		String params = "action="+action+"&ip="+ip;
		String result = "";
		try {
			result = httpClient.doGet(pu.getUrlPrefix(host, "naming_service"),
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("service"));
			return serviceList;
		}
		return null;
	}
    
	public static void main(String[] args) throws Exception{
		Logger logger  =  Logger.getLogger(VintageNamingUtils.class);
    	String host = "10.13.1.134:8090";
    	Set<NamingServiceInfo>sl = VintageNamingUtils.getServiceList(host);
    	for (NamingServiceInfo s : sl){
    		logger.info(s.getName());
    		logger.info(s.getThreshold());
    		logger.info(s.getType());
    		Set<String>cl = VintageNamingUtils.getCluster(host, s.getName());
    		for (String c : cl){
    			try{
					NamingInfo result = VintageNamingUtils.lookup(host, s.getName(), c);
					logger.info("get working nodes: ----" + s.getName() + "----" + c);
					for (int i = 0; i < result.getNodes().getWorking().length; i++){
						logger.info(result.getNodes().getWorking()[i].getHost());
						logger.info(result.getNodes().getWorking()[i].getExtInfo());
			    	}
					logger.info("get unreachable nodes: ----" + s.getName() + "----" + c);
			    	for (int i = 0; i < result.getNodes().getUnreachable().length; i++){
			    		logger.info(result.getNodes().getUnreachable()[i].getHost());
			    		logger.info(result.getNodes().getUnreachable()[i].getExtInfo());
			    	}
    			} catch (Exception e){
    				e.printStackTrace();
    				logger.error("aaaaaaaaaaaaaaaaaaaaaaaa");
    			}
    		}
    	}
    }
}