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

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for {@link ReactiveWebServerFactory} implementations.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public abstract class AbstractReactiveWebServerFactory extends
		AbstractConfigurableReactiveWebServer implements ReactiveWebServerFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	public AbstractReactiveWebServerFactory() {
	}

	public AbstractReactiveWebServerFactory(int port) {
		super(port);
	}

	/**
	 * Return the absolute temp dir for given web server.
	 * @param prefix servlet container name
	 * @return The temp dir for given servlet container.
	 */
	protected File createTempDir(String prefix) {
		try {
			File tempDir = File.createTempFile(prefix + ".", "." + getPort());
			tempDir.delete();
			tempDir.mkdir();
			tempDir.deleteOnExit();
			return tempDir;
		}
		catch (IOException ex) {
			throw new EmbeddedWebServerException(
					"Unable to create tempDir. java.io.tmpdir is set to "
							+ System.getProperty("java.io.tmpdir"),
					ex);
		}
	}

}
