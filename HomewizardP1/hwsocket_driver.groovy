/*
    Hubitat driver for the HomeWizard Power Socket meter.
    (c) Fanman76 11-2021
 */
import groovy.json.JsonSlurper

metadata
{
    definition(name: "HomeWizard Power Socket", namespace: "Fan", author: "Fanman-76", importUrl: "https://raw.githubusercontent.com/Fanman76/Hubitat/master/HomewizardP1/hwsocket_driver.groovy")
    {
        capability "Initialize"
        capability "PowerMeter"
        
        attribute "product_type", "string"
        attribute "product_name", "string"
        attribute "serial", "string"
        attribute "firmware_version", "string"
        attribute "api_version", "string"
        attribute "wifi_ssid", "string"
        attribute "wifi_strength", "number"
        attribute "total_power_import_t1_kwh", "number"
        attribute "total_power_export_t1_kwh", "number"
        attribute "active_power_l1_w", "number"
        attribute "power", "number"        
        
        command "getBasicInfo"
        command "getUsageInfo"
        command "unschedule"
    }
}
preferences
{
    section
    {
        input "ipAddress", "text", title: "IP address meter", required: true
        input name: "enablePoll", type: "bool", title: "Enable device polling", defaultValue: false
        input ( name: 'pollInterval', type: 'enum', title: 'Update interval (in minutes)', options: ['1', '5', '10', '15', '30', '60', '180'], required: true, defaultValue: '60' )   
    }
}

def unschedule()
{
    unschedule()
}

void getBasicInfo()
{
    String DeviceInfoURI = "http://${ipAddress}/api" 
 
    httpGet([uri:DeviceInfoURI, contentType: "application/json"])

	{ resp->
			def contentType = resp.headers['content-type']
        
        sendEvent(name: "product_type", value: resp.data.product_type.toString())
        sendEvent(name: "product_name", value: resp.data.product_name.toString())
        sendEvent(name: "serial", value: resp.data.serial.toString())
        sendEvent(name: "firmware_version", value: resp.data.firmware_version.toString())
        sendEvent(name: "api_version", value: resp.data.api_version.toString())
   } 
}

void getUsageInfo()
{
    String DeviceInfoURI = "http://${ipAddress}/api/v1/data" 
 
    httpGet([uri:DeviceInfoURI, contentType: "application/json"])

	{ resp->
			def contentType = resp.headers['content-type']

        sendEvent(name: "wifi_ssid", value: resp.data.wifi_ssid.toString())
        sendEvent([name: "wifi_strength", value: resp.data.wifi_strength.toInteger(), unit: "%"])
        sendEvent([name: "total_power_import_t1_kwh", value: resp.data.total_power_import_t1_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "total_power_export_t1_kwh", value: resp.data.total_power_export_t1_kwh.toInteger(), unit: "kWh"])
        sendEvent([name: "power", value: resp.data.active_power_w.toInteger(), unit: "W"])
   } 
}

def initialize()
{
    if (enablePoll) {
		Integer interval = Integer.parseInt(settings.pollInterval)
		
		switch (interval) {
			case 1: 
				runEvery1Minute(getUsageInfo)
				break
			case 5:
				runEvery5Minutes(getUsageInfo)
				break
			case 10:
				runEvery10Minutes(getUsageInfo)
				break
			case 15:
				runEvery15Minutes(getUsageInfo)
				break
			case 30:
				runEvery30Minutes(getUsageInfo)
				break
			case 60:
				runEvery1Hour(getUsageInfo)
				break
			case 180:
				runEvery3Hours(getUsageInfo)
				break
			default:
				runIn(interval*60,getUsageInfo)
				break
		}
		
		runIn(2, getUsageInfo) //  Run after updates in addition to the scheduled poll
	}
}
