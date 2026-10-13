/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration.nestedproperties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("my.server")
class MyServerProperties(
	var name: String,
	var host: Host) {

	class Host(val ip: String, val port: Int = 0)

}

