/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Arrays

@ConfigurationProperties("my.messaging")
class MyMessagingProperties(

	val addresses: List<String> = ArrayList(Arrays.asList("a", "b")),

	var containerType: ContainerType = ContainerType.SIMPLE) {

	enum class ContainerType {
		SIMPLE, DIRECT
	}
}

