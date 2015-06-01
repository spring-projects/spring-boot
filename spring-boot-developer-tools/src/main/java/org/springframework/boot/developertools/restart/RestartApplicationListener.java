/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.restart;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * {@link ApplicationListener} to initialize the {@link Restarter}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see Restarter
 */
public class RestartApplicationListener implements ApplicationListener<ApplicationEvent>,
		Ordered {

	private int order = HIGHEST_PRECEDENCE;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartedEvent) {
			Restarter.initialize(((ApplicationStartedEvent) event).getArgs());
		}
		if (event instanceof ApplicationReadyEvent
				|| event instanceof ApplicationFailedEvent) {
			Restarter.getInstance().finish();
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order of the listener.
	 * @param order the order of the listener
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
