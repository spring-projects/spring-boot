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

package org.springframework.boot.autoconfigure.jms.artemis;

import java.io.File;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Configuration used to create the embedded Artemis server.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ArtemisEmbeddedConfigurationFactory {

	private static final Log logger = LogFactory
			.getLog(ArtemisEmbeddedConfigurationFactory.class);

	private final ArtemisProperties.Embedded properties;

	ArtemisEmbeddedConfigurationFactory(ArtemisProperties properties) {
		this.properties = properties.getEmbedded();
	}

	public Configuration createConfiguration() {
		ConfigurationImpl configuration = new ConfigurationImpl();
		configuration.setSecurityEnabled(false);
		configuration.setPersistenceEnabled(this.properties.isPersistent());
		String dataDir = getDataDir();
		configuration.setJournalDirectory(dataDir + "/journal");
		if (this.properties.isPersistent()) {
			configuration.setJournalType(JournalType.NIO);
			configuration.setLargeMessagesDirectory(dataDir + "/largemessages");
			configuration.setBindingsDirectory(dataDir + "/bindings");
			configuration.setPagingDirectory(dataDir + "/paging");
		}
		TransportConfiguration transportConfiguration = new TransportConfiguration(
				InVMAcceptorFactory.class.getName(),
				this.properties.generateTransportParameters());
		configuration.getAcceptorConfigurations().add(transportConfiguration);
		if (this.properties.isDefaultClusterPassword()) {
			logger.debug("Using default Artemis cluster password: "
					+ this.properties.getClusterPassword());
		}
		configuration.setClusterPassword(this.properties.getClusterPassword());
		return configuration;
	}

	private String getDataDir() {
		if (this.properties.getDataDirectory() != null) {
			return this.properties.getDataDirectory();
		}
		String tempDirectory = System.getProperty("java.io.tmpdir");
		return new File(tempDirectory, "artemis-data").getAbsolutePath();
	}

}
