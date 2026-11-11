/*
 * Copyright 2012-present the original author or authors.
 */
package org.springframework.boot.docs.io.restclient.httpservice.customization

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer

@Configuration(proxyBeanMethods = false)
class MyHttpServiceGroupConfiguration {

	@Bean
	fun myHttpServiceGroupConfigurer(): RestClientHttpServiceGroupConfigurer {
		return RestClientHttpServiceGroupConfigurer { groups ->
			groups.forEachClient { group, clientBuilder ->
				val groupName = group.name()
				clientBuilder.defaultHeader("service-group", groupName)
			}
		}
	}

}
