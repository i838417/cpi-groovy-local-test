package com.sap.catenax.release3

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.asdk.datastore.*
import com.sap.it.api.asdk.runtime.*
import java.util.HashMap
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

Message processData(Message message) {
	writeProduceEventDS(message)
    return message
}


def writeProduceEventDS (Message message){
	def messageLog = messageLogFactory.getMessageLog(message)
	def in_payLoad = message.getBody(String.class)
	messageLog.addAttachmentAsString("MT DS IN ", " Input: "+in_payLoad , "text/xml")
		try{ 
		def service = new Factory(DataStoreService.class).getService()
		def input = new JsonSlurper().parseText(in_payLoad)
			

		if( service != null) {
			postProduceSerialNumberEventsDS(input,service,messageLog)
			postReceiveSerialNumberEventsDS(input,service,messageLog)
			postProduceEventsDS(input,service,messageLog)
			postReceiveEventsDS(input,service,messageLog)		
		}
	} catch(Exception e){
		messageLog.addAttachmentAsString("Error DS ", " Input: "+in_payLoad+ "Error :"+e.toString(), "text/xml")
	}		

}

def postProduceSerialNumberEventsDS(def input,def service,def messageLog){
				if(input.PostMaterialTraceabilityEventNotification != null && input.PostMaterialTraceabilityEventNotification.eventPackage != null 
			&& input.PostMaterialTraceabilityEventNotification.eventPackage.produceSerialNumberEvents != null){
				def senderLbnId  = input.PostMaterialTraceabilityEventNotification.senderLbnId
				def senderSystemId = input.PostMaterialTraceabilityEventNotification.senderSystemId
				def messageId = input.PostMaterialTraceabilityEventNotification.messageId
				def dsContent = [:]
				input.PostMaterialTraceabilityEventNotification.eventPackage.produceSerialNumberEvents.each{
					def dBean = new DataBean()
					def dConfig = new DataConfig()
					def catenaXid = ""
					dsContent.serialId = it.serialNumbers[0].serialId
					dsContent.productId = it.productId
					dsContent.systemId = it.systemId
					dsContent.senderLbnId = senderLbnId
					dsContent.senderSystemId = senderSystemId
					dsContent.messageId = messageId
					dsContent.payLoad = it
					dsContent.eventType = "ProduceSerialNumberEvent"
					catenaXid = it.properties[0].value
					dsContent.catenaXId = catenaXid 
					def dsJson = new JsonBuilder(dsContent)
					if(messageLog != null){
                		messageLog.addAttachmentAsString("DS Content ", dsJson.toPrettyString(), "text/xml")
            		}
					def byteArray = dsJson.toPrettyString() as byte[]
					dBean.setDataAsArray(byteArray)		
					dConfig.setStoreName("ProductInforByCatinaXid")
					dConfig.setId(catenaXid)
					dConfig.setOverwrite(true)
					result = service.put(dBean,dConfig)
					dConfig.setId(catenaXid+"_"+senderLbnId)
					result = service.put(dBean,dConfig)
				}
			}

}
def postProduceEventsDS(def input,def service,def messageLog){
				if(input.PostMaterialTraceabilityEventNotification != null && input.PostMaterialTraceabilityEventNotification.eventPackage != null 
			&& input.PostMaterialTraceabilityEventNotification.eventPackage.produceEvents != null){
				def senderLbnId  = input.PostMaterialTraceabilityEventNotification.senderLbnId
				def senderSystemId = input.PostMaterialTraceabilityEventNotification.senderSystemId
				def messageId = input.PostMaterialTraceabilityEventNotification.messageId
				def dsContent = [:]
				input.PostMaterialTraceabilityEventNotification.eventPackage.produceEvents.each{
					def dBean = new DataBean()
					def dConfig = new DataConfig()
					def catenaXid = ""
					dsContent.batchId = it.batchId
					dsContent.productId = it.productId
					dsContent.systemId = it.systemId
					dsContent.senderLbnId = senderLbnId
					dsContent.senderSystemId = senderSystemId
					dsContent.messageId = messageId
					dsContent.payLoad = it
					dsContent.eventType = "ProduceEvent"
					catenaXid = it.properties[0].value
					dsContent.catenaXId = catenaXid 
					def dsJson = new JsonBuilder(dsContent)
					if(messageLog != null){
                		messageLog.addAttachmentAsString("DS Content ", dsJson.toPrettyString(), "text/xml")
            		}
					def byteArray = dsJson.toPrettyString() as byte[]
					dBean.setDataAsArray(byteArray)		
					dConfig.setStoreName("ProductInforByCatinaXid")
					dConfig.setId(catenaXid)
					dConfig.setOverwrite(true)
					result = service.put(dBean,dConfig)
					dConfig.setId(catenaXid+"_"+senderLbnId)
					result = service.put(dBean,dConfig)
				}
			}

}
def postReceiveSerialNumberEventsDS(def input,def service,def messageLog){
				if(input.PostMaterialTraceabilityEventNotification != null && input.PostMaterialTraceabilityEventNotification.eventPackage != null 
			&& input.PostMaterialTraceabilityEventNotification.eventPackage.receiveSerialNumberEvents != null){
				def senderLbnId  = input.PostMaterialTraceabilityEventNotification.senderLbnId
				def senderSystemId = input.PostMaterialTraceabilityEventNotification.senderSystemId
				def messageId = input.PostMaterialTraceabilityEventNotification.messageId
				def dsContent = [:]
				input.PostMaterialTraceabilityEventNotification.eventPackage.receiveSerialNumberEvents.each{
					def dBean = new DataBean()
					def dConfig = new DataConfig()
					def catenaXid = ""
					dsContent.serialId = it.serialNumbers[0].serialId
					dsContent.productId = it.productId
					dsContent.systemId = it.systemId
					dsContent.senderLbnId = senderLbnId
					dsContent.senderSystemId = senderSystemId
					dsContent.messageId = messageId
					dsContent.payLoad = it
					dsContent.eventType = "ReceiveSerialNumberEvent"
					catenaXid = it.properties[0].value
					dsContent.catenaXId = catenaXid 
					def dsJson = new JsonBuilder(dsContent)
					if(messageLog != null){
                		messageLog.addAttachmentAsString("DS Content ", dsJson.toPrettyString(), "text/xml")
            		}
					def byteArray = dsJson.toPrettyString() as byte[]
					dBean.setDataAsArray(byteArray)		
					dConfig.setStoreName("ProductInforByCatinaXid")
					//dConfig.setId(catenaXid)
					dConfig.setOverwrite(true)
					//result = service.put(dBean,dConfig)
					dConfig.setId(catenaXid+"_"+senderLbnId)
					result = service.put(dBean,dConfig)
				}
			}
}
	def postReceiveEventsDS(def input,def service,def messageLog){
				if(input.PostMaterialTraceabilityEventNotification != null && input.PostMaterialTraceabilityEventNotification.eventPackage != null 
			&& input.PostMaterialTraceabilityEventNotification.eventPackage.receiveEvents != null){
				def senderLbnId  = input.PostMaterialTraceabilityEventNotification.senderLbnId
				def senderSystemId = input.PostMaterialTraceabilityEventNotification.senderSystemId
				def messageId = input.PostMaterialTraceabilityEventNotification.messageId
				def dsContent = [:]
				input.PostMaterialTraceabilityEventNotification.eventPackage.receiveEvents.each{
					def dBean = new DataBean()
					def dConfig = new DataConfig()
					def catenaXid = ""
					dsContent.batchId = it.batchId
					dsContent.productId = it.productId
					dsContent.systemId = it.systemId
					dsContent.senderLbnId = senderLbnId
					dsContent.senderSystemId = senderSystemId
					dsContent.messageId = messageId
					dsContent.payLoad = it
					dsContent.eventType = "ReceiveEvent"
					catenaXid = it.properties[0].value
					dsContent.catenaXId = catenaXid 
					def dsJson = new JsonBuilder(dsContent)
					if(messageLog != null){
                		messageLog.addAttachmentAsString("DS Content ", dsJson.toPrettyString(), "text/xml")
            		}
					def byteArray = dsJson.toPrettyString() as byte[]
					dBean.setDataAsArray(byteArray)		
					dConfig.setStoreName("ProductInforByCatinaXid")
					//dConfig.setId(catenaXid)
					dConfig.setOverwrite(true)
					//result = service.put(dBean,dConfig)
					dConfig.setId(catenaXid+"_"+senderLbnId)
					result = service.put(dBean,dConfig)
				}
			}

}