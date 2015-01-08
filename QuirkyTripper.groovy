/**
 *  Quirky Wink Tripper Contact Sensor
 *
 *  Copyright 2015 Mitch Pond, SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Quirky/Wink Tripper", namespace: "mitchpond", author: "Mitch Pond") {
    
	capability "Contact Sensor"
	capability "Battery"
	capability "Refresh"
	capability "Configuration"
    
	command "configure"
        
	fingerprint endpointId: "01", profileId: "0104", deviceId: "0402", inClusters: "0000,0001,0003,0500,0020,0B05", outClusters: "0003,0019"
	}

	// simulator metadata
	simulator {}

	// UI tile definitions
	tiles {
		standardTile("contact", "device.contact", width: 2, height: 2) {
			state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e")
			state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821")
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main ("contact")
		details(["contact","battery","refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description: $description"

	Map map = [:]
	if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	}
	else if (description?.startsWith('read attr -')) {
		map = parseReportAttributeMessage(description)
	}
	else if (description?.startsWith('zone status')) {
		map = parseIasMessage(description)
	}

	log.debug "Parse returned $map"
	def result = map ? createEvent(map) : null

	if (description?.startsWith('enroll request')) {
		List cmds = enrollResponse()
		log.debug "enroll response: ${cmds}"
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}
	return result
}
/*
def getBattery() {
	//seems to only process this request when it has another report to send.
	//Likely that the sensor is only awake for a short time after open/close
	log.debug "Requesting battery level.."
	"st rattr 0x${device.deviceNetworkId} 1 1 0x20"
}
*/
def refresh() {
	"st rattr 0x${device.deviceNetworkId} 1 1 0x20"
}

def configure() {
	String zigbeeId = swapEndianHex(device.hub.zigbeeId)
	log.debug "Confuguring Reporting, IAS CIE, and Bindings."
    
    	def cmd = [
		"zcl global write 0x500 0x10 0xf0 {${zigbeeId}}", "delay 200",
		"send 0x${device.deviceNetworkId} 1 1", "delay 1500",
	
		"zcl global send-me-a-report 0x500 0x0012 0x19 0 0xFF {}", "delay 200", //get notified on tamper
		"send 0x${device.deviceNetworkId} 1 1", "delay 1500",
		
		"zcl global send-me-a-report 1 0x20 0x20 600 3600 {01}", "delay 200", //battery report request
		"send 0x${device.deviceNetworkId} 1 1", "delay 1500",
	
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x500 {${device.zigbeeId}} {}", "delay 500",
		"zdo bind 0x${device.deviceNetworkId} 1 1 0x0b05 {${device.zigbeeId}} {}", "delay 500",
		"zdo bind 0x${device.deviceNetworkId} 1 1 1 {${device.zigbeeId}} {}"
		]
    	cmd + refresh()
}

def enrollResponse() {
	log.debug "Sending enroll response"
	[	
	"raw 0x500 {01 23 00 00 00}", "delay 200",
	"send 0x${device.deviceNetworkId} 1 1"
	]
}

private Map parseCatchAllMessage(String description) {
 	Map resultMap = [:]
 	def cluster = zigbee.parse(description)
 	if (shouldProcessMessage(cluster)) {
		switch(cluster.clusterId) {
			case 0x0001:
			resultMap = getBatteryResult(cluster.data.last())
			break
            }
        }

	return resultMap
}

private boolean shouldProcessMessage(cluster) {
	// 0x0B is default response indicating message got through
	// 0x07 is bind message
	boolean ignoredMessage = cluster.profileId != 0x0104 || 
		cluster.command == 0x0B ||
		cluster.command == 0x07 ||
		(cluster.data.size() > 0 && cluster.data.first() == 0x3e)
	return !ignoredMessage
}

private Map parseReportAttributeMessage(String description) {
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
	log.debug "Desc Map: $descMap"

	Map resultMap = [:]
	if (descMap.cluster == "0001" && descMap.attrId == "0020") {
		resultMap = getBatteryResult(Integer.parseInt(descMap.value, 16))
	}

	return resultMap
}

private Map parseIasMessage(String description) {
	List parsedMsg = description.split(' ')
	String msgCode = parsedMsg[2]
	int status = Integer.decode(msgCode)

	Map resultMap = [:]
    
	if (status & 0b00000001) {resultMap = getContactResult('open')}
	else if (~status & 0b00000001) resultMap = getContactResult('closed')
    
	//TODO: state updates and maybe tiles for tamper alert and battery alert
    
	if (status & 0b00000100) {log.debug "Tampered"}
	else if (~status & 0b00000100) log.debug "Not tampered"
    
	if (status & 0b00001000) log.debug "Low battery"
	else if (~status & 0b00001000) log.debug "Battery OK"
    
	return resultMap
}

private Map getBatteryResult(rawValue) {
	log.debug 'Battery'
	def linkText = getLinkText(device)

	def result = [
		name: 'battery'
		]

	def volts = rawValue / 10
	def descriptionText
	if (volts > 3.5) {
		result.descriptionText = "${linkText} battery has too much power (${volts} volts)."
	}
	else {
		def minVolts = 2.1
		def maxVolts = 3.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		result.value = Math.min(100, (int) pct * 100)
		result.descriptionText = "${linkText} battery was ${result.value}%"
	}

	return result
}


private Map getContactResult(value) {
	log.debug 'Contact Status'
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} was ${value == 'open' ? 'opened' : 'closed'}"
	return [
		name: 'contact',
		value: value,
		descriptionText: descriptionText
		]
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}

private String swapEndianHex(String hex) {
	reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
	int i = 0;
	int j = array.length - 1;
	byte tmp;
	while (j > i) {
		tmp = array[j];
		array[j] = array[i];
		array[i] = tmp;
		j--;
		i++;
	}
	return array
}
