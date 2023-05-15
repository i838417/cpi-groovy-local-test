package com.sap.catenax.lookup.assemblypartrelationship

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import java.time.LocalDate
import groovy.time.TimeCategory
import java.util.Date
import java.text.SimpleDateFormat

class Tester_getAssemblyPartRelation_Transform2_v1 {

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
		//script.processData(msg);
		Tester_getAssemblyPartRelation_Transform2_v1 myClass = new Tester_getAssemblyPartRelation_Transform2_v1();
		myClass.processData(msg);
		
		println("Body:\r\n" + msg.getBody())
		
		def displayMaps = { String mapName, Map map ->
			println mapName
			map.each { key, value -> println( key + " = " + value) }
		}
		displayMaps("Headers:", msg.getHeaders())
		displayMaps("Properties:", msg.getProperties())
	}
	
	def Message processData(Message message) {
		
	   def body = message.getBody() as String
   //	def messageLog = messageLogFactory.getMessageLog(message)
	   def catenaXid = message.getHeaders().get("catenaXId")
	   def debugEnabled = message.getHeaders().get("DebugEnabled")
	   
   //	if(messageLog != null && debugEnabled == "1"){
   //		messageLog.addAttachmentAsString("MT Result Message", body, "text/xml")
   //	}
	   def input
	   def childIds
	   try {
		   input = new JsonSlurper().parseText(body)
		   childIds = getChildIds(input,catenaXid);
	   }catch(def e){
			   message.setHeader("Content-Type", "application/json" + "; charset=utf-8" )
			   message.setBody(" item ("+catenaXid+") does not exist  ")
			   return message
	   }
	   def resultMap = [:]
	   def childPartsArray = []
	   
	   input.ctxObjects.each{
		   if(childIds.contains(it.ctxId)  ){
				   def childMap = null
				   if(it.ProductBatch ) {
					   childMap = it.ProductBatch
				   }else if(it.ProductSerialNumber ) {
					   childMap = it.ProductSerialNumber
				   }
				   if(childMap != null) {
				   def childPartsMap = [:]
				   def quantityMap = [:]
				   //def measurementUnitMap = [:]
				   
				   childPartsMap.lifecycleContext = "AsBuilt"
				   
				   def lexicalValue
				   def datatypeURI
				   def quantityNumber
				   if(childMap.quantities[0].qualifier == "DISCRETE"){
					   quantityNumber = childMap.quantities[0].value
					   if(childMap.quantities[0].unit == "PC"){
						   lexicalValue = "piece"
					   }
					   datatypeURI =  "urn:bamm:io.openmanufacturing:meta-model:1.0.0#"+lexicalValue
				   }
				   else if(childMap.quantities[0].qualifier == "BASE_UNIT"){
					   
					   quantityNumber = childMap.quantities[0].value
					   if(childMap.quantities[0].unit == "KGM"){
						   lexicalValue = "kilogram"
					   }else if(childMap.quantities[0].unit == "MTR"){
						   lexicalValue = "meter"
					   }else if(childMap.quantities[0].unit == "LTR"){
						   lexicalValue = "litre"
					   }
					   datatypeURI =  "urn:bamm:io.openmanufacturing:meta-model:1.0.0#curie"
				   }
				   //measurementUnitMap.lexicalValue =  lexicalValue
				   //measurementUnitMap.datatypeURI =  datatypeURI
				   quantityMap.quantityNumber = quantityNumber
				   //quantityMap.measurementUnit = measurementUnitMap
				   quantityMap.measurementUnit = lexicalValue
				   childPartsMap.quantity = quantityMap
				   def formatedCreatedOn = getFormatedDate(childMap.creationDate)
				   childPartsMap.createdOn = formatedCreatedOn
				   childPartsMap.lastModifiedOn = formatedCreatedOn
				   childPartsMap.childCatenaXId = "urn:uuid:"+it.ctxId
				   childPartsArray << childPartsMap
			   }
		   }
	   }
   
   
	   resultMap.catenaXId = "urn:uuid:"+catenaXid
	   resultMap.childParts = childPartsArray
	   def mtJson = new JsonBuilder(resultMap)
   
   //	if(messageLog != null && debugEnabled == "1"){
   //		messageLog.addAttachmentAsString("assemblypartrelation Message", mtJson.toPrettyString(), "text/xml")
   //	}
   
	   message.setHeader("Content-Type", "application/json" + "; charset=utf-8" )
	   message.setBody(mtJson.toPrettyString())
	   return message
   }
   
   def getChildIds(def input,def catenaXid) {
	   def childIdList = []
	   input.ctxProductEdges.each {
		   if(it.toCtxId == catenaXid && it.fromCtxId ) {
			   childIdList << it.fromCtxId
		   }
	   }
	   return childIdList
   }
   def getFormatedDate(def inDate) {
	   if(inDate == null ) return ""
	   Date date1 = new SimpleDateFormat("yyyyMMdd").parse(inDate)
	   def delta = Math.abs(new Random().nextInt() % 9000000) +1000000
	   date1 = new Date(date1.getTime()+delta)
	   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	   return sdf.format(date1.getTime())
   }

}