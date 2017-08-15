package com.weibo.dorteam.Bean;

public class ConfigInfo {

	private String groupId;
	private String sign;
	private ConfigNodeInfo[] nodes;
	public void setGroupId(String groupId){
		this.groupId = groupId;
	}
	public void setSign(String sign){
		this.sign = sign;
	}
	public void setNodes(ConfigNodeInfo[] nodes){
		this.nodes = nodes;
	}
	
	public String getGroupId() {
		return this.groupId;
	}
	public String getSign() {
		return this.sign;
	}
	public ConfigNodeInfo[] getNodes(){
		return this.nodes;
	}
	
	
}
