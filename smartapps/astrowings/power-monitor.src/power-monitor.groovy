/**
 *  Power Monitor
 *
 *  Copyright © 2016 Phil Maynard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0												*/
 	       def urlApache() { return "http://www.apache.org/licenses/LICENSE-2.0" }				/*
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *   --------------------------------
 *   ***   VERSION HISTORY  ***
 *	
 *    v2.01 (22-Nov-2019) - unschedule rechecks only if warnstate exists
 *    v2.00 (20-Nov-2019) - add options to allow power drop/exceedance for a limited time
 *    v1.10 (18-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *    v1.00 (09-Jul-2019) - initial release
 *    v0.10 (03-Jul-2019) - developing
 *
*/

definition(
    name: "Power Monitor",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Notify when power draw from selected device exceeds set thresholds.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@3x.png"
)


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 2.00" }
def		versionDate()			{ return "20-Nov-2019" }     
def		gitAppName()			{ return "power-monitor" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageSettings")
    page(name: "pageAbout")
    page(name: "pageLogOptions")
    page(name: "pageUninstall")
}


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

def pageMain() {
    dynamicPage(name: "pageMain", install: true, uninstall: false) {
    	section(){
        	paragraph "", title: "This SmartApp sends a notification (SMS optional) to notify " +
	        	"when power draw from the selected device exceeds the set thresholds."
        }
        section("Monitor this device") {
            input "thePower", "capability.powerMeter", title: "Which power metering device?", description: "Monitor power draw from this device", required: true, submitOnChange: true
        }
        section("Select power thresholds") {
			if (thePower) {
            	input "loPower", "number", title: "Low threshold", description: "Notify when power draw (watts) drops below this number " +
                	"(low threshold notifications disabled if zero or not set)", required: false
                input "loDuration", "number", title: "Low power time tolerance", description: "Optionally, set the minimum time (minutes) " +
                    "during which the power must remain below the set threshold before a notification will be triggred (e.g. to be notified " +
                    "only when a low power condition lasts for an extended period)", required: false
                input "hiPower", "number", title: "High threshold", description: "Notify when power draw (watts) exceeds this number " +
                	"(high threshold notifications disabled if zero or not set)", required: false
                input "hiDuration", "number", title: "High power time tolerance", description: "Optionally, set the minimum time (minutes) " +
                    "during which the power must remain above the set threshold before a notification will be triggred (e.g. to avoid " +
                    "notifications generated by the initial power spike of an appliance starting up)", required: false
                input "hiSpike", "number", title: "Spike threshold", description: "If desired, set the power draw (watts) above which " +
                	"a notification will immediately be triggered, regardless of how much time it remains at that level (e.g. to warn " +
                    "of an abnormal condition)", required: false //TODO: add option to kill power automatically when power exceeds hiSpike
			}        
        }
        section("Select polling interval") {
			if (thePower) {
            	input "pollInterval", "number", title: "Polling interval", description: "Query the ${thePower.label} for power draw every " +
                	"x minutes (wait for device updates if zero or not set)", required: false
			}        
        }
		section("Select notification method") {
			if (thePower) {
                input "sendPush", "bool", title: "Send push notification on selected events", defaultValue: true, required: false
                input "sendSMS", "bool", title: "Send SMS notification on selected events", defaultValue: false, required: false, submitOnChange: true
                if (sendSMS) {
                	input "numberSMS", "phone", title: "Phone number", required: true
                }
            }
		}
		section() {
			if (thePower) {
            	href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
            }
            href "pageAbout", title: "About", description: "", image: getAppImg("info-icn.png"), required: false
        }
    }
}

def pageSettings() {
	dynamicPage(name: "pageSettings", install: false, uninstall: false) {
        section() {
			label title: "Assign a name", defaultValue: "${app.name}", required: false
            href "pageUninstall", title: "", description: "Uninstall this SmartApp", image: getAppImg("trash-circle-red-512.png"), state: null, required: true
		}
		section("Debugging Tools", hideable: true, hidden: true) {
            input "noAppIcons", "bool", title: "Disable App Icons", description: "Do not display icons in the configuration pages", image: getAppImg("disable_icon.png"), defaultValue: false, required: false, submitOnChange: true
            href "pageLogOptions", title: "IDE Logging Options", description: "Adjust how logs are displayed in the SmartThings IDE", image: getAppImg("office8-icn.png"), required: true, state: "complete"
            paragraph title: "Application info", appInfo()
        }
    }
}

