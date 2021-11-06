/*
    Hubitat driver for the HomeWizard Power Socket meter.
 */

metadata
{
    definition(name: "HomeWizard Power Socket", namespace: "Fan", author: "Fanman-76", importUrl: "https://raw.githubusercontent.com/Fanman76/Hubitat/master/HomewizardP1/hwsocket_driver.groovy")
    {
        capability "Initialize"
        capability "PowerMeter"
        capability "Refresh"
        
        attribute "wifi_ssid", "string"
        attribute "wifi_strength", "number"
        attribute "total_power_import_t1_kwh", "number"
        attribute "total_power_export_t1_kwh", "number"
        attribute "active_power_l1_w", "number"
        attribute "power", "number"
    }
}

preferences
{
    section
    {
        input "ipAddress", "text", title: "IP address meter", required: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input ( name: 'pollInterval', type: 'enum', title: 'Update interval (in minutes)', options: ['1', '5', '10', '15', '30', '60', '180'], required: true, defaultValue: '60' )
	    input name: "enablePoll", type: "bool", title: "Enable device polling", defaultValue: false
    }
}

def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}

def updated()
{
    refresh()
}

def initialize()
{
    sendEvent(name: "wifi_strength", value: "unknown")
    sendEvent(name: "wifi_ssid", value: "unknown")
    sendEvent(name: "total_power_import_t1_kwh", value: "unknown")
    sendEvent(name: "total_power_export_t1_kwh", value: "unknown")
    sendEvent(name: "active_power_l1_w", value: "unknown")
    sendEvent(name: "power", value: "unknown")

    unschedule()
    refresh()
}

def refresh()
{

    try
    {
        def res = httpGetExec([uri: getBaseURI()], true)
        sendEvent(name: "wifi_ssid", value: res?.wifi_ssid.toString())
        sendEvent([name: "wifi_strength", value: res?.wifi_strength.toInteger(), unit: "%"])
        sendEvent([name: "total_power_import_t1_kwh", value: res?.total_power_import_t1_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "total_power_export_t1_kwh", value: res?.total_power_export_t1_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "power", value: res?.active_power_w.toInteger(), unit: "W"])
        
     if (res) { 
       if (res.active_power_l1_w != "NULL") {
         sendEvent([name: "active_power_l1_w", value: res?.active_power_l1_w.toInteger(), unit: "W"])
      }
    }  

        
    if (enablePoll) {
	
		Integer interval = Integer.parseInt(settings.pollInterval)
		
		switch (interval) {
			case 1: 
				runEvery1Minute(refresh)
				break
			case 5:
				runEvery5Minutes(refresh)
				break
			case 10:
				runEvery10Minutes(refresh)
				break
			case 15:
				runEvery15Minutes(refresh)
				break
			case 30:
				runEvery30Minutes(refresh)
				break
			case 60:
				runEvery1Hour(refresh)
				break
			case 180:
				runEvery3Hours(refresh)
				break
			default:
				runIn(interval*60,refresh)
				break
		}
		
	    logDebug("Scheduled to run ${interval} minutes") 
		runIn(2, getBaseURI) //  Run after updates in addition to the scheduled poll
	}
    }
    catch (Exception e)
    {
        logDebug("refresh() failed: ${e.message}")
        logDebug("run Refresh command re-start polling")
        sendEvent(name: "commStatus", value: "error")
    }    
}

def getBaseURI()
{
    return "http://${ipAddress}/api/v1/data"
}

def httpGetExec(params, throwToCaller = false)
{
    logDebug("httpGetExec(${params})")
    
    try
    {
        def result
        httpGet(params)
        { resp ->
            if (resp.data)
            {
//                logDebug("resp.data = ${resp.data}")
                result = resp.data
            }
        }
        return result
    }
    catch (Exception e)
    {
        logDebug("httpGetExec() failed: ${e.message}")
        if(throwToCaller)
        {
            throw(e)
        }
    }
}