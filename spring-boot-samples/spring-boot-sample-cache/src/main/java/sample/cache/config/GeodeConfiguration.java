/*
 * Copyright 2011-2016 the original author or authors.
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

package sample.cache.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;

/**
 * Spring {@link Configuration @Configuration} class to configure Geode as a caching provider.
 * This {@link Configuration @Configuration} class is conditionally loaded based on
 * whether Geode is on the classpath.
 *
 * @author John Blum
 * @see org.springframework.boot.autoconfigure.condition.ConditionalOnClass
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.ImportResource
 * @since 1.5.0
 */
@Configuration
@ImportResource("geode.xml")
@Profile("geode")
@SuppressWarnings("unused")
public class GeodeConfiguration {

}
