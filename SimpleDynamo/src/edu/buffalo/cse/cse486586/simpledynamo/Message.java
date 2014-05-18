package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;
import java.util.HashMap;

import android.database.MatrixCursor;

public class Message implements Serializable{
	
	private static final long serialVersionUID = 1L;
    private String key;
    private String value;
    private String type;
    private String originator;
    private String succ;
    private String comingFrom;
    private String insertForwrdTo;
    private HashMap <String,String> keyValue;
    private HashMap <String,String> localKeyValue;
    private HashMap <String,String> recoveryKeyValue;
    private HashMap <String,String> pred2KeyValue;
    private HashMap <String,Integer> remoteKeyVersion;
    
    
    
    String getValue() {
		return value;
	}
	void setValue(String value) {
		this.value = value;
	}
	String getType() {
		return type;
	}
	void setType(String type) {
		this.type = type;
	}

	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getOriginator() {
		return originator;
	}
	public void setOriginator(String originator) {
		this.originator = originator;
	}
	public String getSucc() {
		return succ;
	}
	public void setSucc(String succ) {
		this.succ = succ;
	}
	public HashMap <String,String> getKeyValue() {
		return keyValue;
	}
	public void setKeyValue(HashMap <String,String> keyValue) {
		this.keyValue = keyValue;
	}
	public String getComingFrom() {
		return comingFrom;
	}
	public void setComingFrom(String comingFrom) {
		this.comingFrom = comingFrom;
	}
	public HashMap <String,String> getLocalKeyValue() {
		return localKeyValue;
	}
	public void setLocalKeyValue(HashMap <String,String> localKeyValue) {
		this.localKeyValue = localKeyValue;
	}
	public HashMap <String,String> getPred2KeyValue() {
		return pred2KeyValue;
	}
	public void setPred2KeyValue(HashMap <String,String> pred2KeyValue) {
		this.pred2KeyValue = pred2KeyValue;
	}
	public HashMap <String,String> getRecoveryKeyValue() {
		return recoveryKeyValue;
	}
	public void setRecoveryKeyValue(HashMap <String,String> recoveryKeyValue) {
		this.recoveryKeyValue = recoveryKeyValue;
	}
	public String getInsertForwrdTo() {
		return insertForwrdTo;
	}
	public void setInsertForwrdTo(String insertForwrdTo) {
		this.insertForwrdTo = insertForwrdTo;
	}
	public HashMap <String,Integer> getRemoteKeyVersion() {
		return remoteKeyVersion;
	}
	public void setRemoteKeyVersion(HashMap <String,Integer> remoteKeyVersion) {
		this.remoteKeyVersion = remoteKeyVersion;
	}	
}
