/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.webservices;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link WebServicesProperties}.
 *
 * @author Madhura Bhave
 */
public class WebServicesPropertiesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private WebServicesProperties properties;

	@Test
	public void pathMustNotBeEmpty() {
		this.properties = new WebServicesProperties();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Path must have length greater than 1");
		this.properties.setPath("");
	}

	@Test
	public void pathMustHaveLengthGreaterThanOne() {
		this.properties = new WebServicesProperties();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Path must have length greater than 1");
		this.properties.setPath("/");
	}

	@Test
	public void customPathMustBeginWithASlash() {
		this.properties = new WebServicesProperties();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Path must start with '/'");
		this.properties.setPath("custom");
	}

}
