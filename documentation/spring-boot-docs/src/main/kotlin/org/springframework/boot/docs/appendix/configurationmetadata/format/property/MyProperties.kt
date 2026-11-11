/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.format.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty

@ConfigurationProperties("my.app")
class MyProperties(val name: String?) {

	var target: String? = null
		@Deprecated("") @DeprecatedConfigurationProperty(replacement = "my.app.name", since = "1.2.0") get
		@Deprecated("") set

}

