package com.weibo.dorteam.Bean;

public class NamingNodesInfo {
	private NamingNodeInfo[] working;
	private NamingNodeInfo[] unreachable;
	
	public void setWorking(NamingNodeInfo[] working){
		this.working = working;
	}
	public void setUnreachable(NamingNodeInfo[] unreachable) {
		this.unreachable = unreachable;
	}
	public NamingNodeInfo[] getWorking() {
		return this.working;
	}
	public NamingNodeInfo[] getUnreachable() {
		return this.unreachable;
	}
}
