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

package org.springframework.boot.devtools.autoconfigure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.core.log.LogMessage;

/**
 * Manages an optional {@link LiveReloadServer}. The {@link LiveReloadServer} may
 * gracefully fail to start (e.g. because of a port conflict) or may be omitted entirely.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class OptionalLiveReloadServer implements InitializingBean {

	private static final Log logger = LogFactory.getLog(OptionalLiveReloadServer.class);

	private LiveReloadServer server;

	/**
	 * Create a new {@link OptionalLiveReloadServer} instance.
	 * @param server the server to manage or {@code null}
	 */
	public OptionalLiveReloadServer(LiveReloadServer server) {
		this.server = server;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		startServer();
	}

	void startServer() throws Exception {
		if (this.server != null) {
			try {
				if (!this.server.isStarted()) {
					this.server.start();
				}
				logger.info(LogMessage.format("LiveReload server is running on port %s", this.server.getPort()));
			}
			catch (Exception ex) {
				logger.warn("Unable to start LiveReload server");
				logger.debug("Live reload start error", ex);
				this.server = null;
			}
		}
	}

	/**
	 * Trigger LiveReload if the server is up and running.
	 */
	public void triggerReload() {
		if (this.server != null) {
			this.server.triggerReload();
		}
	}

}
