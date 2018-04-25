package com.radware.vdirect.ansible.workshop.cicd.bluegreen

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;

import com.radware.alteon.api.ServiceNames
import com.radware.alteon.api.Protocols
import com.radware.alteon.api.impl.EasyBean
import com.radware.alteon.api.AdcCLIConnectionBean
import com.radware.alteon.api.AdcCLIConnection
import com.radware.alteon.api.AdcTemplateResult
import com.radware.alteon.api.AdcTimeoutException
import com.radware.alteon.beans.adc.EnumActionType
import com.radware.alteon.beans.adc.SlbNewCfgGroupEntry
import com.radware.alteon.beans.adc.SlbNewCfgRealServerEntry
import com.radware.alteon.sdk.*
import com.radware.alteon.sdk.Tenant
import com.radware.alteon.workflow.AdcWorkflowException
import com.radware.alteon.workflow.impl.java.Devices
import com.radware.alteon.workflow.impl.DeviceConnection;
import com.radware.alteon.workflow.impl.WorkflowAdaptor;
import com.radware.alteon.workflow.impl.java.Action;
import com.radware.alteon.workflow.impl.java.ConfigurationTemplate;
import com.radware.alteon.workflow.impl.java.Param;
import com.radware.alteon.workflow.impl.java.State;
import com.radware.alteon.workflow.impl.java.Device;
import com.radware.alteon.workflow.impl.java.Workflow;
import com.radware.logging.VDirectLogger;
import com.radware.vdirect.server.VDirectServerClient
import com.radware.vdirect.client.api.DeviceType
import com.radware.vdirect.client.api.IAdcSessionBoundConfigurationTemplateManager;
import com.radware.vdirect.client.api.IAdcSessionBoundObjectFactory;
import com.radware.vdirect.client.sdk.IAdcSessionBoundResourceManager;
import com.radware.vdirect.client.sdk.IAdcSessionBoundResourcePoolManager;
import com.radware.vdirect.client.sdk.impl.InternalResourceManager;
import com.radware.alteon.beans.adc.SlbNewCfgEnhVirtServicesEntry;
import com.radware.alteon.beans.adc.SlbCurCfgEnhVirtualServerEntry;
import com.radware.alteon.beans.adc.SlbNewCfgEnhVirtualServerEntry;
import com.radware.alteon.beans.adc.SlbCurCfgEnhVirtServicesSeventhPartEntry;
import com.radware.alteon.beans.adc.SlbCurCfgEnhVirtServicesFifthPartEntry;
import com.radware.alteon.beans.adc.SlbCurCfgEnhGroupEntry;
import com.radware.vdirect.ansible.workshop.cicd.types.*;


@Workflow(
createAction='init',
deleteAction='teardown',
states = [
	@State('applied_blue'),
	@State('applied_green'),
	@State('removed')],
devices = [
	@Device(name = "adc", type = DeviceType.alteon, prompt="Alteon device")
],
properties = [
	@Param(name="application", type='Application', prompt="Application configuration"),
	@Param(name="applicationState", type='string', defaultValue="BLUE", values=["BLUE", "GREEN"], prompt="Application state")
	]
)

class BlueGreenCICDWorkflow {
	@Autowired WorkflowAdaptor workflow
	@Autowired Devices devices
	@Autowired VDirectLogger log
	@Autowired IAdcSessionBoundObjectFactory factory
	@Autowired IAdcSessionBoundResourcePoolManager services;
	@Autowired IAdcSessionBoundResourceManager resourceManager;
	@Autowired VDirectServerClient vDirectServerClient;
	
	private static final String TEMPLATE_TXT = '''

	<% if (state.equals("BLUE")) { %>
		<h3>Application state: <font color="blue">${state}</font></h3>
    <% } 
    else { %>
		<h3>Application state: <font color="green">${state}</font></h3>
    <% } %>
    <ul>
        VIP ${app.ip} (Virt id = ${app.id})
		<ul>
	        <% for (s in app.services) { %>
	            Service ${s.id}<br>
	            <ul>
					port ${s.port}<br>
		            protocol ${s.protocol}<br>
		            <font color="blue">blue group</font> ${s.blueGroupId}<br>
		            <font color="green">green group</font> ${s.greenGroupId}<br>
				</ul>
	        <% } %>
		</ul>
    </ul>
	'''

