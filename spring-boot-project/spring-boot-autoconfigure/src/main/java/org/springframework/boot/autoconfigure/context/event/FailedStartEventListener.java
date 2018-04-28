/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.context.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Event listener to catch all {@link ApplicationFailedEvent}'s and grab the sources and main application class for further
 * analysis in failure analyzers.
 *
 * @author Dan King
 */
@Component
public class FailedStartEventListener implements ApplicationListener<ApplicationFailedEvent> {

	private static final Logger log = LoggerFactory.getLogger(FailedStartEventListener.class);

	@Override
	public void onApplicationEvent(ApplicationFailedEvent event) {
		SpringApplication springApplication = event.getSpringApplication();
		FailedStartupInfoHolder.setAllSources(springApplication.getAllSources());
		FailedStartupInfoHolder.setMainApplication(springApplication.getMainApplicationClass());
		log.warn("Failed to start.");
	}

}
