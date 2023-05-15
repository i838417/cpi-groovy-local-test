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
	def eventType = message.getHeaders().get("eventType")

//	if(messageLog != null){
//		messageLog.addAttachmentAsString("MT Result Message", body, "text/xml")
//	}
	def input = []
//	try {
		def inputArray = new JsonSlurper().parseText(body)
		input = inputArray[0]
//	}catch(def e){
//			message.setHeader("Content-Type", "application/json" + "; charset=utf-8" )
//			def catenaXid = message.getHeaders()["catenaXId"]
//			message.setBody(" item ("+catenaXid+") does not exist  ")
//			return message
//	}

	def resultMap = [:]
	if (input != null){
		def localIdentifiersArray = []
		def localIdentifiersMap = [:]
		def manufacturingInformationMap = [:]
		def partTypeInformationMap = [:]

		localIdentifiersMap.key = "PartInstanceID"
		if(eventType == "ProduceSerialNumberEvent"){
			localIdentifiersMap.value = input.serialId
			localIdentifiersArray << localIdentifiersMap
		}else{
			localIdentifiersMap.value = input.batchId
			localIdentifiersArray << localIdentifiersMap
			localIdentifiersMap = [:]
			localIdentifiersMap.key = "batchId"
			localIdentifiersMap.value = input.batchId
			localIdentifiersArray << localIdentifiersMap
		}
		
		resultMap.localIdentifiers = localIdentifiersArray
		manufacturingInformationMap.date = getFormatedDate(input.creationDate)
		manufacturingInformationMap.country = input.location
		resultMap.manufacturingInformation = manufacturingInformationMap
		resultMap.catenaXId = "urn:uuid:"+input["p.CATENAX_UID"]
		partTypeInformationMap.manufacturerPartId = input.productId
		partTypeInformationMap.customerPartId = input.productId
		partTypeInformationMap.classification = "Product"
		partTypeInformationMap.nameAtManufacturer = input.productName
		partTypeInformationMap.nameAtCustomer = input.productName
		resultMap.partTypeInformation = partTypeInformationMap
	}
	def mtJson = new JsonBuilder(resultMap)

//	if(messageLog != null){
//		messageLog.addAttachmentAsString("SerialPart Message", mtJson.toPrettyString(), "text/xml")
//	}

	message.setHeader("Content-Type", "application/json" + "; charset=utf-8" )
	message.setBody(mtJson.toPrettyString())
	return message
}

def getFormatedDate(def inDate) {
	if(inDate == null ) return ""
	Date date1 = new SimpleDateFormat("yyyyMMdd").parse(inDate)
	def delta = Math.abs(new Random().nextInt() % 9000000) +1000000
	date1 = new Date(date1.getTime()+delta)
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	return sdf.format(date1.getTime())
}