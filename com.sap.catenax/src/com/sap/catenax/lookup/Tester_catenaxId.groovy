package com.sap.catenax.lookup

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

class Tester_catenaxId {

	static main(args) {
		//def jsonFile = new File("src/com/sap/catenax/lookup/UI01.json")
		//String jsonContent = jsonFile.getText()
		//JsonSlurper jsonSlurper = new JsonSlurper()
		//def input = jsonSlurper.parseText(jsonContent)
		Message msg = new MessageImpl();
		//msg.setBody(jsonContent);
		msg.setHeader("CamelHttpPath", "60f90177-8213-4cdc-a6a7-068cf3333ec1");
		//msg.setHeader("LbnMTId", "10010001150");
		//msg.setHeader("DebugEnabled", "1");
		
		GroovyShell shell = new GroovyShell();
		def script = shell.parse(new File("src/com/sap/catenax/lookup/getSerialparttypization_V2_catenaxId.groovy"));
		
		// Execute script
		script.processData(msg);
		
		
	}

}