package com.weibo.dorteam.Bean;

public class NamingInfo {

	private String service;
	private String cluster;
	private String sign;
	private NamingNodesInfo nodes;
	
	public void setService(String name){
		this.service = name;
	}
	public void setCluster(String cluster){
		this.cluster = cluster;
	}
	public void setNodes(NamingNodesInfo nodes) {
		this.nodes = nodes;
	}
	
	public String getService() {
		return this.service;
	}
	public String getCluster() {
		return this.cluster;
	}
	public NamingNodesInfo getNodes() {
		return this.nodes;
	}
}
