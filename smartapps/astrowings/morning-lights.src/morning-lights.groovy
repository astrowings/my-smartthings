/**
 *  Morning Lights
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
 *   v1 (29-Oct-2016): inital version base code adapted from 'Sunset Lights - v2'
 *
*/
definition(
    name: "Morning Lights",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Turn on selected lights in the morning and turn them off automatically at sunrise.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn@3x.png")


//   -----------------------------------
//   ***   SETTING THE PREFERENCES   ***

preferences {
	page(name: "page1", title: "Morning Lights - Turn ON", nextPage: "page2", uninstall: true) {
        section("About") {
        	paragraph title: "This SmartApp turns on selected lights at a specified time and turns them off at sunrise. " +
            	"Different turn-on times can be configured for each day of the week, and they can be " +
                "randomized within a specified window to simulate manual activation.",
				"version 1"
        }
        section("Choose the lights to turn on") {
            input "theLights", "capability.switch", title: "Lights", multiple: true, required: true
        }
    	section("Set a different time to turn on the lights on each day " +
        	"(optional - lights will turn on at the default time if not set)") {
            input "weekdayOn", "time", title: "Mon-Fri", required: false
            input "saturdayOn", "time", title: "Saturday", required: false
        	input "sundayOn", "time", title: "Sunday", required: false
        }
    	section("Turn the lights on at this time if no weekday time is set " +
        	"(optional - this setting used only if no weekday time is specified; " +
            "lights activation is disabled otherwise)") {
        	input "defaultOn", "time", title: "Default time?", required: false
        }
        //TODO: add option to specify turn-off time
    }
	page(name: "page2", title: "Random Factor", install: true) {
    	section("Optionally, specify a window around the scheduled time when the lights will turn on/off " +
        	"(e.g. a 30-minute window would have the lights switch sometime between " +
            "15 minutes before and 15 minutes after the scheduled time.)") {
            input "randOn", "number", title: "Random ON window (minutes)?", required: false
            input "randOff", "number", title: "Random OFF window (minutes)?", required: false
        }
        section("The settings above are used to randomize preset times such that lights will " +
        	"turn on/off at slightly different times from one day to another, but if multiples lights " +
            "are selected, they will still switch status at the same time. Use the options below " +
            "to insert a random delay between the switching of each individual light. " +
            "This option can be used independently of the ones above.") {
            input "onDelay", "bool", title: "Delay switch-on?", required: false
            input "offDelay", "bool", title: "Delay switch-off?", required: false
            input "delaySeconds", "number", title: "Delay switching by up to (seconds)?", required: true, defaultValue: 15
        }
    }
}


//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	log.info "installed with settings $settings"
	initialize()
}

def updated() {
    log.info "updated with settings $settings"
	unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    log.info "uninstalled"
}

