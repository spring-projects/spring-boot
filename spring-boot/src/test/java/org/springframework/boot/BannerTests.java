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

package org.springframework.boot;

import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link Banner} and its usage by {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Michael Stummvoll
 */
@Configuration
public class BannerTests {

	static class DummyBanner implements Banner {

		private int writeCount;

		@Override
		public void write(PrintStream out) {
			out.println("Dummy Banner");
			++writeCount;
		}

	}

	@Test
	public void testCustomBanner() throws Exception {
		SpringApplication app = new SpringApplication(BannerTests.class);
		app.setWebEnvironment(false);
		DummyBanner dummyBanner = new DummyBanner();
		app.setBanner(dummyBanner);
		app.run();
		Assert.assertEquals(1, dummyBanner.writeCount);
	}

}
