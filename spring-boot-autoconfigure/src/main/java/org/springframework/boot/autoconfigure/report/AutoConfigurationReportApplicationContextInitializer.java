/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.report;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationErrorHandler;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Dave Syer
 */
public class AutoConfigurationReportApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>,
		SpringApplicationErrorHandler {

	private AutoConfigurationReport report;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		this.report = AutoConfigurationReport.registerReport(applicationContext,
				beanFactory);
	}

	@Override
	public void handle(SpringApplication springApplication, String[] args, Throwable e) {
		if (this.report != null) {
			this.report.initialize(); // salvage a report if possible
		}
	}

}
