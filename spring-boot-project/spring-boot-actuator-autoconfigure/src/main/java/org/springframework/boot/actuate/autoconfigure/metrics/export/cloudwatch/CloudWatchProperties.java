/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.cloudwatch;

import org.springframework.boot.actuate.autoconfigure.metrics.export.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring CloudWatch metrics export.
 *
 * @author Jon Schneider
 * @author Dawid Kublik
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.export.cloudwatch")
public class CloudWatchProperties extends StepRegistryProperties {

	/**
	 * The namespace which will be used when sending
	 * metrics to CloudWatch. This property is needed and must not be null.
	 */
	private String namespace = "";


	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

}
