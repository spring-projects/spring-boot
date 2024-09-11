/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.json.JsonWriter;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Service details for Graylog Extended Log Format structured logging.
 *
 * @param name the application name
 * @param version the version of the application
 * @author Samuel Lissner
 * @since 3.4.0
 */
public record GraylogExtendedLogFormatService(String name, String version) {

	static final GraylogExtendedLogFormatService NONE = new GraylogExtendedLogFormatService(null, null);

	private GraylogExtendedLogFormatService withDefaults(Environment environment) {
		String name = withFallbackProperty(environment, this.name, "spring.application.name");
		String version = withFallbackProperty(environment, this.version, "spring.application.version");
		return new GraylogExtendedLogFormatService(name, version);
	}

	private String withFallbackProperty(Environment environment, String value, String property) {
		return (!StringUtils.hasLength(value)) ? environment.getProperty(property) : value;
	}

	/**
	 * Add {@link JsonWriter} members for the service.
	 * @param members the members to add to
	 */
	public void jsonMembers(JsonWriter.Members<?> members) {
		members.add("host", this::name).whenHasLength();
		members.add("_service_version", this::version).whenHasLength();
	}

	/**
	 * Return a new {@link GraylogExtendedLogFormatService} from bound from properties in
	 * the given {@link Environment}.
	 * @param environment the source environment
	 * @return a new {@link GraylogExtendedLogFormatService} instance
	 */
	public static GraylogExtendedLogFormatService get(Environment environment) {
		return Binder.get(environment)
			.bind("logging.structured.gelf.service", GraylogExtendedLogFormatService.class)
			.orElse(NONE)
			.withDefaults(environment);
	}

}
