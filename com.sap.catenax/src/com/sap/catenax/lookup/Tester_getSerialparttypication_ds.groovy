package com.sap.catenax.lookup

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

class Tester_getSerialparttypication_ds {

	static main(args) {
		def jsonFile = new File("src/com/sap/catenax/lookup/getSerialparttypization_ds.json")
		String jsonContent = jsonFile.getText()
		JsonSlurper jsonSlurper = new JsonSlurper()
		def input = jsonSlurper.parseText(jsonContent)
		Message msg = new MessageImpl();
		msg.setBody(jsonContent);
		//msg.setHeader("DebugEnabled", "1");
		
		GroovyShell shell = new GroovyShell();
		def script = shell.parse(new File("src/com/sap/catenax/lookup/getSerialparttypization_V2_mt.groovy"));
		
		// Execute script
		script.processData(msg);
		
		
	}

}