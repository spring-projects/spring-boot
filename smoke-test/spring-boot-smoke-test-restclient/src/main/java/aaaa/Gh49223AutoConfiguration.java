package aaaa;

import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration(afterName = { "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration",
		"org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration" })
class Gh49223AutoConfiguration {

	// Class must be in a package name that is ordered early

}