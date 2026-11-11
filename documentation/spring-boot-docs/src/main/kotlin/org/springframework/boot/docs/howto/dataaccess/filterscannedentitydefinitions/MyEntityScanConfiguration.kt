/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.filterscannedentitydefinitions

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.persistenceunit.ManagedClassNameFilter

@Configuration(proxyBeanMethods = false)
class MyEntityScanConfiguration {

	@Bean
	fun entityScanFilter() : ManagedClassNameFilter {
		return ManagedClassNameFilter { className ->
			className.startsWith("com.example.app.customer.")
		}
	}
}

