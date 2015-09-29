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

package org.springframework.boot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link ManifestReader}.
 *
 * @author Arnost Havelka
 */
public class ManifestReaderTests {

	private static final String SPRING_CONTEXT = "spring-context";
	private ManifestReader mr;

	@Before
	public void init() {
		mr = new ManifestReader();
	}
	
	@Test
	public void verifyManifestReader() {
		Map<String, String> versions = mr.getVersions();
		String value = versions.get(SPRING_CONTEXT);
		assertThat(value, not(isEmptyString()));
		assertThat(value, startsWith(SPRING_CONTEXT));
	}

}