def pageAbout() {
	dynamicPage(name: "pageAbout", title: "About this SmartApp", install: false, uninstall: false) { //with 'install: false', clicking 'Done' goes back to previous page
		section() {
        	href url: readmeLink(), title: app.name, description: "Copyright ©2016 Phil Maynard\n${versionNum()}", image: getAppImg("readme-icn.png")
            href url: urlApache(), title: "License", description: "View Apache license", image: getAppImg("license-icn.png")
		}
    }
}

def pageLogOptions() {
	dynamicPage(name: "pageLogOptions", title: "IDE Logging Options", install: false, uninstall: false) {
        section() {
	        input "debugging", "bool", title: "Enable debugging", description: "Display the logs in the IDE", defaultValue: true, required: false, submitOnChange: true
        }
        if (debugging) {
            section("Select log types to display") {
                input "log#info", "bool", title: "Log info messages", defaultValue: true, required: false
                input "log#trace", "bool", title: "Log trace messages", defaultValue: true, required: false
                input "log#debug", "bool", title: "Log debug messages", defaultValue: false, required: false
                input "log#warn", "bool", title: "Log warning messages", defaultValue: true, required: false
                input "log#error", "bool", title: "Log error messages", defaultValue: true, required: false
			}
            section() {
                input "setMultiLevelLog", "bool", title: "Enable Multi-level Logging", defaultValue: true, required: false,
                    description: "Multi-level logging prefixes log entries with special characters to visually " +
                        "represent the hierarchy of events and facilitate the interpretation of logs in the IDE"
            }
            section() {
                input "maxInfoLogs", "number", title: "Display Log Entries", defaultValue: 5, required: false, range: "0..50"
                    description: "Select the maximum number of most recent log entries to display in the " +
                        "application's 'Debugging Tools' section. Enter '0' to disable."
            }
        }
    }
}

def pageUninstall() {
	dynamicPage(name: "pageUninstall", title: "Uninstall", install: false, uninstall: true) {
		section() {
        	paragraph "CAUTION: You are about to completely remove the SmartApp '${app.name}'. This action is irreversible. If you want to proceed, tap on the 'Remove' button below.",
                required: true, state: null
        }
	}
}


//   ---------------------------------
//   ***   PAGES SUPPORT METHODS   ***

