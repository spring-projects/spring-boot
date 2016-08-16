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

package org.springframework.boot.autoconfigure.cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.spring.SpringCacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.ignite.IgniteAutoConfiguration;
import org.springframework.boot.autoconfigure.ignite.IgniteConfigResourceCondition;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Apache Ignite cache configuration. Can either reuse the {@link Ignite} that has been
 * configured by the general {@link IgniteAutoConfiguration} or create a separate one if
 * the {@code spring.cache.ignite.config} property has been set.
 * <p>
 * If the {@link IgniteAutoConfiguration} has bean disabled, an attempt to configure a
 * default {@link Ignite} is still made, using the same defaults.
 *
 * @author wmz7year
 * @since 1.4.1
 * @see IgniteConfigResourceCondition
 */
@Configuration
@ConditionalOnClass({ Ignite.class, SpringCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional(CacheCondition.class)
@Import({ IgniteInstanceConfiguration.Existing.class,
		IgniteInstanceConfiguration.Specific.class })
class IgniteCacheConfiguration {

}
