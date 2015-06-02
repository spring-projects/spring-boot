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

package org.springframework.boot.developertools;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.developertools.RemoteUrlPropertyExtractor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link RemoteUrlPropertyExtractor}.
 *
 * @author Phillip Webb
 */
public class RemoteUrlPropertyExtractorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void missingUrl() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("No remote URL specified");
		doTest();
	}

	@Test
	public void malformedUrl() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Malformed URL '::://wibble'");
		doTest("::://wibble");

	}

	@Test
	public void multipleUrls() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Multiple URLs specified");
		doTest("http://localhost:8080", "http://localhost:9090");
	}

	@Test
	public void validUrl() throws Exception {
		ApplicationContext context = doTest("http://localhost:8080");
		assertThat(context.getEnvironment().getProperty("remoteUrl"),
				equalTo("http://localhost:8080"));
	}

	private ApplicationContext doTest(String... args) {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.addListeners(new RemoteUrlPropertyExtractor());
		return application.run(args);
	}

	@Configuration
	static class Config {

	}

}
