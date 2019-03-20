/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.net.URL;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JvmUtils}.
 *
 * @author Phillip Webb
 */
public class JvmUtilsTests {

	@Test
	public void getToolsJar() throws Exception {
		URL jarUrl = JvmUtils.getToolsJarUrl();
		assertThat(jarUrl.toString()).endsWith(".jar");
		assertThat(new File(jarUrl.toURI()).exists()).isTrue();
	}

}
