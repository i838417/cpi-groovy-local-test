package com.sap.catenax.lookup

import com.sap.gateway.ip.core.customdev.processor.MessageImpl;
import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.time.LocalDate
import groovy.time.TimeCategory
import java.util.Date
import java.text.SimpleDateFormat

def Message processData(Message message) {
	
	JsonSlurper jsonSlurper = new JsonSlurper()
	def body = message.getBody()
//	def body = message.getBody(java.lang.String) as String
//	def messageLog = messageLogFactory.getMessageLog(message)
//	if(messageLog != null){
//		messageLog.addAttachmentAsString("DS Message", body, "text/xml")
//	}
	def input
//	try {
		input = new JsonSlurper().parseText(body)
//	}catch(def e){
//			message.setHeader("Content-Type", "application/json" + "; charset=utf-8" )
//			def catenaXid = message.getHeaders()["CamelHttpPath"]
//			message.setBody(" item ("+catenaXid+") does not exist  ")
//			message.setHeader("path", "ERROR" )
//			return message
//	}
	
	def queryMap = [:]
	def serializedProductKeyMap = [:]
	def productBatchKeyMap = [:]
	def requestedAttributesArray = ["creationDate","productName","plant","p.CATENAX_UID","keyEnc","status","serialId","batchId","productId","systemId","location"]
	def filterAttributesArray = []
	def filterAttributesMap = [:]
	def filtervalues = []
	def eventType = input.eventType
	
	if(eventType == "ProduceSerialNumberEvent"){
		serializedProductKeyMap.serialId = input.serialId
		serializedProductKeyMap.productId = input.productId
		serializedProductKeyMap.systemId = input.systemId
		queryMap.serializedProductKey = serializedProductKeyMap
		message.setHeader("serialId", input.serialId )
	}else{
		productBatchKeyMap.batchId = input.batchId
		productBatchKeyMap.productId = input.productId
		productBatchKeyMap.systemId = input.systemId
		queryMap.productBatchKey = productBatchKeyMap
		message.setHeader("batchId", input.batchId )
	}
 
	filterAttributesMap.name = "p.CATENAX_UID"
	filtervalues << input.catenaXId
	filterAttributesMap.values = filtervalues
	filterAttributesArray << filterAttributesMap
	//queryMap.serializedProductKey = serializedProductKeyMap
	queryMap.filterAttributes = filterAttributesArray
	queryMap.requestedAttributes = requestedAttributesArray
	
	
   // message.setHeader("serialId", input.serialId )
	message.setHeader("eventType", eventType )
	message.setHeader("productId", input.productId )
	message.setHeader("systemId", input.systemId )
	message.setHeader("catenaXId", input.catenaXId )

	

	def mtJson = new JsonBuilder(queryMap)
//	if(messageLog != null){
//		messageLog.addAttachmentAsString("AttributeQuery Message", mtJson.toPrettyString(), "text/xml")
//	}

	message.setHeader("Content-Type", "application/json" + "; charset=utf-8" )
	message.getHeaders().remove("CamelHttpPath")
	message.setHeader("path", "LBN-MT" )
	message.setBody(mtJson.toPrettyString())
	return message
}