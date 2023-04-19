package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.enablingannotatedtypes

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("some.properties")
class SomeProperties