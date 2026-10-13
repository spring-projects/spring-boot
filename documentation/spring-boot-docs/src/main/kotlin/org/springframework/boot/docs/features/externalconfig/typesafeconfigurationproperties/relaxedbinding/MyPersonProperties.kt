/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.relaxedbinding

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("my.main-project.person")
class MyPersonProperties {

	var firstName: String? = null

}

