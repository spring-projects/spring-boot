/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.rest;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Rest's MVC
 * integration.
 * <p>
 * Activates when the application is a web application and no
 * {@link RepositoryRestMvcConfiguration} is found.
 * </p>
 * <p>
 * Once in effect, the auto-configuration is the equivalent of importing the
 * {@link RepositoryRestMvcConfiguration}.
 * </p>
 * 
 * @author Rob Winch
 * @since 1.1.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnMissingBean(RepositoryRestMvcConfiguration.class)
@ConditionalOnClass(RepositoryRestMvcConfiguration.class)
@Import(RepositoryRestMvcConfiguration.class)
public class RepositoryRestMvcAutoConfiguration {

}
