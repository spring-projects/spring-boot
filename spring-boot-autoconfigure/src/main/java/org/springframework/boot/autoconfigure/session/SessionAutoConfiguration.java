/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.SessionConfigurationImportSelector;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session.
 *
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(Session.class)
@ConditionalOnWebApplication
@ConditionalOnMissingBean(SessionRepository.class)
@EnableConfigurationProperties(SessionProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HazelcastAutoConfiguration.class,
		MongoAutoConfiguration.class, RedisAutoConfiguration.class })
@Import(SessionConfigurationImportSelector.class)
public class SessionAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(value = ServerProperties.class, search = SearchStrategy.CURRENT)
	// Just in case user switches off ServerPropertiesAutoConfiguration
	public static class ServerPropertiesConfiguration {

		@Bean
		// Use the same bean name as the default one for any old webapp
		public ServerProperties serverProperties() {
			return new ServerProperties();
		}

	}

	/**
	 * {@link ImportSelector} to add {@link StoreType} configuration classes.
	 */
	static class SessionConfigurationImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			StoreType[] types = StoreType.values();
			String[] imports = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				imports[i] = SessionStoreMappings.getConfigurationClass(types[i]);
			}
			return imports;
		}

	}

}
