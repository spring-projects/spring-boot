/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.boot.test;

/**
 * Manipulate the TestContext to merge properties from {@code @IntegrationTest}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.2.0
 * @deprecated since 1.4.0 in favor of IntegrationTestPropertiesListener
 */
@Deprecated
public class IntegrationTestPropertiesListener
		extends org.springframework.boot.test.context.IntegrationTestPropertiesListener {

}
