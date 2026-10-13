/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.conversion.datasizes.constructorbinding

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit

@ConfigurationProperties("my")
class MyProperties(@param:DataSizeUnit(DataUnit.MEGABYTES) @param:DefaultValue("2MB") val bufferSize: DataSize,
		@param:DefaultValue("512B") val sizeThreshold: DataSize)

