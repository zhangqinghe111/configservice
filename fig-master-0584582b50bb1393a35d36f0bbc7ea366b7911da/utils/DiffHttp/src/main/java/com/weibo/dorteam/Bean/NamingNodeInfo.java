package com.weibo.dorteam.Bean;

public class NamingNodeInfo {

	private String host;
	private String extInfo;
	
	public void setHost(String host){
		this.host = host;
	}
	public void setExtInfo(String extinfo) {
		this.extInfo = extinfo;
	}
	public String getHost() {
		return this.host;
	}
	public String getExtInfo() {
		return this.extInfo;
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof NamingNodeInfo) {
        	NamingNodeInfo instance = (NamingNodeInfo) obj;
            return (host.equals(instance.host) && extInfo.equals(instance.extInfo));
        }
        return super.equals(obj);
    }
	
	public int hashCode() {
		NamingNodeInfo instance = (NamingNodeInfo) this;
        return host.hashCode();
    }
}