def appInfo() {
	def tz = location.timeZone
    def debugLevel = state.debugLevel
    def warnState = state.warnState
    def installTime = state?.installTime
    def initializeTime = state?.initializeTime
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def pwrCheck = state?.pwrCheck
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += installTime ? "  └ last install date: ${new Date(installTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += initializeTime ? "  └ last initialize date: ${new Date(initializeTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "  └ current mode: ${location.currentMode}\n"
        strInfo += "\n • Last initiated execution:\n"
		strInfo += lastInitiatedExecution ? "  └ name: ${lastInitiatedExecution.name}\n" : ""
        strInfo += lastInitiatedExecution ? "  └ time: ${new Date(lastInitiatedExecution.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += "\n • Last completed execution:\n"
		strInfo += lastCompletedExecution ? "  └ name: ${lastCompletedExecution.name}\n" : ""
        strInfo += lastCompletedExecution ? "  └ time: ${new Date(lastCompletedExecution.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += lastCompletedExecution ? "  └ time to complete: ${lastCompletedExecution.duration}s\n" : ""
        strInfo += "\n • Last power check:\n"
        strInfo += pwrCheck ? "  └ time: ${new Date(pwrCheck.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += pwrCheck ? "  └ power: ${pwrCheck.power} W\n" : ""
        strInfo += "\n • State stored values:\n"
        strInfo += "  └ warningLevel: ${warnState}\n" //add description
        strInfo += "  └ debugLevel: ${debugLevel}\n"
        strInfo += "  └ number of stored log entries: ${numLogs}\n"
        if (numLogs > 0) {
            strInfo += "\n • Last ${numLogs} log messages (most recent on top):\n"
            for (int i = 0; i < numLogs; i++) {
                def datLog = new Date(debugLog[i].time).format('dd MMM HH:mm:ss', tz)
                def msgLog = "${datLog} (${debugLog[i].type}):\n${debugLog[i].msg}"
                strInfo += " ::: ${msgLog}\n"
            }
        }
    return strInfo
}


//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	debug "installed with settings: ${settings}", "trace"
	state.installTime = now()
	initialize()
}

def updated() {
    debug "updated with settings ${settings}", "trace", 0
	unsubscribe()
    initialize()
}

def uninstalled() {
    state.debugLevel = 0
    debug "application uninstalled", "trace"
}

def initialize() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "initialize()"]
    state.debugLevel = 0
    debug "initializing", "trace", 1
    state.initializeTime = now()
    state.warnState = 0  // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
    subscribeToEvents()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    subscribe(location, "position", locationPositionChange) //update settings if the hub location changes
    if (!pollInterval) {
    	debug "polling interval not set; subscribing to power events", "debug"
        subscribe(thePower, "power", powerHandler)
    } else {
    	debug "scheduling checkPower() to run every ${pollInterval} minutes", "debug"
        schedule("0 0/${pollInterval} * * * ?",checkPower)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def locationPositionChange(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "locationPositionChange()"]
    debug "locationPositionChange(${evt.descriptionText})", "warn"
	initialize()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "locationPositionChange()", duration: elapsed]
}

def powerHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "powerHandler()"]
    //debug "powerHandler event description: ${evt.description}", "trace", 1
    //debug "powerHandler event date: ${evt.date}", "debug"
    //debug "powerHandler event device: ${evt.device}", "debug"
    //debug "powerHandler event doubleValue: ${evt.doubleValue}", "debug"
    //debug "powerHandler event floatValue: ${evt.floatValue}", "debug"
    //debug "powerHandler event integerValue: ${evt.integerValue}", "debug"
    //debug "powerHandler event longValue: ${evt.longValue}", "debug"
    //debug "powerHandler event numberValue: ${evt.numberValue}", "debug"
    //debug "powerHandler event numericValue: ${evt.numericValue}", "debug"
    //debug "powerHandler event name: ${evt.name}", "debug"
    //debug "powerHandler event value: ${evt.value}", "debug"
    def handlerPower = evt.numberValue
    //debug "handlerPower: ${handlerPower}(isString: ${(handlerPower instanceof String)})", "debug"
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "powerHandler()", duration: elapsed]
    debug "powerHandler completed in ${elapsed} seconds", "trace", -1
    checkPower(handlerPower)
}


//   -------------------
//   ***   METHODS   ***

def checkPower(w) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "checkPower()"]
    debug "executing checkPower(w: ${w})", "trace", 1
    //debug "argPower: ${argPower}(isString: ${(argPower instanceof String)})", "debug"
    //debug "thePower.currentPower: ${thePower.currentPower} (isString: ${(thePower.currentPower instanceof String)})", "debug"
    //debug "thePower.capabilities: ${thePower.capabilities}", "debug"
    //debug "thePower.supportedAttributes: ${thePower.supportedAttributes}", "debug"
    //debug "thePower.supportedCommands: ${thePower.supportedCommands}", "debug"
    def warnState = state.warnState
    w = w ?: thePower.currentPower
    state.pwrCheck = [time: startTime, power: w]
    debug "power: ${w} watts", "debug"
    if (hiPower && w > hiPower) {
    	debug "power draw of ${w} watts is above the specified high threshold (${loPower} W); calling hiPower()", "debug"
        hiPower(w)
    } else if (loPower && w < loPower) {
    	debug "power draw of ${w} watts is below the specified low threshold (${loPower} W); calling loPower()", "debug"
        loPower(w)
    } else {
    	debug "power draw of ${w} watts is within set limits (${loPower} - ${hiPower} W)", "debug"
    	if (warnState == 11 || warnState == 21) {
	        debug "unschedule rechecks and wait for next trigger", "debug"
            unschedule(loRecheck)
            unschedule(hiRecheck)
        }
        state.warnState = 0  // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "checkPower()", duration: elapsed]
    debug "checkPower() completed in ${elapsed} seconds", "trace", -1
}

