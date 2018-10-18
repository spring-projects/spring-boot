/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;

/**
 * {@link ConfigurableWebServerFactory} for Tomcat-specific features.
 *
 * @author Brian Clozel
 * @since 2.0.0
 * @see TomcatServletWebServerFactory
 * @see TomcatReactiveWebServerFactory
 */
public interface ConfigurableTomcatWebServerFactory extends ConfigurableWebServerFactory {

	/**
	 * Set the Tomcat base directory. If not specified a temporary directory will be used.
	 * @param baseDirectory the tomcat base directory
	 */
	void setBaseDirectory(File baseDirectory);

	/**
	 * Sets the background processor delay in seconds.
	 * @param delay the delay in seconds
	 */
	void setBackgroundProcessorDelay(int delay);

	/**
	 * Add {@link Valve}s that should be applied to the Tomcat {@link Engine}.
	 * @param engineValves the valves to add
	 */
	void addEngineValves(Valve... engineValves);

	/**
	 * Add {@link TomcatConnectorCustomizer}s that should be added to the Tomcat
	 * {@link Connector}.
	 * @param tomcatConnectorCustomizers the customizers to add
	 */
	void addConnectorCustomizers(TomcatConnectorCustomizer... tomcatConnectorCustomizers);

	/**
	 * Add {@link TomcatContextCustomizer}s that should be added to the Tomcat
	 * {@link Context}.
	 * @param tomcatContextCustomizers the customizers to add
	 */
	void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers);

	/**
	 * Set the character encoding to use for URL decoding. If not specified 'UTF-8' will
	 * be used.
	 * @param uriEncoding the uri encoding to set
	 */
	void setUriEncoding(Charset uriEncoding);

}
