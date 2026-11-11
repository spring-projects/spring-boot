/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.customstarter.configurationkeys

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("acme")
class AcmeProperties(

	/**
	 * Whether to check the location of acme resources.
	 */
	var isCheckLocation: Boolean = true,

	/**
	 * Timeout for establishing a connection to the acme server.
	 */
	var loginTimeout:Duration = Duration.ofSeconds(3))

