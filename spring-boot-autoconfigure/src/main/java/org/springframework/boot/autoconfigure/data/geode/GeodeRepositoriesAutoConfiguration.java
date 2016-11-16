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

package org.springframework.boot.autoconfigure.data.geode;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.client.ClientCache;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.config.GemfireRepositoryConfigurationExtension;
import org.springframework.data.gemfire.repository.support.GemfireRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Geode Repositories.
 * <p>
 * Activates when there is a bean of type {@link Cache} or {@link ClientCache} configured
 * in the Spring context, the Spring Data Geode
 * {@link org.springframework.data.gemfire.repository.GemfireRepository}
 * type is on the classpath, and there is no other, existing
 * {@link org.springframework.data.gemfire.repository.GemfireRepository}
 * configured.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling Geode Repositories
 * using the
 * {@link org.springframework.data.gemfire.repository.config.EnableGemfireRepositories}
 * annotation.
 *
 * @author John Blum
 * @since 1.5.0
 * @see org.springframework.boot.autoconfigure.data.geode.GeodeRepositoriesAutoConfigureRegistrar
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.data.gemfire.repository.config.EnableGemfireRepositories
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.client.ClientCache
 */
@Configuration
@ConditionalOnBean({ Cache.class, ClientCache.class })
@ConditionalOnClass(GemfireRepository.class)
@ConditionalOnMissingBean({ GemfireRepositoryConfigurationExtension.class, GemfireRepositoryFactoryBean.class })
@ConditionalOnProperty(prefix = "spring.data.geode.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(GeodeRepositoriesAutoConfigureRegistrar.class)
public class GeodeRepositoriesAutoConfiguration {

}
