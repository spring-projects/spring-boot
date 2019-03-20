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

package org.springframework.boot.actuate.info;

import java.util.Map;
import java.util.Properties;

import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * An {@link InfoContributor} that exposes {@link BuildProperties}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class BuildInfoContributor extends InfoPropertiesInfoContributor<BuildProperties> {

	public BuildInfoContributor(BuildProperties properties) {
		super(properties, Mode.FULL);
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("build", generateContent());
	}

	@Override
	protected PropertySource<?> toSimplePropertySource() {
		Properties props = new Properties();
		copyIfSet(props, "group");
		copyIfSet(props, "artifact");
		copyIfSet(props, "name");
		copyIfSet(props, "version");
		copyIfSet(props, "time");
		return new PropertiesPropertySource("build", props);
	}

	@Override
	protected void postProcessContent(Map<String, Object> content) {
		replaceValue(content, "time", getProperties().getTime());
	}

}
