/*
    Hubitat driver for the HomeWizard P1 energy meter.
    Special thanks to TOMW for making this possible!

 */

metadata
{
    definition(name: "HomeWizard P1 Meter", namespace: "fan", author: "Fanman-76", importUrl: "https://github.com/Fanman76/Hubitat-HomeWizard_P1/blob/master/hwp1_driver.groovy")
    {
        capability "Initialize"
        capability "PowerMeter"
        capability "Refresh"
        
        attribute "commStatus", "string"
        attribute "smr_version", "string"
        attribute "meter_model", "string"
        attribute "wifi_ssid", "string"
        attribute "wifi_strength", "string"
        attribute "total_power_import_t1_kwh", "number"
        attribute "total_power_import_t2_kwh", "number"
        attribute "total_power_import_t3_kwh", "number"
		attribute "active_power_w", "number"
		attribute "active_power_l1_w", "string"
		attribute "active_power_l2_w", "string"
		attribute "active_power_l3_w", "string"
		attribute "total_gas_m3", "number"
		attribute "gas_timestamp", "date"
    }
}

preferences
{
    section
    {
        input "ipAddress", "text", title: "IP address meter", required: true
        input "refreshInterval", "number", title: "Refresh interval (seconds)", defaultValue: 10
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
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
    sendEvent(name: "active_power_w", value: "unknown")
    sendEvent(name: "WiFi-Strength", value: "unknown")
    sendEvent(name: "smr_version", value: "unknown")
    sendEvent(name: "meter_model", value: "unknown")
    sendEvent(name: "wifi_ssid", value: "unknown")
    sendEvent(name: "wifi_strength", value: "unknown")
    sendEvent(name: "total_power_import_t1_kwh", value: "unknown")
    sendEvent(name: "total_power_import_t2_kwh", value: "unknown")
    sendEvent(name: "total_power_export_t1_kwh", value: "unknown")
    sendEvent(name: "total_power_export_t2_kwh", value: "unknown")
    sendEvent(name: "active_power_l1_w", value: "unknown")
    sendEvent(name: "active_power_l2_w", value: "unknown")
    sendEvent(name: "active_power_l3_w", value: "unknown")
    sendEvent(name: "total_gas_m3", value: "unknown")
    sendEvent(name: "gas_timestamp", value: "unknown")
    refresh()
}

def refresh()
{
    unschedule()
    
    try
    {
        def res = httpGetExec([uri: getBaseURI()], true)
        sendEvent(name: "commStatus", value: "good")
        sendEvent(name: "smr_version", value: res?.smr_version.toInteger())
        sendEvent(name: "meter_model", value: res?.meter_model.toString())
        sendEvent(name: "wifi_ssid", value: res?.wifi_ssid.toString())
        sendEvent(name: "wifi_strength", value: res?.wifi_strength.toInteger())
        sendEvent(name: "total_power_import_t1_kwh", value: res?.total_power_import_t1_kwh.toInteger())
        sendEvent(name: "total_power_import_t2_kwh", value: res?.total_power_import_t2_kwh.toInteger())
        sendEvent(name: "total_power_export_t1_kwh", value: res?.total_power_export_t1_kwh.toInteger())
        sendEvent(name: "total_power_export_t2_kwh", value: res?.total_power_export_t1_kwh.toInteger())
        sendEvent(name: "active_power_w", value: res?.active_power_w?.toInteger())
        sendEvent(name: "active_power_l1_w", value: res?.active_power_l1_w.toString())
        sendEvent(name: "active_power_l2_w", value: res?.active_power_l2_w.toString())
        sendEvent(name: "active_power_l3_w", value: res?.active_power_l3_w.toString())
        sendEvent(name: "total_gas_m3", value: res?.total_gas_m3.toInteger())
        sendEvent(name: "gas_timestamp", value: res?.gas_timestamp.toInteger())
        
        // schedule next refresh
        runIn(refreshInterval.toInteger(), refresh)
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
                logDebug("resp.data = ${resp.data}")
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
