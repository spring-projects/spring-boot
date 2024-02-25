/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.integration;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * ServiceProperties class.
 */
@ConfigurationProperties(prefix = "service", ignoreUnknownFields = false)
@ManagedResource
public class ServiceProperties {

	private String greeting = "Hello";

	private File inputDir;

	private File outputDir;

	/**
	 * Retrieves the greeting message.
	 * @return The greeting message.
	 */
	@ManagedAttribute
	public String getGreeting() {
		return this.greeting;
	}

	/**
	 * Sets the greeting message.
	 * @param greeting the new greeting message
	 */
	public void setGreeting(String greeting) {
		this.greeting = greeting;
	}

	/**
	 * Returns the input directory.
	 * @return the input directory
	 */
	public File getInputDir() {
		return this.inputDir;
	}

	/**
	 * Sets the input directory for the service properties.
	 * @param inputDir the input directory to be set
	 */
	public void setInputDir(File inputDir) {
		this.inputDir = inputDir;
	}

	/**
	 * Returns the output directory.
	 * @return the output directory
	 */
	public File getOutputDir() {
		return this.outputDir;
	}

	/**
	 * Sets the output directory for the ServiceProperties class.
	 * @param outputDir the output directory to be set
	 */
	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

}
