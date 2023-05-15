package com.sap.catenax

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.time.LocalDate
import groovy.time.TimeCategory
import java.util.Date
import java.text.SimpleDateFormat

def Message processData(Message message) {
	JsonSlurper jsonSlurper = new JsonSlurper()
	Reader reader = message.getBody(Reader)
    def messageLog = messageLogFactory.getMessageLog(message)
	def level = message.getHeaders().get("Level")
    def lbnMTId = message.getHeaders().get("LbnMTId")
	def input = new JsonSlurper().parse(reader)
	def eventMap = [:]
	def eventMapByQuantity = [:]
	def quantityMap = [:]
	def aas_required = input.aas
	def mqtt_required = input.mqtt
	def aasUUIDs = input.remove("aasUUIDs")
	def debugEnabled = message.getHeaders().get("DebugEnabled")
	def rootMfId = input.manufacturer
	def maxLevelInfo = [:]
	maxLevelInfo.maxLevel = 1
	setDateInfo(input,maxLevelInfo)
	getQuantity_AAS_Info(input,quantityMap,input.uuids)
	for(def i = 0; i <= maxLevelInfo.maxLevel ; i++) {
		getReceiveEvents(input,i+"",eventMap,rootMfId)
		getProduceEvents(input,i+"",eventMap,rootMfId)
	}
	//getReceiveEvents(input,level,eventMap)
	//getProduceEvents(input,level,eventMap)
	getDeliverEvents(input,level,eventMap)

	if(input.aas == "Y") {
		input.remove("aas")
		input.remove("aasUUIDs")
	}
	/*
	def mtJson = new JsonBuilder(eventMap)
		
	if(messageLog != null && debugEnabled == "1"){
		messageLog.addAttachmentAsString("MT JSON at Level "+level+" for Quantity 1", mtJson.toPrettyString(), "text/xml")
	}
	*/
	if(eventMap.isEmpty()) {
		message.setHeader("route", "END")
	}else{
		//createQuantities(eventMap,eventMapByQuantity,quantityMap)
		
		message.setHeader("route", "POST_MT")
        //def eventMapByQuantityV2 = getV2Envolope(eventMapByQuantity,lbnMTId)
		def eventMapV2 = getV2Envolope(eventMap,lbnMTId)
		mtJson = new JsonBuilder(eventMapV2)
		if(messageLog != null && debugEnabled == "1"){
			messageLog.addAttachmentAsString("MT Message for Upload ", mtJson.toPrettyString(), "text/xml")
		}
		/*
		if(aas_required == "Y"){
			def uuids = getAASUUIDSForLevel(eventMapByQuantity,level, aasUUIDs)
			message.setHeader("aasUUIDs", uuids )
		}
		*/
		message.setHeader("aas_productlist", quantityMap.aas_productlist)
		message.setHeader("aas_required", aas_required)
		message.setHeader("mqtt_required", mqtt_required)
		message.setHeader("mqtt_productlist", quantityMap.mqtt_productlist)
	}

	message.setHeader("Content-Type", "application/json" + "; charset=utf-8")
	message.setBody(mtJson.toPrettyString())
	return message
}
    def getV2Envolope(eventMapByQuantity, def lbnMTId ){
        def eventMapByQuantityV2 =[:]
        def postMaterialTraceabilityEventNotification = [:]
        def currentdate = new Date(); 
        postMaterialTraceabilityEventNotification.senderLbnId = lbnMTId
        postMaterialTraceabilityEventNotification.senderSystemId = lbnMTId+"-SYS"
        postMaterialTraceabilityEventNotification.messageId = "MSG-"+currentdate.getTime()
        postMaterialTraceabilityEventNotification.eventPackage = eventMapByQuantity
        eventMapByQuantityV2.PostMaterialTraceabilityEventNotification = postMaterialTraceabilityEventNotification
        return eventMapByQuantityV2
    }
	def getAASUUIDSForLevel( def eventMapByQuantity,def level,def aasUUIDs ){
		def requiredids = eventMapByQuantity.produceSerialNumberEvents.size()
		requiredids =  requiredids * 2
		def uuids = []
		if (level == "0"){
			for( i = 0; i < requiredids; i++){
				uuids << aasUUIDs[i]
			}
		}else{
			for( i = aasUUIDs.size() - 1 ; i >= aasUUIDs.size() - requiredids; i--){
				uuids << aasUUIDs[i]
			}
		}
		return uuids
	}

	def getQuantity_AAS_Info(def inputjson,def quantityMap,def uuidlist) {
		if(inputjson instanceof Map) {
			if(inputjson.productid != null ) {
				def quantityInfo = [:]
				def uuidBySerialNo = [:]
				def uuids = []
				def id = inputjson.productid
				def quantity = inputjson.quantity
				int quantityConunt = 0
				
				quantityInfo.quantity = quantity
				
				if(quantity.isNumber() ) {
					quantityConunt = Integer.valueOf(quantity)
				}
				/*
				println("productid: "+inputjson.productid+" quantityConunt: "+quantityConunt+" uuidlist"+uuidlist)
				if(inputjson.sapApp == "Y") {
					for (int i = 0; i < (quantityConunt - 1); i++) {
						uuids << uuidlist.remove(0)
					}
				}
				*/
				if(inputjson.aas == "Y") {
					def aas_productlist
					if(quantityMap.aas_productlist == null) {
						aas_productlist = []
						quantityMap.aas_productlist = aas_productlist
					}else {
						aas_productlist = quantityMap.aas_productlist
					}
					aas_productlist << id
				}
				if(inputjson.mqtt == "Y") {
					def mqtt_productlist
					if(quantityMap.mqtt_productlist == null) {
						mqtt_productlist = []
						quantityMap.mqtt_productlist = mqtt_productlist
					}else {
						mqtt_productlist = quantityMap.mqtt_productlist
					}
					mqtt_productlist << id
				}
				quantityInfo.uuids = uuids
				quantityInfo.uuidBySerialNo = uuidBySerialNo
				quantityInfo.ratio = inputjson.ratio
				quantityInfo.batchManaged = inputjson.batchManaged
				
				quantityMap.put(id,quantityInfo)
			}
		}
		inputjson.each {
			if(it instanceof Map) {
				getQuantity_AAS_Info(it,quantityMap,uuidlist)
			}else if(it.value instanceof List) {
				getQuantity_AAS_Info(it.value,quantityMap,uuidlist)
			}
		}
	}


	def createQuantities(def eventMap,eventMapByQuantity,quantityMap) {
		eventMap.each{
			if(it.value instanceof List ) {
				def eventList = it.value
				if(it.key == "produceSerialNumberEvents") {
					eventList.each { setProduceSerialNumberEventsForQuantity (it,eventMapByQuantity,quantityMap)  }
				}else if(it.key == "deliverSerialNumberEvents") {
					eventList.each { setDeliverSerialNumberEventsForQuantity (it,eventMapByQuantity,quantityMap)  }
				}else if(it.key == "receiveSerialNumberEvents") {
					eventList.each { setReceiveSerialNumberEventsForQuantity (it,eventMapByQuantity,quantityMap)  }
				}else {
					eventMapByQuantity.putAt(it.key, it.value)
				}
			}
		}
	}

	def setProduceSerialNumberEventsForQuantity (def inputEvent,def eventMapByQuantity, def quantityMap ){
		def quantityInfo = quantityMap.get(inputEvent.productId)
		def eventArray
		def int quantity =  0
		def int subComponentCount = 0
		def subComponentCountByProduct =[:]
		if(eventMapByQuantity.produceSerialNumberEvents == null ) {
			eventArray = []
			eventMapByQuantity.produceSerialNumberEvents = eventArray
		}else {
			eventArray = eventMapByQuantity.produceSerialNumberEvents
		}
		if (quantityInfo != null && quantityInfo.quantity.isInteger()) {
			quantity = quantityInfo.quantity as Integer
		}

		for (int i = 0; i < quantity; i++) {
			def eventCopy = deepClone(inputEvent)
			def newSerialId
			if(i == 0) {
				newSerialId = inputEvent.serialNumbers[0].serialId
				eventCopy.properties[0].value = inputEvent.properties[0].value
				def ctXPart =[:]
				ctXPart.name = "CATENA_X_PART"
				ctXPart.value = eventCopy.properties[0].value
				eventCopy.properties << ctXPart
			}else {
				newSerialId = inputEvent.serialNumbers[0].serialId+"-"+i
				//eventCopy.properties[0].value = quantityInfo.uuids[0]
                def uid = UUID.randomUUID().toString()
                eventCopy.properties[0].value = uid
				def ctXPart =[:]
				ctXPart.name = "CATENA_X_PART"
				ctXPart.value = eventCopy.properties[0].value
				eventCopy.properties << ctXPart
				quantityInfo.uuidBySerialNo.put(newSerialId, uid)
			}
			eventCopy.serialNumbers[0].serialId = newSerialId
			def int ratio =  0
			def newComponentSerialNumbers = []
			for(item in inputEvent.serialNumbers[0].componentSerialNumbers) {
				def rationInfo = quantityMap.get(item.productId)
				if (rationInfo != null && rationInfo.ratio.isInteger()){
					ratio = rationInfo.ratio as Integer
				}
				def subComponentKey =  item.productId+"_"+item.serialId
				if(subComponentCountByProduct.get(subComponentKey) != null) {
					subComponentCount = subComponentCountByProduct.get(item.productId) as Integer
				}else {
					subComponentCount =  0
				}
				if(ratio > 0) {
					for (int j = 0; j < ratio; j++) {
						def compCopy = deepClone(item)
						if(j == 0 && subComponentCount == 0) {
							compCopy.serialId = item.serialId
							subComponentCount++
						}else {
							compCopy.serialId = item.serialId+"-"+subComponentCount
							subComponentCount++
						}
						newComponentSerialNumbers << compCopy
					}
					subComponentCountByProduct.put(subComponentKey,subComponentCount)
				}
			}
			eventCopy.serialNumbers[0].componentSerialNumbers = newComponentSerialNumbers

			eventArray << eventCopy
		}
	}

	def setDeliverSerialNumberEventsForQuantity (def inputEvent,def eventMapByQuantity, def quantityMap ){
		def quantityInfo = quantityMap.get(inputEvent.productId)
		def eventArray
		def int quantity =  0
		def int subComponentCount = 0
		if(eventMapByQuantity.deliverSerialNumberEvents == null ) {
			eventArray = []
			eventMapByQuantity.deliverSerialNumberEvents = eventArray
		}else {
			eventArray = eventMapByQuantity.deliverSerialNumberEvents
		}
		if (quantityInfo != null &&  quantityInfo.quantity.isInteger()) {
			quantity = quantityInfo.quantity as Integer
		}
		for (int i = 0; i < quantity; i++) {
			def eventCopy = deepClone(inputEvent)
			if(i == 0) {
				eventCopy.serialNumbers[0].serialId = inputEvent.serialNumbers[0].serialId
				eventCopy.vendorDeliveryId = inputEvent.vendorDeliveryId
				eventCopy.purchaseOrderId = inputEvent.purchaseOrderId
			}else {
				eventCopy.serialNumbers[0].serialId = inputEvent.serialNumbers[0].serialId+"-"+i
				eventCopy.vendorDeliveryId = inputEvent.vendorDeliveryId+"-"+i
				eventCopy.purchaseOrderId = inputEvent.purchaseOrderId+"-"+i
			}
			eventArray << eventCopy
		}
	}

	def setReceiveSerialNumberEventsForQuantity (def inputEvent,def eventMapByQuantity, def quantityMap ){

		def quantityInfo = quantityMap.get(inputEvent.productId)
		def eventArray = []
		def int quantity =  0
		def int subComponentCount = 0
		if(eventMapByQuantity.receiveSerialNumberEvents == null ) {
			eventArray = []
			eventMapByQuantity.receiveSerialNumberEvents = eventArray
		}else {
			eventArray = eventMapByQuantity.receiveSerialNumberEvents
		}
		if (quantityInfo != null &&  quantityInfo.quantity.isInteger()) {
			quantity = quantityInfo.quantity as Integer
		}
		for (int i = 0; i < quantity; i++) {
			def eventCopy = deepClone(inputEvent)
			def newSerialId

			if(i == 0) {
				newSerialId = inputEvent.serialNumbers[0].serialId
				eventCopy.properties[0].value = inputEvent.properties[0].value
				def ctXPart =[:]
				ctXPart.name = "CATENA_X_PART"
				ctXPart.value = eventCopy.properties[0].value
				eventCopy.properties << ctXPart
				eventCopy.vendorDeliveryId = inputEvent.vendorDeliveryId
				eventCopy.purchaseOrderId = inputEvent.purchaseOrderId
			}else {
				newSerialId = inputEvent.serialNumbers[0].serialId+"-"+i
				eventCopy.vendorDeliveryId = inputEvent.vendorDeliveryId+"-"+i
				eventCopy.purchaseOrderId = inputEvent.purchaseOrderId+"-"+i
				if(quantityInfo.uuidBySerialNo.get(newSerialId) != null ) {
					eventCopy.properties[0].value = quantityInfo.uuidBySerialNo.get(newSerialId)
				    def ctXPart =[:]
				    ctXPart.name = "CATENA_X_PART"
				    ctXPart.value = eventCopy.properties[0].value
				    eventCopy.properties << ctXPart
				}else {
					//eventCopy.properties[0].value = quantityInfo.uuids.remove(0)
                    def uid = UUID.randomUUID().toString()
                    eventCopy.properties[0].value = uid
				    def ctXPart =[:]
				    ctXPart.name = "CATENA_X_PART"
				    ctXPart.value = eventCopy.properties[0].value
				    eventCopy.properties << ctXPart
				}
			}
			eventCopy.serialNumbers[0].serialId = newSerialId
			eventArray << eventCopy
		}
	}


	
	def deepClone(def input) {
		def json = new JsonBuilder(input)
		JsonSlurper jsonSlurper = new JsonSlurper()
		def output = jsonSlurper.parseText(json.toString())
		return output
	}

	def getReceiveEvents(def inputjson,def level, def receiveEventMap ,def rootMfId) {
		if(inputjson instanceof Map) {
			if(inputjson.level == level ) {
				getReceiveEventData(inputjson,level,receiveEventMap, rootMfId)
			}
		}
		inputjson.each {
			if(it instanceof Map) {
				getReceiveEvents(it,level,receiveEventMap, rootMfId)
			}else if(it.value instanceof List) {
				getReceiveEvents(it.value,level,receiveEventMap, rootMfId)
			}
		}
		
	}
	
	def getReceiveEventData(inputjson,level,receiveEventMap ,def rootMfId) {
		def companyId = inputjson.manufacturer
		inputjson.each {
			if(it.value instanceof List &&  it.key != "properties") {
				def subcomponentsList = it.value
				subcomponentsList.each { 
					if (it.manufacturer != rootMfId) { 
						setReceiveData (it,receiveEventMap,companyId)
					}  
				}
			}

		}
	}
	def setReceiveData(inputMap,receiveEventMap,companyId) {
		def eventData = [:]
		eventData.productId = inputMap.productid
		eventData.systemId = companyId+"-SYS"
		eventData.status = inputMap.status
		eventData.plant = inputMap.plant
		eventData.properties = inputMap.properties
		eventData.quantities = getMTQuantityInfo(inputMap,inputMap.batchManaged)
		def revCreationDate = getDaysAfterDate(inputMap.creationDate,5)
		eventData.creationDate = getFormatedDate(revCreationDate)
		def PODate = getDaysAfterDate(inputMap.creationDate,-10)
		if(inputMap.batchManaged == "N") {
			def serialMap = [:]
			def serialMapArray = []
			serialMap.serialId = inputMap.serialId
			serialMapArray << serialMap
			eventData.put("serialNumbers",serialMapArray)
			def _serialId = inputMap.serialId
			if(_serialId.indexOf ("SN") > -1) {
				eventData.vendorDeliveryId = _serialId.replace("SN","DL")
				eventData.purchaseOrderId = _serialId.replace("SN","PO")
			}else {
				eventData.vendorDeliveryId = "DL-"+_serialId
				eventData.purchaseOrderId = "PO-"+_serialId
			}
			eventData.productDetail = getproductDetailInfo(inputMap)
			eventData.vendorCountry = "DE"
			eventData.vendorPostalCode ="69190"
			eventData.purchaseOrderItem = "10"
			eventData.purchaseOrderDate = getFormatedDate(PODate)
			def receiveSerialNumberEventsArray = receiveEventMap.get("receiveSerialNumberEvents")
			if(receiveSerialNumberEventsArray == null) {
				receiveSerialNumberEventsArray = []
				receiveEventMap.put("receiveSerialNumberEvents", receiveSerialNumberEventsArray)
			}
			receiveSerialNumberEventsArray << eventData
		}else {
			def deliveryItemKeysArray = []
			def deliveryItemKeysMap = [:]
			def _batchId = inputMap.batchId
			if(_batchId.indexOf ("BN") > -1) {
				deliveryItemKeysMap.vendorDeliveryId = _batchId.replace("BN","DL")
				deliveryItemKeysMap.purchaseOrderId = _batchId.replace("BN","PO")
				eventData.batchId = _batchId.replace("BN","RCV")
			}else {
				deliveryItemKeysMap.vendorDeliveryId = "DL-"+_batchId
				deliveryItemKeysMap.purchaseOrderId = "PO-"+_batchId
				eventData.batchId = "RCV-"+_batchId
			}
			deliveryItemKeysMap.vendorCountry = "DE"
			deliveryItemKeysMap.vendorPostalCode ="69190"
			deliveryItemKeysMap.purchaseOrderItem = "10"
			deliveryItemKeysMap.vendorBatchId = inputMap.batchId
			deliveryItemKeysMap.purchaseOrderDate = getFormatedDate(PODate)
			deliveryItemKeysArray << deliveryItemKeysMap
			eventData.put("deliveryItemKeys", deliveryItemKeysArray)
			def receiveEventsArray = receiveEventMap.get("receiveEvents")
			if(receiveEventsArray == null) {
				receiveEventsArray = []
				receiveEventMap.put("receiveEvents", receiveEventsArray)
			}
			eventData.productName = inputMap.productName
			receiveEventsArray << eventData
		}
		return eventData
	}
	def generateDeveliveryEvent(def input,def level, def eventMap ) {
		
		def deliverEventMap = [:]
		def serialMapArray = []
		def evenMapArray = []

		getDeliverEvents(input,level,deliverEventMap)
		deliverEventMap.each{
			if(it.value.get("deliverSerialNumberEvents")!= null ) {
				it.value.get("deliverSerialNumberEvents").serialNumbers = it.value.get("serialNumbers")
				serialMapArray << it.value.get("deliverSerialNumberEvents")
			}else if (it.value.get("deliverEvents") != null) {
				evenMapArray << it.value.get("deliverEvents")
			}
		}
		eventMap.deliverSerialNumberEvents = serialMapArray
		eventMap.deliverEvents = evenMapArray

	}
	def setDateInfo(def inputjson,maxLevelInfo) {
		if(inputjson instanceof Map) {
			setDatesForMap(inputjson)
			if(inputjson.level != null && inputjson.level.isNumber() ) {
				def level = Integer.valueOf(inputjson.level)
				//println("level: "+level+" maxLevel:"+maxLevelInfo.maxLevel)
				if(level > maxLevelInfo.maxLevel ) {
					maxLevelInfo.maxLevel = level
				}
			}
		}
		inputjson.each {
			if(it instanceof Map) {
				setDateInfo(it,maxLevelInfo)
			}else if(it.value instanceof List) {
				setDateInfo(it.value,maxLevelInfo)
			}
		}
	}
	def getDeliverEvents(def inputjson,def level, def deliverEventMap) {
		if(inputjson instanceof Map) {
			setDeliverEventData(inputjson,level,deliverEventMap)
		}
		inputjson.each {
			if(it instanceof Map) {
				getDeliverEvents(it,level,deliverEventMap)
			}else if(it.value instanceof List) {
				getDeliverEvents(it.value,level,deliverEventMap)
			}
		}
	}
	def setDeliverEventData(inputMap,level,deliverEventMap) {
		if(inputMap.containsKey("level")) {
			def currentlevel = inputMap.level
			def sapApp = inputMap.sapApp
			if(level == currentlevel && sapApp =="Y") {
				if(inputMap.batchManaged == "N"){
					if(deliverEventMap.deliverSerialNumberEvents == null) {
						def serialEvenMapArray = []
						deliverEventMap.put("deliverSerialNumberEvents",serialEvenMapArray <<setDeliveryData(inputMap))
					}else {
						deliverEventMap.deliverSerialNumberEvents << setDeliveryData(inputMap)
					}
				}else{
					if(deliverEventMap.deliverEvents == null) {
						def evenMapArray = []
						deliverEventMap.put("deliverEvents",evenMapArray <<setDeliveryData(inputMap))
					}else {
						deliverEventMap.deliverEvents << setDeliveryData(inputMap)
					}
						
				}
			}
		}
	}

	def setDeliveryData(inputMap) {
		def eventData = [:]
		if(inputMap.batchManaged == "N") {
			def serialMap = [:]
			def serialMapArray = []
			serialMap.serialId = inputMap.serialId
			serialMapArray << serialMap
			eventData.put("serialNumbers",serialMapArray)
			def _serialId = inputMap.serialId
			if(_serialId.indexOf ("SN") > -1) {
				eventData.vendorDeliveryId = _serialId.replace("SN","DL")
				eventData.purchaseOrderId = _serialId.replace("SN","PO")
			}else {
				eventData.vendorDeliveryId = "DL-"+_serialId
				eventData.purchaseOrderId = "PO-"+_serialId
			}
		}else {
			def _batchId = inputMap.batchId
			if(_batchId.indexOf ("BN") > -1) {
				eventData.vendorDeliveryId = _batchId.replace("BN","DL")
				eventData.purchaseOrderId = _batchId.replace("BN","PO")
			}else {
				eventData.vendorDeliveryId = "DL-"+_batchId
				eventData.purchaseOrderId = "PO-"+_batchId
			}
			//eventData.vendorDeliveryId = inputMap.batchId.replace("BN","DL")
			//eventData.purchaseOrderId = inputMap.batchId.replace("BN","PO")
			eventData.vendorBatchId = inputMap.batchId
		}
		eventData.vendorCountry = "DE"
		eventData.vendorPostalCode ="69190"
		
		eventData.purchaseOrderItem = "10"
		def PODate = getDaysAfterDate(inputMap.creationDate,-10)
		eventData.purchaseOrderDate = getFormatedDate(PODate)
		eventData.productId = inputMap.productid
		eventData.systemId = inputMap.manufacturer+"-SYS"
		eventData.status = inputMap.status
		eventData.plant = inputMap.plant
		return eventData
	}
	
	def getProduceEvents(def inputjson,def level, def produceEventMap,def rootMfId) {
		if(inputjson instanceof Map) {
			setProduceEventData(inputjson,level,produceEventMap, rootMfId)
		}
		inputjson.each {
			if(it instanceof Map) {
				getProduceEvents(it,level,produceEventMap, rootMfId)
			}else if(it.value instanceof List) {
				getProduceEvents(it.value,level,produceEventMap, rootMfId)
			}
		}
	}
	def setProduceEventData(inputMap,level,produceEventMap,def rootMfId) {
		if(inputMap.containsKey("level")) {
			def currentlevel = inputMap.level
			def sapApp = inputMap.sapApp
			def manufacturer = inputMap.manufacturer
			def tmpMap = [:]
			if(level == currentlevel && sapApp == "Y" && manufacturer == rootMfId) {
				def batchManaged = inputMap.batchManaged
				tmpMap.productId = inputMap.productid
				tmpMap.systemId = inputMap.manufacturer+"-SYS"
				tmpMap.properties = inputMap.properties
				tmpMap.creationDate =  getFormatedDate(inputMap.creationDate)
				tmpMap.location = inputMap.countryCode
				tmpMap.status =  inputMap.status
				tmpMap.quantities = getMTQuantityInfo(inputMap,batchManaged)
				tmpMap.plant = inputMap.plant
				if(batchManaged == "N") {
					def serialMap = [:]
					def serialMapArray = []
					serialMap.serialId = inputMap.serialId
					serialMapArray << serialMap
					tmpMap.serialNumbers = serialMapArray
					tmpMap.productDetail = getproductDetailInfo(inputMap)
					def subComponentMap = getComponentSerialNumbers(inputMap)
					if(subComponentMap.componentSerialNumbers != null) {
						serialMap.componentSerialNumbers = subComponentMap.componentSerialNumbers
					}
					if(subComponentMap.components != null ) {
						tmpMap.components = subComponentMap.components
					}
					if(produceEventMap.containsKey("produceSerialNumberEvents")) {
						produceEventMap.get("produceSerialNumberEvents") << tmpMap
					}else {
						def produceSerialNumberEventsArray = []
						produceSerialNumberEventsArray << tmpMap
						produceEventMap.put("produceSerialNumberEvents",produceSerialNumberEventsArray)
					}
				}else {
					tmpMap.batchId  = inputMap.batchId
					tmpMap.productName  = inputMap.productName
					
					def subComponentMap = getComponentSerialNumbers(inputMap)
					if(subComponentMap.components != null ) {
						tmpMap.components = subComponentMap.components
					}

					if(produceEventMap.containsKey("produceEvents")) {
						produceEventMap.get("produceEvents") << tmpMap
					}else {
						def produceEventsArray = []
						produceEventsArray << tmpMap
						produceEventMap.put("produceEvents",produceEventsArray)
					}
				}
			}
		}
	}

	def getComponentSerialNumbers(inputjson) {
		def subComponents = [:]
		def componentSerialNumbersArray = []
		def componentArray = []
		def companyId = inputjson.manufacturer
		inputjson.each {
			if(it.value instanceof List &&  it.key != "properties") {
				def subcomponentsList = it.value
				subcomponentsList.each {
					if(it.batchManaged == "N") {
						def componentSerialNumbersMap = [:]
						componentSerialNumbersMap.serialId = it.serialId
						componentSerialNumbersMap.productId = it.productid
						componentSerialNumbersMap.systemId = companyId+"-SYS"
						componentSerialNumbersArray << componentSerialNumbersMap
					}else {
						def componentMap = [:]
						def _batchId = it.batchId
						if(_batchId.indexOf ("BN") > -1) {
							componentMap.batchId = _batchId.replace("BN","RCV")
						}else{
							componentMap.batchId = "RCV-"+_batchId
						}
						componentMap.productId = it.productid
						componentMap.systemId = companyId+"-SYS"
						componentArray << componentMap
					}
				}
			}
		}
		subComponents.componentSerialNumbers = componentSerialNumbersArray
		subComponents.components = componentArray
		return subComponents
	}

	def getMTQuantityInfo(def inputMap, def batchManaged){
		def quantityArray = []
		if( batchManaged == 'Y'){
			def batchQuantityMap = [:]
			def qualifier 
			if(inputMap.qualifier == null || inputMap.qualifier ==""){
				qualifier = "BASE_UNIT"
			}else {
				qualifier = inputMap.qualifier
			}
			batchQuantityMap.qualifier = qualifier
			batchQuantityMap.value = inputMap.batchQuantity
			batchQuantityMap.unit = inputMap.unitOfMeasure
			quantityArray << batchQuantityMap
		}else{
			def quantityMap = [:]
			quantityMap.qualifier = "DISCRETE"
			quantityMap.value = 1
			quantityMap.unit = "PC"
			quantityArray << quantityMap
		}

		return quantityArray
	}
	def getproductDetailInfo(def inputMap) {
		def productDetail = [:]
		def quantities = [["qualifier": "DISCRETE","value": 1,"unit": "PC"]]
		productDetail.put("quantities", quantities)
		productDetail.put("properties", [])
		productDetail.put("productName", inputMap.productName)
		productDetail.put("creationDate", getFormatedDate(inputMap.creationDate))
		return productDetail
	}
	def setDatesForMap(def inputMap ) {
		if(inputMap.containsKey("level")) {
			def level = inputMap.get("level")
			def daysAfter
			if(level == '0') {
				daysAfter = -1
			}else if(level == '1') {
				daysAfter = -14
			}else if(level == '2') {
				daysAfter = -28
			}else if(level == '3') {
				daysAfter = -35
			}else if(level == '4') {
				daysAfter = -42
			}
			def dateVal
			dateVal = getDaysAfterDate(dateVal,daysAfter)
			inputMap.put("creationDate",dateVal)
			dateVal = getDaysAfterDate(dateVal, 365)
			inputMap.put("expirationDate",dateVal)
		}
	}
	def getDaysAfterDate(def currentDate, def daysnumber) {
		def newDate
		if(currentDate == null) {
			currentDate = new Date()
		}
		use (groovy.time.TimeCategory) {
			newDate = currentDate + daysnumber.days
		}
		return newDate
	}
	
	def getFormatedDate(def inDate) {
		if(inDate == null ) return ""
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
		return sdf.format(inDate.getTime())
	}
	
	def printInfo(def inputjson) {
		inputjson.each {
			if(it instanceof Map) {
				printInfo(it)
			}else if(it.value instanceof List) {
				printInfo(it.value)
			}
		}
	}
