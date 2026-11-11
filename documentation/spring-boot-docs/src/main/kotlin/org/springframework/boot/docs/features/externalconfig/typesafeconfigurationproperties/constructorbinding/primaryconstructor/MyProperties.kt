/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.constructorbinding.primaryconstructor

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("my")
class MyProperties() {

	constructor(name: String) : this() {
		this.name = name
	}

	// @fold:on // vars...
	var name: String? = null
	// @fold:off

}

