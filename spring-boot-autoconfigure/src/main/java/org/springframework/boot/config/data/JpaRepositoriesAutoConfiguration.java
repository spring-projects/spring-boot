/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.config.data;

import org.springframework.boot.config.EnableAutoConfiguration;
import org.springframework.boot.strap.context.condition.ConditionalOnClass;
import org.springframework.boot.strap.context.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's JPA Repositories.
 * 
 * @author Phillip Webb
 * @see EnableJpaRepositories
 */
@Configuration
@ConditionalOnClass(JpaRepository.class)
@ConditionalOnMissingBean(JpaRepositoryFactoryBean.class)
@Import(JpaRepositoriesAutoConfigureRegistrar.class)
public class JpaRepositoriesAutoConfiguration {

}
