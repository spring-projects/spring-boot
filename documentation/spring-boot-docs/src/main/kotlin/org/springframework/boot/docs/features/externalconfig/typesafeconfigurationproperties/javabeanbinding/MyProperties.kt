/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.javabeanbinding

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.InetAddress

@ConfigurationProperties("my.service")
class MyProperties {

	var isEnabled = false

	var remoteAddress: InetAddress? = null

	val security = Security()

	class Security {

		var username: String? = null

		var password: String? = null

		var roles: List<String> = ArrayList(setOf("USER"))

	}

}

