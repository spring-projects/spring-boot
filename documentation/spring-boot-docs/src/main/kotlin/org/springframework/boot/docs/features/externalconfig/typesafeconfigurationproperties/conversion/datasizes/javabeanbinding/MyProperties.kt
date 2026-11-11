/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.conversion.datasizes.javabeanbinding

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit

@ConfigurationProperties("my")
class MyProperties {

	@DataSizeUnit(DataUnit.MEGABYTES)
	var bufferSize = DataSize.ofMegabytes(2)

	var sizeThreshold = DataSize.ofBytes(512)

}

