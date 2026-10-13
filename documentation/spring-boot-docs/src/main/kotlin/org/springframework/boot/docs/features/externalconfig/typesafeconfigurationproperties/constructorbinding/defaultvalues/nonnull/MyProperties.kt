/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.constructorbinding.defaultvalues.nonnull

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.net.InetAddress

@ConfigurationProperties("my.service")
// tag::code[]
class MyProperties(val enabled: Boolean, val remoteAddress: InetAddress,
		@DefaultValue val security: Security) {

	class Security(val username: String?, val password: String?,
			@param:DefaultValue("USER") val roles: List<String>)

}
// end::code[]