def loPower(w) { 
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "loPower()"]
    debug "executing loPower(w: ${w})", "trace", 1
    def warnState = state.warnState
    unschedule(hiRecheck)
    if (warnState == 10) { //already warned of low power condition
    	debug "low power warning condition already exists"
    } else if (warnState == 11) { //pre-existing low power condition awaiting scheduled loRecheck()
    	debug "pre-existing low power condition awaiting scheduled loRecheck()"
    } else if (!loDuration) { //new low-power condition without temporary tolerance specified
        state.warnState = 10 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
        def msg = "Power consumption by the ${thePower.label} has dropped to ${w} watts."
        debug msg, "warn"
        sendNotification(msg)
    } else { //new low-power condition: schedule loRecheck to check again after the grace period has elapsed
        state.warnState = 11 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
        debug "New low-power condition detected; scheduling a re-check in ${loDuration} minutes.", "info"
        runIn(loDuration*60, loRecheck)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "loPower()", duration: elapsed]
    debug "loPower() completed in ${elapsed} seconds", "trace", -1
}

def hiPower(w) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "hiPower()"]
    debug "executing hiPower(w: ${w})", "trace", 1
    def warnState = state.warnState
    unschedule(loRecheck)
    if (w > hiSpike) {
        state.warnState = 20 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
        def msg = "Power consumption by the ${thePower.label} has spiked to ${w} watts."
        debug msg, "warn"
        sendNotification(msg)
    } else {
        if (warnState == 20) { //already warned of high power condition
            debug "high power warning condition already exists"
        } else if (warnState == 21) { //pre-existing high power condition awaiting scheduled hiRecheck()
            debug "pre-existing high power condition awaiting scheduled hiRecheck()"
        } else if (!hiDuration) { //new high-power condition without temporary tolerance specified
            state.warnState = 20 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
            def msg = "Power consumption by the ${thePower.label} has increased to ${w} watts."
            debug msg, "warn"
            sendNotification(msg)
        } else { //new high-power condition: schedule hiRecheck to check again after the grace period has elapsed
            state.warnState = 21 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
            debug "New high-power condition detected; scheduling a re-check in ${hiDuration} minutes.", "info"
            runIn(hiDuration*60, hiRecheck)
        }
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "hiPower()", duration: elapsed]
    debug "hiPower() completed in ${elapsed} seconds", "trace", -1
}
    
def loRecheck() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "loRecheck()"]
    debug "executing loRecheck()", "trace", 1
    def warnState = state.warnState
    if (warnState == 11) { 
        state.warnState = 10 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
        def msg = "Power consumption by the ${thePower.label} has remained below ${loPower} watts for ${loDuration} minutes."
        debug msg, "warn"
        sendNotification(msg)
    } else {
    	debug "low power condition was reset since loRecheck() was scheduled; no action"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "loRecheck()", duration: elapsed]
    debug "loRecheck() completed in ${elapsed} seconds", "trace", -1
}

def hiRecheck() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "hiRecheck()"]
    debug "executing hiRecheck()", "trace", 1
    def warnState = state.warnState
    if (warnState == 21) { 
        state.warnState = 20 // 0=ok, 10=low power warning triggered, 11=low power grace period, 20=high power warning triggered, 21=high power grace period
        def msg = "Power consumption by the ${thePower.label} has remained above ${hiPower} watts for ${hiDuration} minutes."
        debug msg, "warn"
        sendNotification(msg)
    } else {
    	debug "high power condition was reset since hiRecheck() was scheduled; no action"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "hiRecheck()", duration: elapsed]
    debug "hiRecheck() completed in ${elapsed} seconds", "trace", -1
}

