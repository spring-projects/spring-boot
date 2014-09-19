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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ResourceBanner}.
 *
 * @author Phillip Webb
 */
public class ResourceBannerTests {

	@Test
	public void renderWithReplacement() throws Exception {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		ResourceBanner banner = new ResourceBanner(resource);
		ConfigurableEnvironment environment = new MockEnvironment();
		Map<String, Object> source = Collections.<String, Object> singletonMap("a", "1");
		environment.getPropertySources().addLast(new MapPropertySource("map", source));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		assertThat(out.toString(), startsWith("banner 1"));
		assertThat(out.toString(), not(containsString("$")));
	}

}
