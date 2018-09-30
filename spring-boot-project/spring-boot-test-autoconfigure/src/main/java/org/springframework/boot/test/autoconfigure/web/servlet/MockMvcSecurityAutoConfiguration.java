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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Spring Security's testing support.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.test.mockmvc", name = "secure", havingValue = "true", matchIfMissing = true)
@Import({ SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class,
		MockMvcSecurityConfiguration.class })
public class MockMvcSecurityAutoConfiguration {

}
