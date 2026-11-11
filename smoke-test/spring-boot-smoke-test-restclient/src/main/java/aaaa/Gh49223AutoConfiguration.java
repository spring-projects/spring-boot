/*
 * Copyright 2012-present the original author or authors.
 */

package aaaa;

import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration(afterName = { "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration",
		"org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration" })
public final class Gh49223AutoConfiguration {

	// Class must be in a package name that is ordered early

}
