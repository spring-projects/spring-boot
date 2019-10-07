package org.springframework.boot.docs.context.properties.bind.kotlin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit

/**
 * A [@ConfigurationProperties][ConfigurationProperties] example that uses
 * [DataSize].
 *
 * @author Ibanga Enoobong Ime
 */
// tag::example[]
@ConfigurationProperties("app.io")
class AppIoProperties {

	@DataSizeUnit(DataUnit.MEGABYTES)
	var bufferSize = DataSize.ofMegabytes(2)

	var sizeThreshold = DataSize.ofBytes(512)

}
// end::example[]
