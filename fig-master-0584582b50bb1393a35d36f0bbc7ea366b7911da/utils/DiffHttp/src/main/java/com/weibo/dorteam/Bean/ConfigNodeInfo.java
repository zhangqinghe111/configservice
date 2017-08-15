package com.weibo.dorteam.Bean;

public class ConfigNodeInfo {

	private String key;
	private String value;
	public void setKey(String key){
		this.key = key;
	}
	public void setValue(String value){
		this.value = value;
	}
	
	public String getKey() {
		return this.key;
	}
	public String getValue() {
		return this.value;
	}
	
	public boolean equals(Object obj) {
        if (obj instanceof ConfigNodeInfo) {
        	ConfigNodeInfo instance = (ConfigNodeInfo) obj;
            return (key.equals(instance.key) && value.equals(instance.value));
        }
        return super.equals(obj);
    }
	
	public int hashCode() {
		ConfigNodeInfo instance = (ConfigNodeInfo) this;
        return key.hashCode();
    }
}
