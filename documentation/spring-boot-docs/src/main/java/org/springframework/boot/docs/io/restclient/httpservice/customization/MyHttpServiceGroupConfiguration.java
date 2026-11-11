/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.httpservice.customization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

@Configuration(proxyBeanMethods = false)
public class MyHttpServiceGroupConfiguration {

	@Bean
	RestClientHttpServiceGroupConfigurer myHttpServiceGroupConfigurer() {
		return (groups) -> groups.forEachClient((group, clientBuilder) -> {
			String groupName = group.name();
			clientBuilder.defaultHeader("service-group", groupName);
		});
	}

}
