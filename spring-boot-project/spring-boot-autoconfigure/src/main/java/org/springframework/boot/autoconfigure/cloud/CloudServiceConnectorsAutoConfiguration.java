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

package org.springframework.boot.autoconfigure.cloud;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.cloud.config.java.CloudScan;
import org.springframework.cloud.config.java.CloudScanConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Cloud Service Connectors.
 * <p>
 * Activates when there is no bean of type {@link Cloud} and the "cloud" profile is
 * active.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of adding the
 * {@link CloudScan @CloudScan} annotation in one of the configuration file. Specifically,
 * it adds a bean for each service bound to the application and one for
 * {@link ApplicationInstanceInfo}.
 *
 * @author Ramnivas Laddad
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile("cloud")
@AutoConfigureOrder(CloudServiceConnectorsAutoConfiguration.ORDER)
@ConditionalOnClass(CloudScanConfiguration.class)
@ConditionalOnMissingBean(Cloud.class)
@Import(CloudScanConfiguration.class)
public class CloudServiceConnectorsAutoConfiguration {

	/**
	 * The order for cloud configuration. Cloud configurations need to happen early (so
	 * that they run before data, mongo, etc.).
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

	private static final Log logger = LogFactory.getLog(CloudServiceConnectorsAutoConfiguration.class);

	public CloudServiceConnectorsAutoConfiguration() {
		logger.warn("Support for Spring Cloud Connectors has been deprecated in favor of Java CFEnv");
	}

}
