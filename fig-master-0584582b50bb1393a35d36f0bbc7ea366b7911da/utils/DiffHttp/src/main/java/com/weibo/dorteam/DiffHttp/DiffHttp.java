package com.weibo.dorteam.DiffHttp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.weibo.dorteam.Bean.NamingNodeInfo;
import com.weibo.dorteam.Bean.NamingServiceInfo;
import com.weibo.dorteam.utils.ConfParser;
import com.weibo.dorteam.utils.VintageConfigUtils;
import com.weibo.dorteam.utils.VintageNamingUtils;

public class DiffHttp {
	public static Logger log = Logger.getLogger(DiffHttp.class);

	public static void main(String[] args){
		ConfParser cp = new ConfParser();
		String hostA = cp.getParameter("hostA");
		String hostB = cp.getParameter("hostB");
		CheckConfig(hostA, hostB);
		CheckNaming(hostA, hostB);
	}
	
	public static void CheckNaming(String hostA, String hostB) {
		try {
			Set<NamingServiceInfo> serviceA = VintageNamingUtils.getServiceList(hostA);
			Set<NamingServiceInfo> serviceB = VintageNamingUtils.getServiceList(hostB);
			Set<NamingServiceInfo> services = new HashSet<NamingServiceInfo>();
			services.addAll(serviceA);
			services.addAll(serviceB);
			
			for(NamingServiceInfo service:services){
				if (service.getName().contains("trigger")){
					continue;
				}
				if (serviceA.contains(service) && serviceB.contains(service)){
					Set<String>clusterA = VintageNamingUtils.getCluster(hostA, service.getName());
					Set<String>clusterB = VintageNamingUtils.getCluster(hostB, service.getName());
					Set<String> clusters = new HashSet<String>();
					clusters.addAll(clusterA);
					clusters.addAll(clusterB);

					for (String cluster:clusters) {
						if (clusterA.contains(cluster) && clusterB.contains(cluster)){
							String signA = VintageNamingUtils.getsign(hostA, service.getName(), cluster);
							String signB = VintageNamingUtils.getsign(hostB, service.getName(), cluster);
							if (!signA.equals(signB)){
								Map<String, Set<NamingNodeInfo>> nodesA = VintageNamingUtils.lookupNodes(hostA, service.getName(), cluster);
								Map<String, Set<NamingNodeInfo>> nodesB = VintageNamingUtils.lookupNodes(hostB, service.getName(), cluster);
								Set<NamingNodeInfo> nodes = new HashSet<NamingNodeInfo>();
								nodes.addAll(nodesA.get("working"));
								nodes.addAll(nodesB.get("working"));
								nodes.addAll(nodesA.get("unreachable"));
								nodes.addAll(nodesB.get("unreachable"));
								for (NamingNodeInfo node : nodes) {
									try{
										if(nodesA.get("working").contains(node) && nodesB.get("working").contains(node)) {
											log.info("Nodes consistent: service=" + service.getName() + " cluster=" + cluster + " " + node.getHost() + " is in the working list");
											continue;
										} else if(nodesA.get("unreachable").contains(node) && nodesB.get("unreachable").contains(node)) {
											log.info("Nodes consistent: service=" + service.getName() + " cluster=" + cluster + " " + node.getHost() + " is in the unreachable list");
											continue;
										}else if (nodesA.get("working").contains(node) && nodesB.get("unreachable").contains(node)){
							    			log.error("Diff in nodes: service=" + service.getName() + " cluster=" + cluster + " "+ node.getHost() + " in "+ hostA + " working list");
							    			log.error("Diff in nodes: service=" + service.getName() + " cluster=" + cluster + " "+ node.getHost() + " in "+ hostB + " unreachable list");
										} else if (nodesA.get("unreachable").contains(node) && nodesB.get("working").contains(node)){
							    			log.error("Diff in nodes: service=" + service.getName() + " cluster=" + cluster + " "+ node.getHost() + " in "+ hostA + " unreachable list");
							    			log.error("Diff in nodes: service=" + service.getName() + " cluster=" + cluster + " "+ node.getHost() + " in "+ hostB + " working list");
										} else if (nodesA.get("working").contains(node) || nodesA.get("unreachable").contains(node)) {
							    			log.error("Diff in nodes: service=" + service.getName() + " cluster=" + cluster + " "+ node.getHost() + " is not exist in "+ hostB);
										} else if (nodesB.get("working").contains(node) || nodesB.get("unreachable").contains(node)) {
							    			log.error("Diff in nodes: service=" + service.getName() + " cluster=" + cluster + " "+ node.getHost() + " is not exist in "+ hostA);
										}
									} catch(Exception e){
										e.printStackTrace();
									}
								}
							} else {
								log.info("Cluster consistent: service=" + service.getName() + " cluster=" + cluster);
							}
						} else if (clusterA.contains(cluster)){
			    			log.error("Diff in clusters: service=" + service.getName() + " " + cluster + " is not exist in " + hostB);
						} else if (clusterB.contains(cluster)){
			    			log.error("Diff in clusters: service=" + service.getName() + " " + cluster + " is not exist in " + hostA);
						}
					}
					
				} else if (serviceA.contains(service)){
	    			log.error("Diff in services: " + service.getName() + " is not exist in " + hostB);
				} else if (serviceB.contains(service)){
	    			log.error("Diff in services: " + service.getName() + " is not exist in " + hostA);
				}
			}

		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void CheckConfig(String hostA, String hostB){
		Set<String> groupA = VintageConfigUtils.getgroup(hostA);
		Set<String> groupB = VintageConfigUtils.getgroup(hostB);
		Set<String> groups = new HashSet<String>();
		groups.addAll(groupA);
		groups.addAll(groupB);
    	for(String group:groups){
    		if (groupA.contains(group) && groupB.contains(group)){
//    			if (group.equals("trigger-comment-core")){
//    				System.out.println("hello");
//    			}
    			String signA = VintageConfigUtils.getsign(hostA, group);
    			String signB = VintageConfigUtils.getsign(hostB, group);
    			if (! signA.equals(signB)){
    				Set<String>keysA = VintageConfigUtils.getkeys(hostA, group);
    				Set<String>keysB = VintageConfigUtils.getkeys(hostB, group);
    				Set<String>keys = new HashSet<String>();
    				keys.addAll(keysA);
    				keys.addAll(keysB);
    				for (String key: keys) {
    					if (keysA.contains(key) && keysB.contains(key)){
    		    			String valueA;
							try {
								valueA = VintageConfigUtils.lookup(hostA, "group="+group+"&key="+key).getNodes()[0].getValue();
	    		    			String valueB = VintageConfigUtils.lookup(hostB, "group="+group+"&key="+key).getNodes()[0].getValue();
	    		    			if (!valueA.equals(valueB)){
	    		    				log.error("Diff in value, "+hostA+": group=" + group+ " key=" + key + " value=" + valueA);
	    		    				log.error("Diff in value, "+hostB+": group=" + group+ " key=" + key + " value=" + valueB);
	    		    			}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
    		    			
    					} else if (keysA.contains(key)){
    						log.error("Diff in keys: group="+group+" "+ key + " is not exist in " + hostB);
    					} else if (keysB.contains(key)){
    						log.error("Diff in keys: group="+group+" "+ key + " is not exist in "+ hostA);
    					}
    				}
    			}
    		} else if(groupA.contains(group)) {
    			log.error("Diff in groups: " + group + " is not exist in " + hostB);
    		} else if(groupB.contains(group)){
    			log.error("Diff in groups: " + group + " is not exist in " + hostA);
    		}
    	}
	}
	
}
