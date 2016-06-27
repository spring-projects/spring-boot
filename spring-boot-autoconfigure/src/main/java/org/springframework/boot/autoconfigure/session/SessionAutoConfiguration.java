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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.SessionConfigurationImportSelector;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.SessionRepositoryValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Session.
 *
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnMissingBean(SessionRepository.class)
@ConditionalOnClass(Session.class)
@ConditionalOnWebApplication
@EnableConfigurationProperties(SessionProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HazelcastAutoConfiguration.class,
		MongoAutoConfiguration.class, RedisAutoConfiguration.class })
@Import({ SessionConfigurationImportSelector.class, SessionRepositoryValidator.class })
public class SessionAutoConfiguration {

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

	/**
	 * Bean used to validate that a {@link SessionRepository} exists and provide a
	 * meaningful message if that's not the case.
	 */
	static class SessionRepositoryValidator {

		private SessionProperties sessionProperties;

		private ObjectProvider<SessionRepository<?>> sessionRepositoryProvider;

		SessionRepositoryValidator(SessionProperties sessionProperties,
				ObjectProvider<SessionRepository<?>> sessionRepositoryProvider) {
			this.sessionProperties = sessionProperties;
			this.sessionRepositoryProvider = sessionRepositoryProvider;
		}

		@PostConstruct
		public void checkSessionRepository() {
			StoreType storeType = this.sessionProperties.getStoreType();
			if (storeType != StoreType.NONE) {
				Assert.notNull(this.sessionRepositoryProvider.getIfAvailable(),
						"No session repository could be auto-configured, check your "
								+ "configuration (session store type is '" + storeType
								+ "')");
			}
		}

	}

}