def sendNotification(msg) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sendNotification()"]
	debug "executing sendNotification()", "trace", 1
    if (sendPush) {
    	debug "sendPush : ${msg}", "warn"
        sendPush(msg)
    }
    if (sendSMS && numberSMS) {
    	debug "sendSMS : ${msg}", "warn"
        sendSms(numberSMS, msg)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "sendNotification()", duration: elapsed]
    debug "sendNotification() completed in ${elapsed} seconds", "trace", -1
}


//   -------------------------
//   ***   APP FUNCTIONS   ***



//   ------------------------
//   ***   COMMON UTILS   ***

def convertToHMS(ms) {
    int hours = Math.floor(ms/1000/60/60)
    int minutes = Math.floor((ms/1000/60) - (hours * 60))
    int seconds = Math.floor((ms/1000) - (hours * 60 * 60) - (minutes * 60))
    long millisec = ms-(hours*60*60*1000)-(minutes*60*1000)-(seconds*1000)
    int tenths = (millisec/100).round(0)
    return "${hours}h${minutes}m${seconds}.${tenths}s"
}

def getAppImg(imgName, forceIcon = null) {
	def imgPath = appImgPath()
    return (!noAppIcons || forceIcon) ? "$imgPath/$imgName" : ""
}

def getWebData(params, desc, text=true) {
	try {
		debug "trying getWebData for ${desc}"
		httpGet(params) { resp ->
			if(resp.data) {
				if(text) {
					return resp?.data?.text.toString()
				} else { return resp?.data }
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			debug "${desc} file not found", "warn"
		} else {
			debug "getWebData(params: $params, desc: $desc, text: $text) Exception:", "error"
		}
		return "an error occured while trying to retrieve ${desc} data"
	}
}

def debug(message, lvl = null, shift = null, err = null) {
	
    def debugging = settings.debugging
	if (!debugging) {
		return
	}
    
    lvl = lvl ?: "debug"
	if (!settings["log#$lvl"]) {
		return
	}
	
    def multiEnable = (settings.setMultiLevelLog == false ? false : true) //set to true by default
    int maxLevel = 4
	int level = state.debugLevel ?: 0
	int levelDelta = 0
	def prefix = "║"
	def pad = "░"
	
    //shift is:
	//	 0 - initialize level, level set to 1
	//	 1 - start of routine, level up
	//	-1 - end of routine, level down
	//	 anything else - nothing happens
	
    switch (shift) {
		case 0:
			level = 0
			prefix = ""
			break
		case 1:
			level += 1
			prefix = "╚"
			pad = "═"
			break
		case -1:
			levelDelta = -(level > 0 ? 1 : 0)
			pad = "═"
			prefix = "╔"
			break
	}

	if (level > 0) {
		prefix = prefix.padLeft(level, "║").padRight(maxLevel, pad)
	}

	level += levelDelta
	state.debugLevel = level

	if (multiEnable) {
		prefix += " "
	} else {
		prefix = ""
	}

    def logMsg = null
    if (lvl == "info") {
    	def leftPad = (multiEnable ? ": :" : "")
        log.info "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else if (lvl == "trace") {
    	def leftPad = (multiEnable ? "::" : "")
        log.trace "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else if (lvl == "warn") {
    	def leftPad = (multiEnable ? "::" : "")
		log.warn "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else if (lvl == "error") {
    	def leftPad = (multiEnable ? "::" : "")
		log.error "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else {
		log.debug "$prefix$message", err
        logMsg = "${message}"
	}
    
    if (logMsg) {
    	def debugLog = state.debugLogInfo ?: [] //create list if it doesn't already exist
        debugLog.add(0,[time: now(), msg: logMsg, type: lvl]) //insert log info into list slot 0, shifting other entries to the right
        int maxLogs = settings.maxInfoLogs ?: 5
        int listSize = debugLog.size()
        while (listSize > maxLogs) { //delete old entries to prevent list from growing beyond set size
            debugLog.remove(maxLogs)
            listSize = debugLog.size()
        }
    	state.debugLogInfo = debugLog
    }
}