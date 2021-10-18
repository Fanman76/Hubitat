/*
    Hubitat driver for the HomeWizard P1 energy meter.
 */

metadata
{
    definition(name: "HomeWizard P1 Meter", namespace: "Fan", author: "Fanman-76", importUrl: "https://raw.githubusercontent.com/Fanman76/Hubitat/master/HomewizardP1/hwp1_driver.groovy")
    {
        capability "Initialize"
        capability "PowerMeter"
        capability "Refresh"
        
        attribute "commStatus", "string"
        attribute "smr_version", "string"
        attribute "meter_model", "string"
        attribute "wifi_ssid", "string"
        attribute "wifi_strength", "number"
        attribute "total_power_import_t1_kwh", "number"
        attribute "total_power_import_t2_kwh", "number"
        attribute "total_power_export_t1_kwh", "number"
        attribute "total_power_export_t2_kwh", "number"
	    attribute "active_power_l1_w", "number"
	    attribute "active_power_l2_w", "number"
	    attribute "active_power_l3_w", "number"
	    attribute "total_gas_m3", "number"
    }
}

preferences
{
    section
    {
        input "ipAddress", "text", title: "IP address meter", required: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "threephase", type: "bool", title: "Enable 3 phase logging", defaultValue: false
		input name: "gas", type: "bool", title: "Enable gas usage logging", defaultValue: true
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
    sendEvent(name: "commStatus", value: "unknown")
    sendEvent(name: "wifi_strength", value: "unknown")
    sendEvent(name: "smr_version", value: "unknown")
    sendEvent(name: "meter_model", value: "unknown")
    sendEvent(name: "wifi_ssid", value: "unknown")
    sendEvent(name: "wifi_strength", value: "unknown")
    sendEvent(name: "total_power_import_t1_kwh", value: "unknown")
    sendEvent(name: "total_power_import_t2_kwh", value: "unknown")
    sendEvent(name: "total_power_export_t1_kwh", value: "unknown")
    sendEvent(name: "total_power_export_t2_kwh", value: "unknown")
    sendEvent(name: "active_power_l1_w", value: "unknown")
    sendEvent(name: "total_gas_m3", value: "unknown")

    if (gas) {
            sendEvent(name: "total_gas_m3", value: "unknown")
        }
    else {
            sendEvent(name: "total_gas_m3", value: "0")    
        }
	
    if (threephase) {
            sendEvent(name: "active_power_l2_w", value: "unknown")
            sendEvent(name: "active_power_l3_w", value: "unknown")
        }
    else {
            sendEvent(name: "active_power_l2_w", value: "0")
            sendEvent(name: "active_power_l3_w", value: "0")    
        }
    unschedule()
    refresh()
}

def refresh()
{

    try
    {
        def res = httpGetExec([uri: getBaseURI()], true)
        sendEvent(name: "commStatus", value: "good")
        sendEvent(name: "smr_version", value: res?.smr_version.toString())
        sendEvent(name: "meter_model", value: res?.meter_model.toString())
        sendEvent(name: "wifi_ssid", value: res?.wifi_ssid.toString())
        sendEvent([name: "wifi_strength", value: res?.wifi_strength.toInteger(), unit: "%"])
        sendEvent([name: "total_power_import_t1_kwh", value: res?.total_power_import_t1_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "total_power_import_t2_kwh", value: res?.total_power_import_t2_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "total_power_export_t1_kwh", value: res?.total_power_export_t1_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "total_power_export_t2_kwh", value: res?.total_power_export_t2_kwh.toInteger(), unit: "kWh"])
        
     if (res) { 
       if (res.active_power_l1_w != "NULL") {
         sendEvent([name: "active_power_l1_w", value: res?.active_power_l1_w.toInteger(), unit: "W"])
      }
    }  

     if (gas)
        {
            sendEvent([name: "total_gas_m3", value: res?.total_gas_m3.toInteger(), unit: "m3"])
        }
		
    if (threephase)
        {
            sendEvent([name: "active_power_l2_w", value: res?.active_power_l2_w.toInteger(), unit: "W"])
            sendEvent([name: "active_power_l3_w", value: res?.active_power_l3_w.toInteger(), unit: "W"])
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
