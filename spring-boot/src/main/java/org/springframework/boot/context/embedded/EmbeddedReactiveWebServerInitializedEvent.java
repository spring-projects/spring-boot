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

package org.springframework.boot.context.embedded;

/**
 * Event to be published after the {@link ReactiveWebApplicationContext} is
 * refreshed and the {@link EmbeddedWebServer} is ready. Useful for
 * obtaining the local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class EmbeddedReactiveWebServerInitializedEvent extends EmbeddedWebServerInitializedEvent {

	private final ReactiveWebApplicationContext applicationContext;

	public EmbeddedReactiveWebServerInitializedEvent(
			EmbeddedWebServer source,
			ReactiveWebApplicationContext applicationContext) {
		super(source);
		this.applicationContext = applicationContext;
	}

	@Override
	public ReactiveWebApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public String getServerId() {
		return "server";
	}
}
