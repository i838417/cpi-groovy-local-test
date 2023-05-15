package com.sap.catenax.lookup

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

class Tester_getSerialparttypication_mt_transform {

	static main(args) {
		def jsonFile = new File("src/com/sap/catenax/lookup/getSerialparttypization_mt.json")
		String jsonContent = jsonFile.getText()
		JsonSlurper jsonSlurper = new JsonSlurper()
		def input = jsonSlurper.parseText(jsonContent)
		Message msg = new MessageImpl();
		msg.setBody(jsonContent);
		// message.setHeader("serialId", input.serialId )
		msg.setHeader("eventType", "ProduceSerialNumberEvent" )
		msg.setHeader("productId", "BATTPACK" )
		msg.setHeader("systemId", "BPNL00000003AZQP-SYS" )
		msg.setHeader("catenaXId", "57479ef0-eac2-4255-a849-42c86355c618" )
	
		//msg.setHeader("DebugEnabled", "1");
		
		GroovyShell shell = new GroovyShell();
		def script = shell.parse(new File("src/com/sap/catenax/lookup/getSerialparttypization_V2_mt_transform.groovy"));
		
		// Execute script
		script.processData(msg);
		
		
	}

}