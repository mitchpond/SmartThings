/**
 *  Device Tamper Alarm
 *
 *  Author: Mitch Pond, SmartThings
 *  Date: 2013-03-20
 */
definition(
    name: "Device Tamper Alarm",
    namespace: "mitchpond",
    author: "Mitch Pond",
    description: "Receive notification when a device is tampered with. Currently supports Quirky Tripper.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
)

preferences {
	section("Choose devices..."){
		input "contact", "capability.contactSensor", title: "Devices supporting tamper", required: false, multiple: true
	}
	section("Via a push notification and/or an SMS message"){
		input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
		input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes","No"]
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(contact, "tamper.tampered", eventHandler)
}

def eventHandler(evt) {
	sendMessage(evt)
}

private sendMessage(evt) {
	def msg = evt.descriptionText
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (!phone || pushAndPhone != "No") {
		log.debug "sending push"
		sendPush(msg)
	}
	if (phone) {
		log.debug "sending SMS"
		sendSms(phone, msg)
	}
}
