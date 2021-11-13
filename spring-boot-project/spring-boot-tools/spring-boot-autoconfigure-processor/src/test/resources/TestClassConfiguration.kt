/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor

import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication

/**
 * @author Pavel Pletnev
 */
@ConditionalOnClass(name = ["org.springframework.foo", "java.io.InputStream"], value = [TestClassConfiguration.Nested::class])
@ConditionalOnBean(type = ["java.io.OutputStream"])
@ConditionalOnSingleCandidate(type = "java.io.OutputStream")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class TestClassConfiguration {
	@AutoConfigureOrder
	internal class Nested
}