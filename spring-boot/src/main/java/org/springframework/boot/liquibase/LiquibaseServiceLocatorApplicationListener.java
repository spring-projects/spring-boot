/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.liquibase;

import liquibase.servicelocator.CustomResolverServiceLocator;
import liquibase.servicelocator.ServiceLocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ClassUtils;

/**
 * {@link ApplicationListener} that replaces the liquibase {@link ServiceLocator} with a
 * version that works with Spring Boot executable archives.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class LiquibaseServiceLocatorApplicationListener
		implements ApplicationListener<ApplicationStartedEvent> {

	static final Log logger = LogFactory
			.getLog(LiquibaseServiceLocatorApplicationListener.class);

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		if (ClassUtils.isPresent("liquibase.servicelocator.ServiceLocator", null)) {
			new LiquibasePresent().replaceServiceLocator();
		}
	}

	/**
	 * Inner class to prevent class not found issues.
	 */
	private static class LiquibasePresent {

		public void replaceServiceLocator() {
			ServiceLocator.getInstance().addPackageToScan(
					CommonsLoggingLiquibaseLogger.class.getPackage().getName());
			ServiceLocator.setInstance(new CustomResolverServiceLocator(
					new SpringPackageScanClassResolver(logger)));
		}

	}

}