	@Action(fromState="none", toState="applied_blue", visible=false)
	void init (
			@Device(ref="adc") DeviceConnection adc,
			@Param(ref="application") Application app) {
	
		if (workflow.state != 'created') {
			return
		}

		devices['adc'] = adc
		devices.autoRevertOnError = true
		validateSetup()
	}
			
	@Action(fromState="*", toState="*", resultType="text/html")
	String showSetup () {
		return buildHTMLSetup()
	}

	private String validateSetup () {
		DeviceConnection connection = devices.connect(devices['adc'])
		Application blueApp = new Application()
		Application greenApp = new Application()

		SlbNewCfgEnhVirtualServerEntry virtBean = new SlbNewCfgEnhVirtualServerEntry()
		virtBean.ipAddress = workflow['application'].ip
		virtBean = connection.read(virtBean)
		if (virtBean == null) {
			throw new AdcWorkflowException("Virt with IP address " + workflow['application'].ip + " was not found.")
		}
		
		String virtId = virtBean.index
		workflow['application'].id = virtId

		workflow['application'].services.each { service ->

			SlbNewCfgEnhVirtServicesEntry serviceBean = connection.readBean('SlbNewCfgEnhVirtServicesEntry', 'servIndex', virtId, 'index', service.id)			
			if (serviceBean == null) {
				throw new AdcWorkflowException("Virt service with id " + service.id + " was not found.")
			}

			SlbCurCfgEnhVirtServicesSeventhPartEntry service7Bean = connection.readBean('SlbCurCfgEnhVirtServicesSeventhPartEntry', 'servSeventhPartIndex', virtId, 'seventhPartIndex', service.id)
			SlbCurCfgEnhVirtServicesFifthPartEntry service5Bean = connection.readBean('SlbCurCfgEnhVirtServicesFifthPartEntry', 'servFifthPartIndex', virtId, 'fifthPartIndex', service.id)

			if (!service7Bean.realGroup.equals(service.blueGroupId)) {
				throw new AdcWorkflowException("Virt service " + service.id + " is not associated to declared BLUE group " + service.blueGroupId)
			}
			
			service.protocol = service5Bean.servApplicationType
			service.port = String.valueOf(serviceBean.virtPort)
			
			SlbCurCfgEnhGroupEntry groupBean = connection.readBean('SlbCurCfgEnhGroupEntry', 'index', service.greenGroupId)
			if (groupBean == null) {
				throw new AdcWorkflowException("Declared GRREN real servers group " + service.greenGroupId + " for virt service " + service.id + " does not exist.")
			}
		}
	}

	private void switchGroup (String state) {
		workflow['application'].services.each { service ->
			Map<String, Object> params = new HashMap<String, Object>();
			params.put('adc', devices['adc'])
			params.put('virtId', workflow['application'].id)
			params.put('servicePort', service.port)
			params.put('serviceProtocol', service.protocol)
			if (state.equals("BLUE")) {
				params.put('groupId', service.blueGroupId)
			}
			else {
				params.put('groupId', service.greenGroupId)
			}
			ConfigurationTemplate template = this.workflow.getTemplate("switch.vm")
			template.setShareConnections(false)
			template.parameters = params
			
			devices.autoRevertOnError = true
			template.run()
		}
		
		devices.commit(true, true, true)
	}

	private String buildHTMLSetup () {
		def setup = [app: workflow['application'], state: workflow['applicationState']]
		def engine = new groovy.text.GStringTemplateEngine()
		def template = engine.createTemplate(TEMPLATE_TXT).make(setup)
		log.info template.toString()
		return template.toString()
	}
	
	@Action(fromState="applied_green", toState="applied_blue")
	void switchToBlue () {
		switchGroup("BLUE")
		workflow['applicationState'] = "BLUE"
	}

	@Action(fromState="applied_blue", toState="applied_green")
	void switchToGreen () {
		switchGroup("GREEN")
		workflow['applicationState'] = "GREEN"
	}

	@Action(toState='removed')
	void teardown () {
		log.info "Tearing down"
		if (workflow.state == 'removed') {
			return
		}
	}
}