package com.radware.vdirect.ansible.workshop.cicd.canary

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
import com.radware.alteon.beans.adc.SlbNewCfgEnhVirtualServerEntry;
import com.radware.vdirect.ansible.workshop.cicd.types.RealServer;


@Workflow(
createAction='init',
deleteAction='teardown',
states = [
	@State('initialized'),
	@State('applied'),
	@State('removed')],
devices = [
	@Device(name = "adc", type = DeviceType.alteon, prompt="Alteon device")
],
properties = [
	@Param(name="group", type="string", prompt="Servers group Id"),
	@Param(name="canary", type="RealServer", prompt="Canary server info"),
	@Param(name="normal_weight", prompt="Production servers weight", type="int", defaultValue="1", minValue="1", maxValue="24"),
	@Param(name="canary_weight", prompt="Canary server weight", type="int", defaultValue="1", minValue="1", maxValue="24"),
	@Param(name="origin_weights", prompt="Canary server weight", type="object", defaultValue="{}")
	]
)

class CanaryCICDWorkflow {
	@Autowired WorkflowAdaptor workflow
	@Autowired Devices devices
	@Autowired VDirectLogger log
	@Autowired IAdcSessionBoundObjectFactory factory
	@Autowired IAdcSessionBoundResourcePoolManager services;
	@Autowired IAdcSessionBoundResourceManager resourceManager;
	@Autowired VDirectServerClient vDirectServerClient;
	
	@Action(
	fromState="none", 
	toState="initialized",
	visible=false)
	void init (
			@Device(ref="adc") DeviceConnection adc) {

		if (workflow.state != 'created') {
			return
		}

		devices['adc'] = adc
		devices.autoRevertOnError = true
	}

	@Action(fromState="initialized", toState="applied")
	void deploy (
			@Param(ref="group") String groupId,
			@Param(ref="canary") RealServer canaryServer,
			@Param(ref="normal_weight") Integer normalWeight,
			@Param(ref="canary_weight") Integer canaryWeight) {
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put('adc', devices['adc'])
		params.put('canary', canaryServer)
		params.put('groupId', groupId)
		params.put('normalWeight', normalWeight)
		params.put('canaryWeight', canaryWeight)
		
		ConfigurationTemplate template = this.workflow.getTemplate("deploy_canary.vm")
		template.setShareConnections(false)
		template.parameters = params
		
		devices.autoRevertOnError = true
		template.run()
		devices.commit(true, true, true)
	}
			
	@Action(fromState="applied", toState="applied")
	void update (
			@Param(ref="normal_weight") Integer normalWeight,
			@Param(ref="canary_weight") Integer canaryWeight) {
			
		Map<String, Object> params = new HashMap<String, Object>();
		params.put('adc', devices['adc'])
		params.put('canary', workflow['canary'])
		params.put('groupId', workflow['group'])
		params.put('normalWeight', normalWeight)
		params.put('canaryWeight', canaryWeight)
		
		ConfigurationTemplate template = this.workflow.getTemplate("update_canary.vm")
		template.setShareConnections(false)
		template.parameters = params
		
		devices.autoRevertOnError = true
		template.run()
		devices.commit(true, true, true)
	}
			
	@Action(fromState="applied", toState="initialized")
	void undeploy () {
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put('adc', devices['adc'])
		params.put('canary', workflow['canary'])
		params.put('groupId', workflow['group'])
		params.put('originWeights', workflow['originWeights'])
		
		ConfigurationTemplate template = this.workflow.getTemplate("undeploy_canary.vm")
		template.setShareConnections(false)
		template.parameters = params
		
		devices.autoRevertOnError = true
		template.run()
		devices.commit(true, true, true)
	}

	@Action(fromState=["initialized","applied"], toState='removed')
	void teardown () {
		if (!workflow.state.equals('applied')) {
			return
		}
		
		log.info "Removing Canary server and setting the production servers weight to " + workflow['normalWeight']
		undeploy()
		log.info "Tearing down"
		if (workflow.state.equals('removed')) {
			return
		}
	}
}