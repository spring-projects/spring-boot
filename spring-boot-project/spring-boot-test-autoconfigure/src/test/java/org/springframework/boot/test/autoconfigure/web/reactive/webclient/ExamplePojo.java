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

package org.springframework.boot.test.autoconfigure.web.reactive.webclient;

import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

/**
 * Example POJO used with {@link WebFluxTest} tests.
 *
 * @author Andy Wilkinson
 */
public class ExamplePojo {

	private final String alpha;

	private final String bravo;

	public ExamplePojo(String alpha, String bravo) {
		this.alpha = alpha;
		this.bravo = bravo;
	}

	public String getAlpha() {
		return this.alpha;
	}

	public String getBravo() {
		return this.bravo;
	}

}
