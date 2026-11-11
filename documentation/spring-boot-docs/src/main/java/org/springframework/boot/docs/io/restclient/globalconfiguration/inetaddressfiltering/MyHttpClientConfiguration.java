/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.globalconfiguration.inetaddressfiltering;

import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyHttpClientConfiguration {

	@Bean
	public InetAddressFilter httpClientInetAddressFilter() {
		return InetAddressFilter.of("192.168.1.0/24").andNot("192.168.1.1", "192.168.1.10");
	}

}