def initialize() {
	log.info "initializing"
    subscribe(location, "sunriseTime", sunriseTimeHandler)	//triggers at sunrise, evt.value is the sunrise String (time for next day's sunrise)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes

	//schedule it to run today too
	schedTurnOff(location.currentValue("sunriseTime"))
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def sunriseTimeHandler(evt) {
    log.trace "sunriseTimeHandler>${evt.descriptionText}"
    def sunriseTimeHandlerMsg = "triggered sunriseTimeHandler; next sunrise will be ${evt.value}"
    log.debug sunriseTimeHandlerMsg
    schedTurnOff(evt.value)
}    

def locationPositionChange(evt) {
	log.trace "locationChange>${evt.descriptionText}"
	initialize()
}


//   -------------------
//   ***   METHODS   ***

def schedTurnOff(sunriseString) {
	log.trace "schedTurnOff(sunriseString: ${sunriseString})"
	
    def datTurnOff = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
    log.debug "sunrise date: ${datTurnOff}"

    //apply random factor
    if (randOff) {
        def random = new Random()
        def randOffset = random.nextInt(randOff)
        datTurnOff = new Date(datTurnOff.time - (randOff * 30000) + (randOffset * 60000))
	}
    
	log.info "scheduling lights OFF for: ${datTurnOff}"
    // This method gets called at sunrise to schedule next day's turn-off. Because of the random factor,
    // today's turn-off could actually be scheduled after sunrise (therefore after this method gets called),
    // so we use [overwrite: false] to prevent today's scheduled turn-off from being overwriten.
    runOnce(datTurnOff, turnOff, [overwrite: false])
    schedTurnOn(datTurnOff)
}

def schedTurnOn(datTurnOff) {
	//fires at sunrise to schedule next day's turn-on
    log.trace "schedTurnOn()"
    
    def DOW_TurnOn = DOWTurnOnTime
    def default_TurnOn = defaultTurnOnTime
    def datTurnOn
    
	if (!DOW_TurnOn && !default_TurnOn) {
    	log.info "user didn't specify turn-on time; tomorrow's scheduling cancelled"
    } else {
        //select which turn-on time to use (1st priority: weekday-specific, 2nd: default, else: no turn-on)
        def useTime
        if (DOW_TurnOn) {
            datTurnOn = DOW_TurnOn
            useTime = "using the weekday turn-on time"
        } else if (default_TurnOn) {
            datTurnOn = default_TurnOn
            useTime = "using the default turn-on time"
    	}
        
        //check that turn-on is scheduled earlier than turn-off by at least 10 minutes
        def safeOff = datTurnOff.time - (10 * 60 * 1000) //subtract 10 minutes from scheduled turn-off time
        if (datTurnOn.time < safeOff) {
            log.info "scheduling lights ON for: ${datTurnOn} (${useTime})"
            runOnce(datTurnOn, turnOn)
        } else {
        	log.info "tomorrow's turn-on time (${datTurnOn}) " +
            	"would be later than (or less than 10 minutes before) " +
                "the scheduled turn-off time (${datTurnOff});  " +
                "scheduling cancelled"
        }
    }
}

def turnOn() {
    log.info "turning lights on"
    def newDelay = 0L
    def delayMS = (onDelay && delaySeconds) ? delaySeconds * 1000 : 5 //ensure positive number for delayMS
    def random = new Random()
    theLights.each { theLight ->
        if (theLight.currentSwitch != "on") {
            log.info "turning on the ${theLight.label} in ${convertToHMS(newDelay)}"
            theLight.on(delay: newDelay)
            newDelay += random.nextInt(delayMS) //calculate random delay before turning on next light
        } else {
            log.info "the ${theLight.label} is already on; doing nothing"
        }
    }
}

def turnOff() {
    log.info "turning lights off"
    def newDelay = 0L
    def delayMS = (offDelay && delaySeconds) ? delaySeconds * 1000 : 5 //ensure positive number for delayMS
    def random = new Random()
    theLights.each { theLight ->
        if (theLight.currentSwitch != "off") {
            log.info "turning off the ${theLight.label} in ${convertToHMS(newDelay)}"
            theLight.off(delay: newDelay)
            newDelay += random.nextInt(delayMS) //calculate random delay before turning off next light
        } else {
            log.info "the ${theLight.label} is already off; doing nothing"
        }
    }
}


//   ----------------
//   ***   UTILS  ***

def convertToHMS(ms) {
    int hours = Math.floor(ms/1000/60/60)
    int minutes = Math.floor((ms/1000/60) - (hours * 60))
    int seconds = Math.floor((ms/1000) - (hours * 60 * 60) - (minutes * 60))
    double millisec = ms-(hours*60*60*1000)-(minutes*60*1000)-(seconds*1000)
    int tenths = (millisec/100).round(0)
    return "${hours}h${minutes}m${seconds}.${tenths}s"
}

def getDefaultTurnOnTime() {
//calculate default turn-on time
//this gets called at sunrise, so when the sun rises on Tuesday, it will
//schedule the lights' turn-on time for Wednesday morning
    if (defaultOn) {
    	//convert preset time to next morning's date
        def timeOn = timeTodayAfter("12:00", defaultOn, location.timeZone)
        
        //apply random factor to turn-on time
        if (randOn) {
	    	def random = new Random()
			def randOffset = random.nextInt(randOn)
            timeOn = new Date(timeOn.time - (randOn * 30000) + (randOffset * 60000))
            log.debug "randomized default turn-on time: $timeOn"
        } else {
        	log.debug "default turn-on time: $timeOn"
        }
        return timeOn
    } else {
        log.debug "default turn-on time not specified"
        return false
	}
}

def getDOWTurnOnTime() {
//calculate weekday-specific turn-on time
//this gets called at sunrise, so when the sun rises on Tuesday, it will
//schedule the lights' turn-on time for Wednesday morning

    def tmrDOW = (new Date() + 1).format("E") //find out tomorrow's day of week

    //find out the preset (if entered) turn-on time for tomorrow
    def DOWtimeOn
    if (saturdayOn && tmrDOW == "Sat") {
        DOWtimeOn = saturdayOn
    } else if (sundayOn && tmrDOW == "Sun") {
        DOWtimeOn = sundayOn
    } else if (weekdayOn && tmrDOW == "Mon") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Tue") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Wed") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Thu") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Fri") {
        DOWtimeOn = weekdayOn
    }

	if (DOWtimeOn) {
    	//convert preset time to tomorrow's date
    	def tmrOn = timeTodayAfter("12:00", DOWtimeOn, location.timeZone)
        
        //apply random factor to turn-on time
		if (randOn) {
        	def random = new Random()
            def randOffset = random.nextInt(randOn)
            tmrOn = new Date(tmrOn.time - (randOn * 30000) + (randOffset * 60000))
            log.debug "randomized DOW turn-on time: $tmrOn"
        } else {
        	log.debug "DOW turn-on time: $tmrOn"
        }
        return tmrOn
    } else {
    	log.debug "DOW turn-on time not specified"
        return false
    }
}