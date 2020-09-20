/*
    Hubitat driver for the HomeWizard P1 energy meter.
    Special thanks to TOMW for making this possible!

 */

metadata
{
    definition(name: "HomeWizard P1 Meter", namespace: "fan", author: "Fanman-76", importUrl: "")
    {
        capability "Initialize"
        capability "PowerMeter"
        capability "Refresh"
        
        attribute "commStatus", "string"
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
    sendEvent(name: "power", value: "unknown")
    sendEvent(name: "WiFi-Strength", value: "unknown")
    sendEvent(name: "total_power_import_t1_kwh", value: "unknown")
    refresh()
}

def refresh()
{
    unschedule()
    
    try
    {
        def res = httpGetExec([uri: getBaseURI()], true)
        sendEvent(name: "commStatus", value: "good")
        sendEvent(name: "power", value: res?.active_power_w?.toInteger())
        sendEvent(name: "WiFi-Strength", value: res?.wifi_strength.toInteger())
        sendEvent(name: "total_power_import_t1_kwh", value: res?.total_power_import_t1_kwh.toInteger())
        
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
