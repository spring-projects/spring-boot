/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.scan.valid.b;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
public class BScanConfiguration {

	public interface BProperties {

	}

	@ConstructorBinding
	@ConfigurationProperties(prefix = "b.first")
	public static class BFirstProperties implements BProperties {

		private final String name;

		public BFirstProperties(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	@ConfigurationProperties(prefix = "b.second")
	public static class BSecondProperties implements BProperties {

		private int number;

		public int getNumber() {
			return this.number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

	}

}
