/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.cache;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.cache.JCacheCacheMeterBinderProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JCacheCacheMeterBinderProviderBackOffTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CacheMetricsAutoConfiguration.class));

	@Test
	void backsOffWhenUserProvidesProvider() {
		JCacheCacheMeterBinderProvider custom = new JCacheCacheMeterBinderProvider();
		this.contextRunner.withBean(JCacheCacheMeterBinderProvider.class, () -> custom).run((context) -> {
			var bean = context.getBean(JCacheCacheMeterBinderProvider.class);
			assertThat(bean).isSameAs(custom);
		});
	}

}
