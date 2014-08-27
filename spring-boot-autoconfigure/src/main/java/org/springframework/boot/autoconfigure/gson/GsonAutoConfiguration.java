/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonFactoryBean;

import com.google.gson.Gson;

/**
 * Auto configuration for gson. The following auto-configuration will get
 * applied:
 * <ul>
 * <li>an {@link Gson} in case none is already configured.</li>
 * <li>an {@link GsonFactoryBean} in case none is already configured.</li>
 * </ul>
 * 
 * @author David Liu
 * @since 1.1.4
 */
@Configuration
@ConditionalOnClass(Gson.class)
public class GsonAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Gson gson() {
		Gson gson = new Gson();
		return gson;
	}

}
