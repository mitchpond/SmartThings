/*************************************************************************************
 *  Hue Party Mode
 *
 *  Author: Mitch Pond
 *  Date: 2015-05-29

Copyright (c) 2015, Mitchell Pond
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*************************************************************************************/
 
definition(
    name: "Hue Party Mode",
    namespace: "mitchpond",
    author: "Mitch Pond",
    description: "Change the color of your lights randomly at an interval of your choosing.",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-ItsPartyTime.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-ItsPartyTime@2x.png",
    )

preferences {
	section("Choose lights..."){
		input "lights", "capability.colorControl", title: "Pick your lights", required: false, multiple: true
	}
	section("Set an interval between color changes"){
		input "interval", "number", title: "Color change interval (default 10)", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	updated()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    state.running = false
    unsubscribe()
    subscribe(app, onAppTouch)
    subscribeToCommand(lights, "off", onLightOff)
}

def onLightOff(evt) {
    //if one of the lights in our device list is turned off, and we are running, unschedule any pending color changes
    if (state.running) {
        log.info("${app.name}: One of our lights was turned off. Stopping execution...")
        unschedule()
        state.running = false
    }
}

def onAppTouch(evt) {
	//if currently running, unschedule any scheduled function calls
    //if not running, start our scheduling loop
    
	if (state.running) {
    	log.debug("${app.name} is running. Stopping execution...")
    	unschedule()
        state.running = false
    }
    else if (!state.running) {
    	log.debug("${app.name} is not running. Beginning execution...")
        lights*.on()
    	changeColor()
        state.running = true
    }
	
}

def changeColor() {
	//calculate a random color, send the setColor command, then schedule our next execution
    log.info("${app.name}: Running scheduled color change")
    def nextHue = new Random().nextInt(101)
    def nextSat = new Random().nextInt(101)
    //def nextColor = Integer.toHexString(new Random().nextInt(0x1000000))
    log.debug nextColor
    lights*.setColor(hue: nextHue, saturation: nextSat)
    runIn(settings.interval, changeColor)
}
