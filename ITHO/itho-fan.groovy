metadata {
    definition(name: "ITO FANBOX", namespace: "Fan", author: "Fanman76", importUrl: "") {
        capability "Polling"
        capability "Initialize"
        capability "Actuator"
        command 'SetSpeed', [[name: 'SetSpeed', type: 'ENUM', constraints: ['low', 'medium', 'high'], defaultValue: "low", description: 'Set preset speed']]
        command 'SetTimer', [[name: 'timer*', description: 'Choose one of preset timers', type: 'ENUM', constraints: ['timer1', 'timer2', 'timer3']]]
        command 'StopTimer'
        attribute "FanPreset", "string"
        attribute "FanSpeed", "integer"
        attribute "FanTimer", "string"
    }
}

preferences {
    section {
        input "ipAddress", "text", title: "IP address", required: true
        input name: "username", type: "string", title: "Username", required: true
        input name: "password", type: "password", title: "Password", required: true
        input (name: "enableDebug", type: "bool", title: "Enable debug logging", defaultValue: false)
        input "autoPoll", "bool", required: false, title: "Enable Auto Poll"
        input "pollInterval", "enum", title: "Auto Poll Interval (in seconds)", required: false, defaultValue: "15", options: ["1", "5", "10", "15", "30", "59"]
    }
}

import hubitat.scheduling.AsyncResponse

void updated() {
    unschedule()
    if (autoPoll) {
        schedule("*/${pollInterval} * * ? * * *", poll, [overwrite: true])
    }
}

void initialize() {
    if (enableDebug) { log.debug "Initializing" }
    int disableTime = 1800
    unschedule()
    sendEvent(name: "FanPreset", value: "")
    sendEvent(name: "FanSpeed", value: "")
    sendEvent(name: "FanTimer", value: "")
    if (enableDebug) {
        log.debug "Debug logging will be automatically disabled in ${disableTime} seconds"
    }
    if (autoPoll) {
        schedule("*/${pollInterval} * * ? * * *", poll, [overwrite: true])
    }
}

void poll() {
    Map params = [uri: "http://${ipAddress}/api.html?username=${username}&password=${password}&get=ithostatus"]
    params.contentType = 'application/json'
    params.requestContentType = 'application/json'
    asynchttpGet('pollResponse', params)
}

void pollResponse(AsyncResponse response, Map data = null) {
    if (response?.status == 200) {
        Map json = response.getJson()
        if (enableDebug) { log.debug "Raw API response: ${json}" }
        String fanInfo
        def fanSpeed
        def fanTimer = ""
        if (json.containsKey('fan-info'))      { fanInfo  = json['fan-info']      }
        if (json.containsKey('fan-speed_rpm')) { fanSpeed = json['fan-speed_rpm'] }
        if (json.containsKey('period-timer'))  { fanTimer = json['period-timer']  }
        sendEvent([name: "FanPreset", value: "${fanInfo}",      unit: "Preset" ])
        sendEvent([name: "FanSpeed",  value: "${fanSpeed} rpm", unit: "RPM"    ])
        sendEvent([name: "FanTimer",  value: "${fanTimer}",     unit: "Minutes"])
    }
}

void SetSpeed(String mode) {
    def setspeeduri = "http://${ipAddress}/api.html?username=${username}&password=${password}&vremotecmd="
    switch (mode) {
        case 'low':
            httpGet(uri: "${setspeeduri}low") { resp ->
                if (resp?.status == 200) {
                    if (enableDebug) { log.debug "SetSpeed to low" }
                    runInMillis(1500, poll)
                }
            }
            break
        case 'medium':
            httpGet(uri: "${setspeeduri}medium") { resp ->
                if (resp?.status == 200) {
                    if (enableDebug) { log.debug "SetSpeed to medium" }
                    runInMillis(1500, poll)
                }
            }
            break
        case 'high':
            httpGet(uri: "${setspeeduri}high") { resp ->
                if (resp?.status == 200) {
                    if (enableDebug) { log.debug "SetSpeed to high" }
                    runInMillis(1500, poll)
                }
            }
            break
    }
}

void SetTimer(String mode) {
    def settimeruri = "http://${ipAddress}/api.html?username=${username}&password=${password}&vremotecmd="
    switch (mode) {
        case 'timer1':
            httpGet(uri: "${settimeruri}timer1") { resp ->
                if (resp?.status == 200) {
                    if (enableDebug) { log.debug "Timer set to timer1" }
                    runInMillis(1500, poll)
                }
            }
            break
        case 'timer2':
            httpGet(uri: "${settimeruri}timer2") { resp ->
                if (resp?.status == 200) {
                    if (enableDebug) { log.debug "Timer set to timer2" }
                    runInMillis(1500, poll)
                }
            }
            break
        case 'timer3':
            httpGet(uri: "${settimeruri}timer3") { resp ->
                if (resp?.status == 200) {
                    if (enableDebug) { log.debug "Timer set to timer3" }
                    runInMillis(1500, poll)
                }
            }
            break
    }
}

void StopTimer() {
    Map params = [uri: "http://${ipAddress}/api.html?username=${username}&password=${password}&vremotecmd=clearqueue"]
    params.contentType = 'application/json'
    params.requestContentType = 'application/json'
    asynchttpGet('clearTimerResponse', params)
}

void clearTimerResponse(AsyncResponse response, Map data = null) {
    if (response?.status == 200) {
        if (enableDebug) { log.debug "Running timers cleared" }
        sendEvent([name: "FanTimer", value: "", unit: "Minutes"])
        runInMillis(1500, poll)
    }
}
