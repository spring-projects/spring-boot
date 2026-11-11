/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the service.
 *
 * @author Phillip Webb
 */
@ConfigurationProperties
public class ServiceProperties {

	private String vehicleServiceRootUrl = "http://localhost:8080/vs";

	public String getVehicleServiceRootUrl() {
		return this.vehicleServiceRootUrl;
	}

	public void setVehicleServiceRootUrl(String vehicleServiceRootUrl) {
		this.vehicleServiceRootUrl = vehicleServiceRootUrl;
	}

}
