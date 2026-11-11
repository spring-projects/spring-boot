/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.integration;

import java.io.File;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

@ConfigurationProperties(prefix = "service", ignoreUnknownFields = false)
@ManagedResource
public class ServiceProperties {

	private String greeting = "Hello";

	private @Nullable File inputDir;

	private @Nullable File outputDir;

	@ManagedAttribute
	public String getGreeting() {
		return this.greeting;
	}

	public void setGreeting(String greeting) {
		this.greeting = greeting;
	}

	public @Nullable File getInputDir() {
		return this.inputDir;
	}

	public void setInputDir(@Nullable File inputDir) {
		this.inputDir = inputDir;
	}

	public @Nullable File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(@Nullable File outputDir) {
		this.outputDir = outputDir;
	}

}
