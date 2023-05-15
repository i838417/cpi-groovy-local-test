package com.sap.catenax.lookup

import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {

	def headers = message.getHeaders();
	def path = headers.get("CamelHttpPath");
	def id = getId(path)
	message.setHeader("catenaxID", id)
	return message;
}
	
def getId(def text) {
	def id = ""
	if(text.indexOf("urn%3Auuid%3A") > -1 ) {
		id = text.replace("urn%3Auuid%3A","")
	}else if(text.indexOf("urn:uuid:") > -1 ) {
		id = text.replace("urn:uuid:","")
	}else {
		id = text
	}
	return id
}