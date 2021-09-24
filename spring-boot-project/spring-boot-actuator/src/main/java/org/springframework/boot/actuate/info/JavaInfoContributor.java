package org.springframework.boot.actuate.info;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JavaInfoContributor implements InfoContributor {
	private final Map<String, Object> javaInfo;

	public JavaInfoContributor() {
		this.javaInfo = new LinkedHashMap<>();
		javaInfo.put("runtime", runtimeInfo());
		javaInfo.put("vm", vmInfo());
		javaInfo.put("vendor", System.getProperty("java.vendor"));
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("java", javaInfo);
	}

	private Map<String, String> runtimeInfo() {
		Map<String, String> runtimeInfo = new HashMap<>();
		runtimeInfo.put("name", System.getProperty("java.runtime.name"));
		runtimeInfo.put("version", System.getProperty("java.runtime.version"));

		return runtimeInfo;
	}

	private Map<String, String> vmInfo() {
		Map<String, String> vmInfo = new HashMap<>();
		vmInfo.put("name", System.getProperty("java.vm.name"));
		vmInfo.put("version", System.getProperty("java.vm.version"));

		return vmInfo;
	}
}
