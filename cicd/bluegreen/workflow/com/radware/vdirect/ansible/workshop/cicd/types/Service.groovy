package com.radware.vdirect.ansible.workshop.cicd.types

import com.radware.alteon.workflow.impl.java.Param;

public class Service {
	@Param(type="string", prompt="Virt service id")
	public String id;
	@Param(type="string", uiVisible="false", required=false)
	public String protocol;
	@Param(type="string", uiVisible="false", required=false)
	public String port;
	@Param(type="string", prompt="BLUE group id")
	public String blueGroupId;
	@Param(type="string", prompt="GREEN group id")
	public String greenGroupId;
}
