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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.net.URL;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * <p>
 * Auto-configuration for Hazelcast Jet.
 * </p>
 * <p>
 * This is activated when Jet classes are present, the beans that would be auto-configured
 * are not already present, and Jet configuration is found.
 * </p>
 * <p>
 * Four rules apply:
 * </p>
 * <ul>
 * <li><b>1st equal</b> If only a Jet client configuration object exists, create a Jet
 * client.</li>
 * <li><b>1st equal</b> If only a Jet server configuration object exists, create a Jet
 * server.</li>
 * <li><b>3rd equal</b> If only a Jet client configuration XML file exists, create a Jet
 * client.</li>
 * <li><b>3rd equal</b> If Jet server configuration XML file exists, create a Jet server.
 * </li>
 * </ul>
 * <p>
 * A {@link JetInstance} always contains a {@link HazelcastInstance}. This
 * {@link HazelcastInstance} is also exposed as a bean to stop subsequent
 * auto-configuration from creating another {@link HazelcastInstance}. Usually a JVM
 * process will not want more than one {@link HazelcastInstance}.
 * </p>
 *
 * @author Neil Stevenson
 */
@AutoConfigureBefore({ HazelcastClientConfiguration.class,
		HazelcastServerConfiguration.class })
@ConditionalOnClass(JetInstance.class)
@ConditionalOnMissingBean({ HazelcastInstance.class, JetInstance.class })
public class HazelcastJetConfiguration {

	private static final Log logger = LogFactory.getLog(HazelcastJetConfiguration.class);

	static final String CONFIG_SYSTEM_PROPERTY_CLIENT = "hazelcast.client.config";
	static final String CONFIG_SYSTEM_PROPERTY_SERVER_IMDG = "hazelcast.config";
	static final String CONFIG_SYSTEM_PROPERTY_SERVER_JET = "hazelcast.jet.config";

	/*
	 * ----- Hazelcast Jet ----- Four mutually exclusive creators
	 */

	/**
	 * <p>
	 * Jet creation #1.
	 * </p>
	 * <p>
	 * If a client configuration object is available, and not a server configuration
	 * object, create a Jet client from it.
	 * </p>
	 */
	@Configuration
	@ConditionalOnSingleCandidate(ClientConfig.class)
	@ConditionalOnMissingBean(JetConfig.class)
	static class JetClientBeanBasedConfiguration {

		@Bean
		public JetInstance jetInstance(ClientConfig clientConfig) {
			logger.trace("JetClientBeanBasedConfiguration.JetInstance");
			return Jet.newJetClient(clientConfig);
		}

	}

	/**
	 * <p>
	 * Jet creation #2.
	 * </p>
	 * <p>
	 * The opposite of Jet creation #1. If a server configuration object is available and
	 * not a client configuration object, create a Jet server.
	 * </p>
	 */
	@Configuration
	@ConditionalOnSingleCandidate(JetConfig.class)
	@ConditionalOnMissingBean(ClientConfig.class)
	static class JetServerBeanBasedConfiguration {

		@Bean
		public JetInstance jetInstance(JetConfig jetConfig) {
			logger.trace("JetServerBeanBasedConfiguration.JetInstance");
			return Jet.newJetInstance(jetConfig);
		}

	}

	/**
	 * <p>
	 * Jet creation #3.
	 * </p>
	 * <p>
	 * If only the client configuration file is given, no server configuration files,
	 * create a Jet client from it
	 * </p>
	 */
	@Configuration
	@Conditional(JetClientConfigurationAvailableCondition.class)
	@ConditionalOnMissingBean({ ClientConfig.class, JetConfig.class })
	static class JetClientFileBasedConfiguration {

		@Bean
		public JetInstance jetInstance(HazelcastProperties hazelcastProperties)
				throws IOException {
			Resource config = hazelcastProperties.resolveConfigLocation();
			logger.trace(
					"JetClientFileBasedConfiguration.JetInstance, resource=" + config);
			if (config != null) {
				URL configUrl = config.getURL();
				ClientConfig clientConfig = new XmlClientConfigBuilder(configUrl).build();

				return Jet.newJetClient(clientConfig);
			}
			return Jet.newJetClient();
		}

	}

	/**
	 * <p>
	 * Jet creation #4.
	 * </p>
	 * <p>
	 * If either of the Jet server configuration files are given, no client configuration,
	 * create a Jet server with that file.
	 * </p>
	 */
	@Configuration
	@Conditional(JetServerConfigurationAvailableCondition.class)
	@ConditionalOnMissingBean({ ClientConfig.class, JetConfig.class })
	static class JetServerFileBasedConfiguration {

