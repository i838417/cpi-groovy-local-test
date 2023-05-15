package com.sap.catenax.release3

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

class Tester {

	static main(args) {
		def jsonFile = new File("src/com/sap/catenax/release3/UI01.json")
		String jsonContent = jsonFile.getText()
		JsonSlurper jsonSlurper = new JsonSlurper()
		def input = jsonSlurper.parseText(jsonContent)
		Message msg = new MessageImpl();
		msg.setBody(jsonContent);
		msg.setHeader("Level", "0");
		msg.setHeader("LbnMTId", "10010001150");
		msg.setHeader("DebugEnabled", "1");
		
		GroovyShell shell = new GroovyShell();
		def script = shell.parse(new File("src/com/sap/catenax/release3/MTTransformation_Upload.groovy"));
		
		// Execute script
		script.processData(msg);
		
//		def json = new JsonBuilder()
//		println("json after " + json.toPrettyString())
		
	}

/*		static main(args) {
		// Load Groovy Script
		GroovyShell shell = new GroovyShell();
		//def script = shell.parse(new File("src/com/sap/catenax/Script1.groovy"));
		def script = shell.parse(new File("src/com/sap/catenax/release3/MTTransformation_Upload.groovy"));
		
		// Initialize message with body, header and property
		Message msg = new MessageImpl();
		msg.setBody(new String("Hello Groovy World"));
		msg.setHeader("oldHeader", "MyGroovyHeader");
		msg.setProperty("oldProperty", "MyGroovyProperty");
		
		// Execute script
		script.processData(msg);
		
		// Display results of script in console
		println("Body:\r\n" + msg.getBody());
		
		def displayMaps = { String mapName, Map map ->
			println mapName
			map.each { key, value -> println( key + " = " + value) }
		};
		displayMaps("Headers:", msg.getHeaders());
		displayMaps("Properties:", msg.getProperties());
	}
*/
}