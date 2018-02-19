package com.radware.vdirect.ansible.workshop.cicd.types

import com.radware.alteon.workflow.impl.java.Param;

public class RealServer {
	@Param(type="string")
	public String id;
	@Param(type="int", defaultValue="80")
	public Integer port;
	@Param(type="ip", prompt="IP address")
	public String address;
	@Param(type="string", defaultValue="canary")
	public String name;
}