		@Bean
		public JetInstance jetInstance(HazelcastProperties hazelcastProperties)
				throws IOException {
			// TODO Split for hazelcast.xml v hazelcast-jet.xml
			Resource config = hazelcastProperties.resolveConfigLocation();
			logger.info(
					"JetServerFileBasedConfiguration.JetInstance, resource=" + config);
			if (config != null && config.isFile()) {
				JetConfig jetConfig = JetConfig.loadFromStream(config.getInputStream());

				return Jet.newJetInstance(jetConfig);
			}
			return Jet.newJetInstance();
		}

	}

	/* ----- Hazelcast IMDG ----- */

	/**
	 * <p>
	 * To avoid later auto-configuration, make it clear there is a
	 * {@link HazelcastInstance}. created internally.
	 * </p>
	 * <p>
	 * If the {@link JetInstance} is a server, the {@link HazelcastInstance} will be a
	 * server. If the {@link JetInstance} is a client, the {@link HazelcastInstance} will
	 * be a client.
	 * </p>
	 */
	@Configuration
	static class HazelcastInstanceFromJetInstance {

		/**
		 * <p>
		 * Expose inner {@link HazelcastInstance} as a bean.
		 * </p>
		 * @param jetInstance Created in this class
		 * @return {@link HazelcastInstance} inside the {@link JetInstance}
		 */
		@Bean
		public HazelcastInstance hazelcastInstance(JetInstance jetInstance) {
			return jetInstance.getHazelcastInstance();
		}

	}

	/* ----- Conditions based on config files, simple and compound ----- */

	// 'hazelcast-client.xml'
	static class ClientXMLAvailableCondition extends HazelcastConfigResourceCondition {

		ClientXMLAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY_CLIENT, "file:./hazelcast-client.xml",
					"classpath:/hazelcast-client.xml");
		}

	}

	// 'hazelcast.xml'
	static class IMDGServerXMLAvailableCondition
			extends HazelcastConfigResourceCondition {

		IMDGServerXMLAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY_SERVER_IMDG, "file:./hazelcast.xml",
					"classpath:/hazelcast.xml");
		}

	}

	// 'hazelcast-jet.xml'
	static class JetServerXMLAvailableCondition extends HazelcastConfigResourceCondition {

		JetServerXMLAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY_SERVER_JET, "file:./hazelcast-jet.xml",
					"classpath:/hazelcast-jet.xml");
		}

	}

	// Exclude 'hazelcast.xml' and exclude 'hazelcast-jet.xml'
	static class NoServerXMLAvailableCondition extends NoneNestedConditions {

		NoServerXMLAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Conditional(IMDGServerXMLAvailableCondition.class)
		static class IMDGServerXML {

		}

		@Conditional(JetServerXMLAvailableCondition.class)
		static class JetServerXML {

		}

	}

	// Exclude 'hazelcast-client.xml'
	static class NoClientXMLAvailableCondition extends NoneNestedConditions {

		NoClientXMLAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Conditional(ClientXMLAvailableCondition.class)
		static class ClientXML {

		}

	}

	// Include 'hazelcast.xml' or include 'hazelcast-jet.xml'
	static class ServerXMLAvailableCondition extends AnyNestedCondition {

		ServerXMLAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Conditional(IMDGServerXMLAvailableCondition.class)
		static class IMDGServerXML {

		}

		@Conditional(JetServerXMLAvailableCondition.class)
		static class JetServerXML {

		}

	}

	/**
	 * <p>
	 * "{@code hazelcast-client.xml}" but not ( "{@code hazelcast.xml}" or
	 * "{@code hazelcast-jet.xml}" ).
	 * </p>
	 */
	static class JetClientConfigurationAvailableCondition extends AllNestedConditions {

		JetClientConfigurationAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Conditional(ClientXMLAvailableCondition.class)
		static class Client {

		}

		@Conditional(NoServerXMLAvailableCondition.class)
		static class NoServer {

		}

	}

	/**
	 * <p>
	 * "{@code hazelcast-jet.xml}" or "{@code hazelcast.xml}" but not
	 * "{@code hazelcast-client.xml}".
	 * </p>
	 */
	static class JetServerConfigurationAvailableCondition extends AllNestedConditions {

		JetServerConfigurationAvailableCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@Conditional(NoClientXMLAvailableCondition.class)
		static class Client {

		}

		@Conditional(ServerXMLAvailableCondition.class)
		static class Server {

		}

	}

}
