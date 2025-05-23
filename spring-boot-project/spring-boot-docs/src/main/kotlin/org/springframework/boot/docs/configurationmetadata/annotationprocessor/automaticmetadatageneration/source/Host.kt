package org.springframework.boot.docs.configurationmetadata.annotationprocessor.automaticmetadatageneration.source

import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@ConfigurationPropertiesScan
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
