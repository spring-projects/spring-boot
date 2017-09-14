/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityProperties}.
 *
 * @author Dave Syer
 */
public class SecurityPropertiesTests {

	private SecurityProperties security = new SecurityProperties();

	@Test
	public void testBinding() {
		bind("spring.security.filter.order", "55");
		assertThat(this.security.getFilter().getOrder()).isEqualTo(55);
	}

	private void bind(String name, String value) {
		bind(new MapConfigurationPropertySource(Collections.singletonMap(name, value)));
	}

	private void bind(ConfigurationPropertySource source) {
		new Binder(source).bind("spring.security", Bindable.ofInstance(this.security));
	}

}
