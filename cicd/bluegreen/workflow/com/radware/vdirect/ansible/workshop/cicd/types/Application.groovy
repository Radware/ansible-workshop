package com.radware.vdirect.ansible.workshop.cicd.types

import com.radware.alteon.workflow.impl.java.Param;

public class Application {
	@Param(type="string", uiVisible="false", required=false)
	public String id;
	@Param(prompt='Application IP address')
	public String ip
	@Param(defaultValue="[]", prompt='Application services configuration')
	public Service[] services
}
