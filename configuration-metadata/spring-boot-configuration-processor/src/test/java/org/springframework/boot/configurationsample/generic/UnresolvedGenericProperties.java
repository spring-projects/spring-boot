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

package org.springframework.boot.configurationsample.generic;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Properties with unresolved generic information.
 *
 * @param <B> mapping key type
 * @param <C> mapping value type
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("generic")
public class UnresolvedGenericProperties<B extends Number, C> extends AbstractGenericProperties<String, B, C> {

}
