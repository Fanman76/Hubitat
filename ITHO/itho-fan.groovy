metadata {
    definition (name: "ITHO Fanspeed", namespace: "Fan", author: "Fanman76") {
		capability "Actuator"
        capability "Polling"
        capability "Initialize"
        
        attribute "FanSpeed", "number"
    }

    preferences {
        section
        {
            input "ipAddress", "text", title: "IP address", required: true
            input name: "username", type: "string", title: "Username", required: true
            input name: "password", type: "password", title: "Password", required: true
            input (name: "enablePoll", type: "bool", title: "Enable speed polling", defaultValue: true)
		    input (name: "enableDebug", type: "bool", title: "Enable debug logging", defaultValue: false)
		    input (name: 'pollInterval', type: 'enum', title: 'Update interval (in minutes)', options: ['1', '5', '10', '15', '30', '60', '180'], required: true, defaultValue: '60' )
            
            
        }
    }
}

void initialize() {
	log.debug "Initializing"
	int disableTime = 1800
   
	unschedule()
    sendEvent(name: "FanSpeed", value: "reset")
	if (enableDebug) {
		log.debug "Debug logging will be automatically disabled in ${disableTime} seconds"
		runIn(disableTime, debugOff)
	}
	
	if (enablePoll) {
	
		Integer interval = Integer.parseInt(settings.pollInterval)
		
		switch (interval) {
			case 1: 
				runEvery1Minute(poll)
				break
			case 5:
				runEvery5Minutes(poll)
				break
			case 10:
				runEvery10Minutes(poll)
				break
			case 15:
				runEvery15Minutes(poll)
				break
			case 30:
				runEvery30Minutes(poll)
				break
			case 60:
				runEvery1Hour(poll)
				break
			case 180:
				runEvery3Hours(poll)
				break
			default:
				runIn(interval*60,poll)
				break
		}
		
	    logDebug("Scheduled to run ${interval} minutes") 
		runIn(2,poll) //  Run after updates in addition to the scheduled poll
	}

}

void debugOff() {
   log.warn("Disabling debug logging")
   device.updateSetting("enableDebug", [value:"false", type:"bool"])
}

void logDebug(str) {
   if (settings.enableDebug) log.debug(str)
}

void poll()
{
    String DeviceInfoURI = "http://${ipAddress}/api.html?username=${username}&password=${password}&get=currentspeed" 
    httpGet([uri:DeviceInfoURI, contentType: "application/json"])

	{ resp->
			def contentType = resp.headers['content-type']
        if (enableDebug) { log.debug "Raw value: ${resp.data}" }
        if (resp.status == 200) {
     def numerical = "${resp.data}"
            if (enableDebug) { log.debug "Numerical value: ${numerical}" }
            def FanSpeed = numerical.toInteger()
            if (enableDebug) { log.debug "Found value: ${FanSpeed}" }
            sendEvent([name: "FanSpeed", value: "${FanSpeed}", unit: "RPM"])
        }
   } 
}