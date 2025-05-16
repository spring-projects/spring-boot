package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.relaxedbinding.mapsfromenvironmentvariables

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("my.props")
class MyMapsProperties {

	val values: Map<String, String> = HashMap()

}
