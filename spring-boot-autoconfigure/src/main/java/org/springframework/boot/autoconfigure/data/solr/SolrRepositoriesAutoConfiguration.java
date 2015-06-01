/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.solr;

import org.apache.solr.client.solrj.SolrServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.solr.repository.SolrRepository;

/**
 * Enables auto configuration for Spring Data Solr repositories.
 * <p>
 * Activates when there is no bean of type
 * {@link org.springframework.data.solr.repository.support.SolrRepositoryFactoryBean}
 * found in context, and both
 * {@link org.springframework.data.solr.repository.SolrRepository} and
 * {@link org.apache.solr.client.solrj.SolrServer} can be found on classpath.
 * </p>
 * If active auto configuration does the same as
 * {@link org.springframework.data.solr.repository.config.EnableSolrRepositories} would
 * do.
 *
 * @author Christoph Strobl
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ SolrServer.class, SolrRepository.class })
@ConditionalOnMissingBean(RepositoryFactoryBeanSupport.class)
@ConditionalOnExpression("${spring.data.solr.repositories.enabled:true}")
@Import(SolrRepositoriesAutoConfigureRegistrar.class)
public class SolrRepositoriesAutoConfiguration {

}
