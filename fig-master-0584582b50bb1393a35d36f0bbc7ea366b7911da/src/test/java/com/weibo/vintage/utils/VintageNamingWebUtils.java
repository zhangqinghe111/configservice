package com.weibo.vintage.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.weibo.vintage.model.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;

import com.weibo.vintage.exception.HttpRequestException;
import com.weibo.vintage.failover.NodeExciseStrategy;
import com.weibo.vintage.json.JsonUtil;
//import com.weibo.vintage.model.HttpResponseMessage;

/**
 * 更正：许多get返回null的可能性导致case中判断异常
 * 
 * @author liuyu9
 * 
 */

public class VintageNamingWebUtils {
	private static HttpClient client = new DefaultHttpClient();
	
	public static Set<String> getWhiteList(String serviceId) {
		String action = "get";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_WHITELIST_URL,
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
			Set<String> nodeSet = new HashSet<String>();
			for (String nodeString : nodeList) {
				nodeSet.add(nodeString);
			}
			return nodeSet;
		}
		return null;
	}

	public static void addWhitelist(String serviceId, Set<String> nodes) {
		VintageNamingWebUtils.addWhitelist(serviceId, setToString(nodes));
	}
	
	public static boolean addWhitelist(String serviceId, String node) {
		String action = "add";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_WHITELIST_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static boolean addWhitelist(String prefix, String serviceId, String node) {
		String action = "add";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(prefix, params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean addWhitelist_forbatch(String serviceId, String node) {
		String action = "add";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_WHITELIST_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static void batchaddWhitelist(String serviceId, String node, int num) {
		for (int i = 0; i < num; i++){
			if (!existsWhitelist(serviceId+i, node)) {
				addWhitelist_forbatch(serviceId+i, node);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void deleteWhitelist(String serviceId, Set<String> nodes) {
		VintageNamingWebUtils.deleteWhitelist(serviceId, setToString(nodes));
	}

	public static boolean deleteWhitelist(String serviceId, String node) {
		String action = "delete";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_WHITELIST_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean deleteWhitelist_forbatch(String serviceId, String node) {
		String action = "delete";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_WHITELIST_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static void batchdeleteWhitelist(String serviceId, String node, int num){
		for (int i = 0; i < num; i++){
			if (existsWhitelist(serviceId+i, node)){
				deleteWhitelist_forbatch(serviceId+i, node);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean existsWhitelist(String serviceId, String node) {
		String action = "exists";
		String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_WHITELIST_URL,
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

	public static boolean addService(String serviceId) {
		return addService(serviceId, NamingServiceType.statics.toString(), true);
	}
	
	public static boolean addService(String serviceId, String type) {
		return addService(serviceId, type, true);
	}
	
	public static boolean addService(String serviceId, Boolean cache) {
		return addService(serviceId, NamingServiceType.statics.toString(), cache);
	}
	
	public static boolean addService(String serviceId, String type, Boolean cache) {
		String action = "addservice";
		String params = "action="+action+"&service="+serviceId+"&type="+type;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			if (cache){
				Thread.sleep(VintageConstantsTest.serviceCacheInterval);
			} 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean addService(String prefix, String serviceId, String type, Boolean cache) {
		String action = "addservice";
		String params = "action="+action+"&service="+serviceId+"&type="+type;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(prefix, params);
			if (cache){
				Thread.sleep(VintageConstantsTest.serviceCacheInterval);
			} 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static void batchaddService(String serviceId, String type, int num){
		for (int i = 0; i < num; i++){
			if (! existsService(serviceId + i)){
				addService(serviceId + i, type, false);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.serviceCacheInterval);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static List<String> getServiceList(String serviceId) {
		return getServiceList();
	}

	public static NamingServiceInfo getService(String serviceId) {
		Set<NamingServiceInfo> infoSets = VintageNamingWebUtils
				.getSingleServiceInfoSet(serviceId);
		if (infoSets != null) {

			for (NamingServiceInfo namingServiceInfo : infoSets) {
				return namingServiceInfo;
			}
		}
		return null;
	}
	
	public static List<String> getServiceList() {
		String action = "getservice";
		String params = "action="+action;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
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
					.getFieldValue("services"));
			return nodeList;
		}
		return null;
	}

	public static List<String> getSingleService(String service) {
		String action = "getservice";
		String params = "action="+action+"&service="+service;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
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
					.getFieldValue("services"));
			return nodeList;
		}
		return null;
	}

	public static Set<NamingServiceInfo> getSingleServiceInfoSet(String service) {
		String action = "getservice";
		String params = "action="+action+"&service="+service;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		Set<NamingServiceInfo> serviceInfoList = new HashSet<NamingServiceInfo>();
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("services"));
			if (nodeList != null && !nodeList.isEmpty()) {
				serviceInfoList = new HashSet<NamingServiceInfo>();
				for (String serviceNode : nodeList) {
					JsonNode jsServiceNode = JsonHelper
							.parserStringToJsonNode(serviceNode);
					NamingServiceInfo serviceInfo = new NamingServiceInfo(
							jsServiceNode.getFieldValue("name").getTextValue(),
							jsServiceNode.getFieldValue("type").getTextValue());
					serviceInfoList.add(serviceInfo);
					// return serviceInfo;
				}
			}
		}
		return serviceInfoList;
	}
	
	public static Set<NamingServiceInfo> getSingleServiceInfoSet(String prefix, String service) {
		String action = "getservice";
		String params = "action="+action+"&service="+service;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		Set<NamingServiceInfo> serviceInfoList = new HashSet<NamingServiceInfo>();
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("services"));
			if (nodeList != null && !nodeList.isEmpty()) {
				serviceInfoList = new HashSet<NamingServiceInfo>();
				for (String serviceNode : nodeList) {
					JsonNode jsServiceNode = JsonHelper
							.parserStringToJsonNode(serviceNode);
					NamingServiceInfo serviceInfo = new NamingServiceInfo(
							jsServiceNode.getFieldValue("name").getTextValue(),
							jsServiceNode.getFieldValue("type").getTextValue());
					serviceInfoList.add(serviceInfo);
					// return serviceInfo;
				}
			}
		}
		return serviceInfoList;
	}

	public static Set<NamingServiceInfo> getServiceInfoSet() {
		String action = "getservice";
		Set<NamingServiceInfo> serviceInfoList = new HashSet<NamingServiceInfo>();
		String params = "action="+action;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
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
					.getFieldValue("services"));
			if (nodeList != null && !nodeList.isEmpty()) {
				serviceInfoList = new HashSet<NamingServiceInfo>();
				for (String serviceNode : nodeList) {
					JsonNode jsServiceNode = JsonHelper
							.parserStringToJsonNode(serviceNode);
					NamingServiceInfo serviceInfo = new NamingServiceInfo(
							jsServiceNode.getFieldValue("name").getTextValue(),
							jsServiceNode.getFieldValue("type").getTextValue());
					serviceInfoList.add(serviceInfo);
				}
			}
		}
		return serviceInfoList;
	}

	public static Map<String, NamingServiceInfo> getServiceInfoMap() {
		String action = "getservice";
		String params = "action="+action;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
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
					.getFieldValue("services"));
			Map<String, NamingServiceInfo> serviceInfoMaps = new HashMap<String, NamingServiceInfo>();
			if (nodeList != null && nodeList.size() > 0) {

				for (String strNode : nodeList) {
					JsonNode subNode = JsonHelper
							.parserStringToJsonNode(strNode);
					String name = JsonUtil.getJsonTextValue(
							subNode.getFieldValue("name"), "");
					String type = JsonUtil.getJsonTextValue(
							subNode.getFieldValue("type"),
							NamingServiceType.statics.toString());
					serviceInfoMaps
							.put(name, new NamingServiceInfo(name, type));
				}
			}
			return serviceInfoMaps;
		}
		return null;
	}

	public static boolean deleteService(String serviceId) {
		String action = "deleteservice";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean deleteService_forbatch(String serviceId) {
		String action = "deleteservice";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static void batchdeleteService(String serviceId, int num) {
		for(int i = 0; i < num; i++){
			if (existsService(serviceId + i)) {
				deleteService_forbatch(serviceId + i);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.serviceCacheInterval);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean existsService(String serviceId) {
		// if (StringUtils.isBlank(serviceId)) {
		// throw new VintageException(ExcepFactor.E_PARAM_INVALID_ERROR,
		// new Object[] { "service", "待检验的服务标识符不能为空", serviceId });
		// }
//		Set<NamingServiceInfo> infoSet = getSingleServiceInfoSet(serviceId
//				+ "&useSmart=false");
		Set<NamingServiceInfo> infoSet = getSingleServiceInfoSet(serviceId);
		return !infoSet.isEmpty();
	}
	
	public static boolean existsService(String prefix, String serviceId) {
		// if (StringUtils.isBlank(serviceId)) {
		// throw new VintageException(ExcepFactor.E_PARAM_INVALID_ERROR,
		// new Object[] { "service", "待检验的服务标识符不能为空", serviceId });
		// }
//		Set<NamingServiceInfo> infoSet = getSingleServiceInfoSet(serviceId
//				+ "&useSmart=false");
		Set<NamingServiceInfo> infoSet = getSingleServiceInfoSet(prefix, serviceId);
		return !infoSet.isEmpty();
	}

	public static boolean addCluster(String serviceId, String cluserId) {
		String action = "addcluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean addCluster(String prefix, String serviceId, String cluserId) {
		String action = "addcluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(prefix, params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean addCluster_forbatch(String serviceId, String cluserId) {
		String action = "addcluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static void batchaddCluster(String serviceId, String cluserId, int num) {
		for (int i = 0; i < num; i++){
			if (!VintageNamingWebUtils.existCluster(serviceId, cluserId+i)){
				addCluster_forbatch(serviceId, cluserId+i);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchaddClusterService(String serviceId, String cluserId, int num) {
		for (int i = 0; i < num; i++){
			if (!VintageNamingWebUtils.existCluster(serviceId+i, cluserId)){
				addCluster_forbatch(serviceId+i, cluserId);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchaddCluster(String serviceId, String cluserId, int start, int end) {
		for (int i = start; i < end; i++){
			if (!VintageNamingWebUtils.existCluster(serviceId, cluserId+i)){
				addCluster_forbatch(serviceId, cluserId+i);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean deleteCluster(String serviceId, String cluserId) {
		String action = "deletecluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static boolean deleteCluster_forbatch(String serviceId, String cluserId) {
		String action = "deletecluster";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static void batchdeleteCluster(String serviceId, String clusterId, int num) {
		for(int i = 0; i < num; i ++){
			if (VintageNamingWebUtils.existCluster(serviceId, clusterId+i)){
				deleteCluster_forbatch(serviceId, clusterId+i);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchdeleteClusterService(String serviceId, String clusterId, int num) {
		for(int i = 0; i < num; i ++){
			if (VintageNamingWebUtils.existCluster(serviceId+i, clusterId)){
				deleteCluster_forbatch(serviceId+i, clusterId);
			}
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static boolean validateResponse(ResponsePacket packet, String action) {
		return VintageUtils.validateResponse(packet, action);
	}

	public static boolean updateService(String serviceId) {
		return updateService(serviceId, NamingServiceType.statics.toString());
	}

	public static boolean updateService(String serviceId, String type) {
		String action = "updateservice";
		String params = "action="+action+"&service="+serviceId+"&type="+type;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static Set<NamingServiceCluster> getCluster(String serviceId) {
		String action = "getcluster";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
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
					.getFieldValue("clusters"));
			if (nodeList!=null && !nodeList.isEmpty()){
				Set<NamingServiceCluster> clusterSet = new HashSet<NamingServiceCluster>();
				for (String cluster : nodeList) {
					clusterSet.add(new NamingServiceCluster(serviceId, cluster));
				}
				return clusterSet;
			}
		}
		return null;
	}
	
	public static Set<NamingServiceCluster> getCluster(String prefix, String serviceId) {
		String action = "getcluster";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> nodeList = JsonUtil.getJsonMultiValues(jsNode
					.getFieldValue("clusters"));
			if (nodeList!=null && !nodeList.isEmpty()){
				Set<NamingServiceCluster> clusterSet = new HashSet<NamingServiceCluster>();
				for (String cluster : nodeList) {
					clusterSet.add(new NamingServiceCluster(serviceId, cluster));
				}
				return clusterSet;
			}
		}
		return null;
	}
	
	public static boolean existCluster(String serviceId, String clusterId) {
		Set<NamingServiceCluster> namingServiceClusters = VintageNamingWebUtils
				.getCluster(serviceId);
		if (namingServiceClusters == null) {
			return false;
		}
		for (NamingServiceCluster cluster : namingServiceClusters) {
			if (cluster.getClusterId().toString().equals(clusterId)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean existCluster(String prefix, String serviceId, String clusterId) {
		Set<NamingServiceCluster> namingServiceClusters = VintageNamingWebUtils
				.getCluster(prefix, serviceId);
		if (namingServiceClusters == null) {
			return false;
		}
		for (NamingServiceCluster cluster : namingServiceClusters) {
			if (cluster.getClusterId().toString().equals(clusterId)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * get the results directly
	 */
	
	public static Set<NamingServiceNode> lookupforupdate(String service,
			String cluster, String sign) {
		VintageUtils.isBlank(service, "查找服务时,服务标识符不能为空");
		VintageUtils.isBlank(cluster, "查找服务时,集群标识符不能为空");
		
		String action = VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE;
		String params = "action="+action+"&service="+service+"&cluster="+cluster+"&sign="+sign;
		try {
			String localurl = VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL + "?" + params;
			HttpGet httpget = new HttpGet(localurl);
			HttpResponse response = client.execute(httpget); 
			
			if (response == null){
				VintageLogger
				.error("naming service lookupForUpdate failed, result is null, "
						+ " service = "
						+ service
						+ " cluster="
						+ cluster
						+ ", sign: " + sign);
				throw new HttpRequestException(
				"Fail to lookup for update, case result message is null! ");
			} else if (response.getStatusLine().getStatusCode() == 304) {
				return null;
			} else {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent()));
				StringBuffer sb = new StringBuffer("");
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();
				ResponsePacket packet = ResponsePacket.parser(sb.toString());
				if (VintageUtils.validateResponse(packet,
						VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE)) {
					String jsonData = packet.getBody();
					NamingServiceCluster namingCluster = NamingServiceCluster
							.parser(jsonData);
					if (namingCluster != null) {
						VintageLogger
								.info("naming service lookup for update success, "
										+ " code = " + packet.getStatusCode()
										+ " response=" + packet.toString());
						return (new NodeExciseStrategy.Statics())
								.process(namingCluster);
					} else {
						VintageLogger
								.error("naming service lookup for update failed, result is null, "
										+ " code = "
										+ packet.getStatusCode()
										+ " response=" + packet.toString());
						throw new HttpRequestException(
								"Cluster received from server is null"
										+ ", service = " + service + " cluster="
										+ cluster + ", sign=" + sign);
					}
				}
				throw new HttpRequestException(
						"Fail to validate response from server, package: "
								+ (packet == null ? null : packet.toJson())
								+ ", service = " + service + " cluster=" + cluster
								+ ", sign=" + sign);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	/*
	 * get the results directly
	 */
	public static NamingServiceCluster lookupforupdate(String prefix_url, String service,
			String cluster, String sign) {
		VintageUtils.isBlank(service, "查找服务时,服务标识符不能为空");
		VintageUtils.isBlank(cluster, "查找服务时,集群标识符不能为空");
		
		String action = VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE;
		String params = "action="+action+"&service="+service+"&cluster="+cluster+"&sign="+sign;
		try {
			String localurl = prefix_url + "?" + params;
			HttpGet httpget = new HttpGet(localurl);
			HttpResponse response = client.execute(httpget);
			if (response == null){
				VintageTestLogger.nodeLog(localurl+" naming service lookupForUpdate failed, result is null, "
						+ " service = "
						+ service
						+ " cluster="
						+ cluster
						+ ", sign: " + sign);
				throw new HttpRequestException(
				"Fail to lookup for update, case result message is null! ");
			} else if (response.getStatusLine().getStatusCode() == 304) {
				VintageTestLogger.nodeLog(localurl + " return 304");
				return null;
			} else {
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent()));
				StringBuffer sb = new StringBuffer("");
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();
				ResponsePacket packet = ResponsePacket.parser(sb.toString());
				if (VintageUtils.validateResponse(packet,
						VintageConstants.CONFIG_SERVICE_LOOKUPFORUPDATE)) {
					String jsonData = packet.getBody();
					NamingServiceCluster namingCluster = NamingServiceCluster
							.parser(jsonData);
					if (namingCluster != null) {
						VintageTestLogger.nodeLog(localurl+" naming service lookup for update success, "
								+ ", service = " + namingCluster.getServiceId() + " cluster="
								+ namingCluster.getClusterId() + ", oldsign=" + sign + ", newsign=" 
								+namingCluster.getSign() + " code = " + packet.getStatusCode()
										+ " response=" + packet.toString());
//						return (new NodeExciseStrategy.Statics())
//								.process(namingCluster);
						return namingCluster;
					} else {
						VintageTestLogger.nodeLog(localurl+" naming service lookup for update failed, result is null, "
										+ " code = "
										+ packet.getStatusCode()
										+ " response=" + packet.toString());
						throw new HttpRequestException(
								"Cluster received from server is null"
										+ ", service = " + service + " cluster="
										+ cluster + ", sign=" + sign);
					}
				}
				throw new HttpRequestException(
						"Fail to validate response from server, package: "
								+ (packet == null ? null : packet.toJson())
								+ ", service = " + service + " cluster=" + cluster
								+ ", sign=" + sign);
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	/*
	 * get the results directly
	 */
	public static String lookup(String service, String cluster) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static String lookup(String prefix, String service, String cluster) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(prefix, params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	/*
	 * get the results directly
	 */
	public static Set<String> lookup_set(String service, String cluster) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		Set<String> serviceSet = null;
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> w_serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("nodes").getFieldValue("working"));
			List<String> u_serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("nodes").getFieldValue("unreachable"));
			serviceSet = new HashSet<String>();
			for (String serviceString : w_serviceList) {
				serviceSet.add(serviceString);
			}
			for (String serviceString : u_serviceList) {
				serviceSet.add(serviceString);
			}
		}
		return serviceSet;
	}

	/*
	 * get the results directly
	 */
	public static Set<String> lookup_set(String service, String cluster, String status) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		Set<String> serviceSet = null;
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("nodes").getFieldValue(status));
			serviceSet = new HashSet<String>();
			for (String serviceString : serviceList) {
				serviceSet.add(serviceString);
			}
		}
		return serviceSet;
	}
	
	/*
	 * get the results directly
	 */
	public static String smartlookup(String service, String cluster) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SMARTSERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public static String getsign(String service, String cluster) {
		String action = "getsign";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
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
	
	public static String getsign(String prefix, String service, String cluster) {
		String action = "getsign";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(prefix, params);
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

	public static String smartgetsign(String service, String cluster) {
		String action = "getsign";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SMARTSERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {

			return packet.getBody();
		}
		return null;
	}

	/*
	 * get sign through lookup action
	 */
	public static String lookupSign(String service, String cluster) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			String signValue = jsNode.getFieldValue("sign").getValueAsText();
			return signValue;
		}
		return null;
	}

	/*
	 * get sign through lookup action
	 */
	public static String smartlookupSign(String service, String cluster) {
		String action = "lookup";
		String params = "action="+action+"&service="+service+"&cluster="+cluster;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SMARTSERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			String signValue = jsNode.getFieldValue("sign").getValueAsText();
			return signValue;
		}
		return null;
	}
	
	public static String register(String serviceId, String clusterId, String ip, int port) {
		String node = ip + ":" + port;
		String action = "register";
		String params = "action="+action+"&service="+serviceId+"&cluster="+clusterId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
	}
	
	public static String register(String serviceId, String clusterId, String ip, int port, String extinfo) {
		String node = ip + ":" + port;
		String action = "register";
		String params = "action="+action+"&service="+serviceId+"&cluster="+clusterId+"&node="+node+"&extInfo="+extinfo;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
	}
	
	public static String register_forbatch(String serviceId, String clusterId, String ip, int port, String extinfo) {
		String node = ip + ":" + port;
		String action = "register";
		String params = "action="+action+"&service="+serviceId+"&cluster="+clusterId+"&node="+node+"&extInfo="+extinfo;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
	}
	
	public static void batchregister(String serviceId, String clusterId, String ip, int port, String extinfo, int num) {
		for (int i = 0; i < num; i++){
			register_forbatch(serviceId, clusterId, ip, port+i, extinfo);
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchregistercluster(String serviceId, String clusterId, String ip, int port, int num) {
		for (int i = 0; i < num; i++){
			register_forbatch(serviceId, clusterId+i, ip, port, "");
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchregistercluster(String serviceId, String clusterId, String ip, int port, int start, int end) {
		for (int i = start; i < end; i++){
			register_forbatch(serviceId, clusterId+i, ip, port, "");
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchregisterservice(String serviceId, String clusterId, String ip, int port, int num) {
		for (int i = 0; i < num; i++){
			register_forbatch(serviceId+i, clusterId, ip, port, "");
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void batchregister(String serviceId, String clusterId, String ip, int startport, int endport, String extinfo) {
		for (int i = startport; i < endport; i++){
			register_forbatch(serviceId, clusterId, ip, i, extinfo);
		}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public static boolean unregister(String serviceId, String cluserId,String ip,int port) {
        String node = ip + ":" + port;
        String action = "unregister";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
        return validateResponse(packet, action);
    }
    
    public static boolean unregister_forbatch(String serviceId, String cluserId,String ip,int port) {
        String node = ip + ":" + port;
        String action = "unregister";
		String params = "action="+action+"&service="+serviceId+"&cluster="+cluserId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
        return validateResponse(packet, action);
    }
    
    public static void batch_unregister(String serviceId, String cluserId,String ip,int port, int num) {
    	for (int i = 0; i < num; i++){
    		unregister_forbatch(serviceId, cluserId, ip, port+i);
    	}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void batchunregistercluster(String serviceId, String cluserId,String ip,int port, int num) {
    	for (int i = 0; i < num; i++){
    		unregister_forbatch(serviceId, cluserId+i, ip, port);
    	}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void batchunregistercluster(String serviceId, String cluserId,String ip,int port, int startnum, int endnum) {
    	for (int i = startnum; i < endnum; i++){
    		unregister_forbatch(serviceId, cluserId+i, ip, port);
    	}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void batchunregisterservice(String serviceId, String cluserId,String ip,int port, int num) {
    	for (int i = 0; i < num; i++){
    		unregister_forbatch(serviceId+i, cluserId, ip, port);
    	}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void batchunregisterservice(String serviceId, String cluserId,String ip,int port, int startnum, int endnum) {
    	for (int i = startnum; i < endnum; i++){
    		unregister_forbatch(serviceId+i, cluserId, ip, port);
    	}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void batchunregister(String serviceId, String cluserId, String ip, int startport, int endport){
    	for (int i = startport; i < endport; i++){
    		unregister_forbatch(serviceId, cluserId, ip, i);
    	}
		try {
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static Set<String> batchunregister(String serviceId, String ip, int port) {
    	String node = ip+":"+port;
    	String action = "batchunregister";
    	String params = "action="+action+"&service="+serviceId+"&node="+node;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet
					.getBody());
			List<String> serviceList = JsonUtil.getJsonMultiValues(jsNode.getFieldValue("results"));
			Set<String> serviceSet = new HashSet<String>();
			for (String serviceString : serviceList) {
				serviceSet.add(serviceString);
			}
			return serviceSet;
			
		}
		return null;
    }
    
    public static Set<String> getnodeinfo(String serviceId, String ip) {
    	String action = "getnodeinfo";
    	String params = "action="+action+"&service="+serviceId+"&ip="+ip;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
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
			Set<String> serviceSet = new HashSet<String>();
			for (String serviceString : serviceList) {
				serviceSet.add(serviceString);
			}
			return serviceSet;
		}
		return null;
    }

    public static Set<String> getnodeservice(String ip) {
		String action = "getnodeservice";
		String params = "action="+action+"&ip="+ip;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_SERVICE_URL,
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
			Set<String> serviceSet = new HashSet<String>();
			for (String serviceString : serviceList) {
				serviceSet.add(serviceString);
			}
			return serviceSet;
		}
		return null;
	}
	
	public static boolean updateThreshold(String serviceId, double threshold) {
		String thres = String.valueOf(threshold);
		String action = "updateservice";
		String params = "action="+action+"&service="+serviceId+"&threshold="+thres;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean addThreshold(String serviceId, String type, double threshold) {
		String thres = String.valueOf(threshold);
		String action = "addservice";
		String params = "action="+action+"&service="+serviceId+"&threshold="+thres+"&type="+type;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean addThreshold(String serviceId, double threshold) {
		String thres = String.valueOf(threshold);
		String action = "addservice";
		String params = "action="+action+"&service="+serviceId+"&threshold="+thres+"&type=dynamic";
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean delThreshold(String serviceId) {

		String action = "deleteservice";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}
	
	public static boolean updateThreshold(String serviceId, String thres) {

		String action = "updateservice";
		String params = "action="+action+"&service="+serviceId+"&threshold="+thres;
		String result = "";
		try {
			result = VintageNamingWebUtils.doPost(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
			Thread.sleep(VintageConstantsTest.HEARTBEATINTERVAL);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		return validateResponse(packet, action);
	}

	public static String getThreshold(String serviceId) {
		String action = "getservice";
		String params = "action="+action+"&service="+serviceId;
		String result = "";
		try {
			result = VintageNamingWebUtils.doGet(VintageConstantsTest.VINTAGE_NAMING_ADMIN_URL,
					params);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ResponsePacket packet = ResponsePacket.parser(result);
		if (validateResponse(packet, action)) {
			JsonNode jsNode = JsonHelper.parserStringToJsonNode(packet.getBody());
			//JsonNode jsnodes = jsNode.getFieldValue("body").getFieldValue("protection threshold");
			JsonNode jsonNode = jsNode.getFieldValue("services");
			for (int i = 0; i < jsonNode.size(); i ++){
				System.out.println(jsonNode.get(i).getFieldValue("name").toString());
				if (jsonNode.get(i).getFieldValue("name").toString().equals("\""+serviceId+"\"")) {
					return jsonNode.get(i).getFieldValue("threshold").toString();
				}
			}
		}
		return null;
	}
	
	private static String setToString(Set<String> nodes) {
		String nodeString = "";
		for (String node : nodes) {
			if (nodeString == "") {
				nodeString = node;
			} else {
				nodeString = nodeString + "," + node;
			}

		}
		return nodeString;
	}

    
	public static String doPost(String relativeurl, String parameters) throws Exception {
		BufferedReader in = null;
		
		if(relativeurl == null || relativeurl.equals(""))
			return null;
		String localurl = relativeurl;
		HttpPost httppost = new HttpPost(localurl);
		StringEntity reqEntity = new StringEntity(parameters,"UTF-8");
		reqEntity.setContentType("application/x-www-form-urlencoded");
		httppost.setEntity(reqEntity);
		HttpResponse response = client.execute(httppost);

		if (response.getStatusLine().getStatusCode() == 307) {
			Header[] hs = response.getHeaders("Location");
			String redirectUrl=hs[0].getValue();
			httppost.releaseConnection();
			return doPost(redirectUrl, parameters);
		}

		in = new BufferedReader(new InputStreamReader(response.getEntity()
				.getContent()));
		StringBuffer sb = new StringBuffer("");
		String line = "";
		String NL = System.getProperty("line.separator");
		while ((line = in.readLine()) != null) {
			sb.append(line + NL);
		}
		in.close();
		VintageTestLogger.accessLog("curl \"" + localurl + "\" -d \"" + parameters + "\"");
		String result = sb.toString();
		VintageTestLogger.accessLog(result);
		httppost.releaseConnection();
		return result;
	}
	
	public static String doGet(String relativeurl, String parameters) throws Exception {
		BufferedReader in = null;
		
		if(relativeurl == null || relativeurl.equals(""))
			return null;
		String localurl = relativeurl + "?" + parameters;
		HttpGet httpget = new HttpGet(localurl);
		HttpResponse response = client.execute(httpget);
		
		if (response.getStatusLine().getStatusCode() == 307) {
			Header[] hs = response.getHeaders("Location");
			String redirectUrl=hs[0].getValue();
			httpget.releaseConnection();
			return doGet(redirectUrl, parameters);
		}

		in = new BufferedReader(new InputStreamReader(response.getEntity()
				.getContent()));
		StringBuffer sb = new StringBuffer("");
		String line = "";
		String NL = System.getProperty("line.separator");
		while ((line = in.readLine()) != null) {
			sb.append(line + NL);
		}
		in.close();
		VintageTestLogger.accessLog("curl \"" + localurl + "\"");
		String result = sb.toString();
		VintageTestLogger.accessLog(result);
		httpget.releaseConnection();
		return result;	
	}
	
	public static void main(String[] args) {
		String serviceId="aaa";
		String clusterId="bbb";
		int port=1111;
		String localIP="127.0.0.1";
		VintageNamingWebUtils.register(serviceId, clusterId, localIP, port);
		String oldSign = VintageNamingWebUtils.getsign(serviceId, clusterId);
		
		//System.out.println(oldSign);
		register(serviceId, clusterId,"127.0.0.2", port);
		VintageNamingWebUtils.addWhitelist(serviceId,"127.0.0.2");
	
		Set<NamingServiceNode> nodeSet = VintageNamingWebUtils.lookupforupdate(serviceId, clusterId, oldSign);
		System.out.println(nodeSet);
	}
	
}
