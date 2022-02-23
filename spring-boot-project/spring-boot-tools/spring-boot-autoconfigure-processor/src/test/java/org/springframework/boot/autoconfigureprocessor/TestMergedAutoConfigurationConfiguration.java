/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Test @AutoConfiguration aliases together with @AutoConfigureBefore
 * and @AutoConfigureAfter.
 *
 * @author Moritz Halbritter
 */
@TestAutoConfigureBefore(value = InputStream.class, name = { "test.before1", "test.before2" })
@TestAutoConfigureAfter(value = OutputStream.class, name = { "test.after1", "test.after2" })
@TestAutoConfiguration(before = ObjectInputStream.class, beforeName = { "test.before3", "test.before4" },
		after = ObjectOutputStream.class, afterName = { "test.after3", "test.after4" })
class TestMergedAutoConfigurationConfiguration {

}
