/**
 *  Test
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
 *  VERSION HISTORY
 *
 *   Test only - no version tracking
 *   
 *
*/
definition(
    name: "Test",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Test",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


//   -----------------------------------
//   ***   SETTING THE PREFERENCES   ***

preferences {
	section("Test this device") {
		input "theSwitch", "capability.switch"
	}
}


//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	log.info "installed with settings: $settings"
    initialize()
}

def updated() {
    log.info "updated with settings $settings"
	unsubscribe()
    //unschedule()
    initialize()
}

def uninstalled() {
    log.info "uninstalled"
}

def initialize() {
	log.info "initializing"
	subscribe(theSwitch, "switch", eventHandler)
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

void eventHandler(evt) {
    log.debug "in eventHandler"
    log.trace "eventHandler>data:${evt.data}"
    log.trace "eventHandler>description:${evt.description}"
    log.trace "eventHandler>descriptionText:${evt.descriptionText}"
    log.trace "eventHandler>device:${evt.device}"
    log.trace "eventHandler>displayName:${evt.displayName}"
    log.trace "eventHandler>deviceId:${evt.deviceId}"
    log.trace "eventHandler>name:${evt.name}"
    log.trace "eventHandler>source:${evt.source}"
    log.trace "eventHandler>stringValue:${evt.stringValue}"
    log.trace "eventHandler>unit:${evt.unit}"
    log.trace "eventHandler>value:${evt.value}"
    log.trace "eventHandler>isDigital:${evt.isDigital()}"
    log.trace "eventHandler>isPhysical:${evt.isPhysical()}"
    log.trace "eventHandler>isStateChange:${evt.isStateChange()}"
}


//   -------------------
//   ***   METHODS   ***



//   ----------------
//   ***   UTILS  ***