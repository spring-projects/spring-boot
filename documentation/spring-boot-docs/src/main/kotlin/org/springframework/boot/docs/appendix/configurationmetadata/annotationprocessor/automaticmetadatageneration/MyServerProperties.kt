/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.annotationprocessor.automaticmetadatageneration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("my.server")
class MyServerProperties(

	/**
	 * Name of the server.
	 */
	var name: String,

	/**
	 * IP address to listen to.
	 */
	var ip: String = "127.0.0.1",

	/**
	 * Port to listen to.
	 */
	var port: Int = 9797)

