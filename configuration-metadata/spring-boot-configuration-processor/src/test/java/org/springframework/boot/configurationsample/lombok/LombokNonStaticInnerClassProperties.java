/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationsample.lombok;

import lombok.Data;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Lombok properties with non-static nested classes.
 *
 * @author Daeho Kwon
 */
@Data
@TestConfigurationProperties("lombok-non-static-inner")
@SuppressWarnings("unused")
public class LombokNonStaticInnerClassProperties {

	// non-final → @Data generates setter → non-static + setter → no metadata
	private InnerWithSetter innerWithSetter;

	// final → @Data does not generate setter → non-static + no setter → metadata generated
	private final InnerWithoutSetter innerWithoutSetter = new InnerWithoutSetter();

	@Data
	public class InnerWithSetter {

		private String value;

	}

	@Data
	public class InnerWithoutSetter {

		private String value;

	}

}
