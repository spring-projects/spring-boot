/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration.source

import org.springframework.boot.context.properties.ConfigurationPropertiesSource

@ConfigurationPropertiesSource
class Host {

	/**
	 * IP address to listen to.
	 */
	var ip: String = "127.0.0.1"

	/**
	 * Port to listener to.
	 */
	var port = 9797

}
