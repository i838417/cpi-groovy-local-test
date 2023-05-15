package com.sap.catenax.lookup.assemblypartrelationship

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

class Tester_getAssemblyPartRelation_Transform {

	static main(args) {
		def jsonFile = new File("src/com/sap/catenax/lookup/assemblypartrelationship/Payload_getAssemblyPartRelation_V2_Transform.json")
		String jsonContent = jsonFile.getText()
		JsonSlurper jsonSlurper = new JsonSlurper()
		def input = jsonSlurper.parseText(jsonContent)
		Message msg = new MessageImpl();
		msg.setBody(jsonContent);
		// message.setHeader("serialId", input.serialId )
		//msg.setHeader("eventType", "ProduceSerialNumberEvent" )
		//msg.setHeader("productId", "BATTPACK" )
		//msg.setHeader("systemId", "BPNL00000003AZQP-SYS" )
		msg.setHeader("catenaXId", "da633d40-30e6-478e-bd5c-849ff91ea034" )
	
		//msg.setHeader("DebugEnabled", "1");
		
		
		GroovyShell shell = new GroovyShell();
		def script = shell.parse(new File("src/com/sap/catenax/lookup/assemblypartrelationship/getAssemblyPartRelation_V2_Transform.groovy"));
		
		// Execute script
		script.processData(msg);
		
		println("Body:\r\n" + msg.getBody())
		
		def displayMaps = { String mapName, Map map ->
			println mapName
			map.each { key, value -> println( key + " = " + value) }
		}
		displayMaps("Headers:", msg.getHeaders())
		displayMaps("Properties:", msg.getProperties())
	}

}