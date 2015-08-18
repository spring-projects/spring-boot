/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheStandaloneIntegrationTests.Application;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.samskivert.mustache.Mustache;

import static org.junit.Assert.assertEquals;

/**
 * Integration Tests for {@link MustacheAutoConfiguration} outside of a web application.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest({ "spring.main.web_environment=false", "env.foo=Heaven", "foo=World" })
public class MustacheStandaloneIntegrationTests {

	@Autowired
	private Mustache.Compiler compiler;

	@Test
	public void directCompilation() throws Exception {
		assertEquals(
				"Hello: World",
				this.compiler.compile("Hello: {{world}}").execute(
						Collections.singletonMap("world", "World")));
	}

	@Test
	public void environmentCollectorCompoundKey() throws Exception {
		assertEquals("Hello: Heaven", this.compiler.compile("Hello: {{env.foo}}")
				.execute(new Object()));
	}

	@Test
	public void environmentCollectorCompoundKeyStandard() throws Exception {
		assertEquals(
				"Hello: Heaven",
				this.compiler.standardsMode(true).compile("Hello: {{env.foo}}")
						.execute(new Object()));
	}

	@Test
	public void environmentCollectorSimpleKey() throws Exception {
		assertEquals("Hello: World",
				this.compiler.compile("Hello: {{foo}}").execute(new Object()));
	}

	@Configuration
	@Import({ MustacheAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static class Application {

	}

}
