/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.solr;

import org.apache.solr.client.solrj.SolrClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.solr.repository.SolrRepository;
import org.springframework.data.solr.repository.config.SolrRepositoryConfigExtension;
import org.springframework.data.solr.repository.support.SolrRepositoryFactoryBean;

/**
 * Enables auto configuration for Spring Data Solr repositories.
 * <p>
 * Activates when there is no bean of type {@link SolrRepositoryFactoryBean} found in
 * context, and both {@link SolrRepository} and {@link SolrClient} can be found on
 * classpath.
 * </p>
 * If active auto configuration does the same as
 * {@link org.springframework.data.solr.repository.config.EnableSolrRepositories} would
 * do.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ SolrClient.class, SolrRepository.class })
@ConditionalOnMissingBean({ SolrRepositoryFactoryBean.class,
		SolrRepositoryConfigExtension.class })
@ConditionalOnProperty(prefix = "spring.data.solr.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(SolrRepositoriesRegistrar.class)
public class SolrRepositoriesAutoConfiguration {

}
