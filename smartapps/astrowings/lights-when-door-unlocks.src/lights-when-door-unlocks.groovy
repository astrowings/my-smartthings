/**
 *  Lights when door unlocks
 *
 *  Copyright © 2016 Phil Maynard
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
 *
 *	VERSION HISTORY                                    */
 	 def versionNum() {	return "version 1.03" }       /*
 
 *	 v1.03 (01-Nov-2016): standardize section headers
 *   v1.02 (26-Oct-2016): added trace for each event handler
 *   v1.01 (26-Oct-2016): added 'About' section in preferences
 *   v1.00 (2016 date unknown): working version, no version tracking up to this point
 *
*/
definition(
    name: "Lights when door unlocks",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Turn a light on when a door is unlocked from outside.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@3x.png")


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	section("About") {
    	paragraph title: "This SmartApp turns a light on when a door is unlocked from outside (i.e. using the keypad). " +
        	"Can be used to light-up an entrance for example.",
        	"version 1.02"
    }
    section("When this door is unlocked using the keypad") {
        input "theLock", "capability.lock", required: true, title: "Which lock?"
    }
    section("Turn on this light") {
        input "theSwitch", "capability.switch", required: true, title: "Which light?"
        input "leaveOn", "number", title: "For how long (minutes)?"
    }
    section("Set the conditions") {
        input "whenDark", "bool", title: "Only after sunset?", required: false, defaultValue: true
    }
}


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

private C_1() { return "this is constant1" }


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***



//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	log.info "installed with settings: $settings"
    initialize()
}

def updated() {
    log.info "updated with settings $settings"
	unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
	switchOff()
    log.info "uninstalled"
}

def initialize() {
	log.info "initializing"
    state.debugLevel = 0
    subscribe(theLock, "lock.unlocked", unlockHandler)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def locationPositionChange(evt) {
	log.trace "locationPositionChange>${evt.descriptionText}"
	initialize()
}

def unlockHandler(evt) {
	log.trace "unlockHandler>${evt.descriptionText}"
    if (allOk) {
    	def unlockText = evt.descriptionText
        if (unlockText.contains("was unlocked with code")) {
            log.debug "${unlockText}; turning the light on"
            switchOn()
            log.debug "scheduling the light to turn off in ${leaveOn} minutes"
            runIn(leaveOn * 60, switchOff)
        } else {
        	log.debug "door wasn't unlocked using the keypad; doing nothing"
        }
    } else {
    	//log.debug "conditions not met; doing nothing" //TODO: why?
    }
}


//   -------------------
//   ***   METHODS   ***

def switchOn() {
	theSwitch.on()
}

def switchOff() {
	log.debug "turning the light off"
	theSwitch.off()
}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getAllOk() {
	def result = theSwitch.currentSwitch == "off" && darkOk
    log.debug "allOk :: ${result}"
    return result
}

/*def getModeOk() {
	def result = !theModes || theModes.contains(location.mode)
	log.debug "modeOk :: $result"
	return result
}*/

def getDarkOk() {
	def result = !whenDark || itsDarkOut
	//log.debug "darkOk :: $result"
	return result
}

def getItsDarkOut() {
    def sunTime = getSunriseAndSunset(sunsetOffset: "00:30")
    def currentDTG = new Date()
    def result = false

	if(sunTime.sunrise < currentDTG && sunTime.sunset > currentDTG){
    	log.debug "it's daytime"
        result = false
    } else {
    	log.debug "it's nighttime"
        result = true
    }
    return result
}


//   ------------------------
//   ***   COMMON UTILS   ***
