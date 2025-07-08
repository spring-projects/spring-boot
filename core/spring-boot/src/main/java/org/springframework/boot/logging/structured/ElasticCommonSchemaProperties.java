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

package org.springframework.boot.logging.structured;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Properties for Elastic Common Schema structured logging.
 *
 * @param service service details
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.4.0
 */
public record ElasticCommonSchemaProperties(Service service) {

	static final ElasticCommonSchemaProperties NONE = new ElasticCommonSchemaProperties(Service.NONE);

	ElasticCommonSchemaProperties withDefaults(Environment environment) {
		Service service = this.service.withDefaults(environment);
		return new ElasticCommonSchemaProperties(service);
	}

	static String withFallbackProperty(Environment environment, String value, String property) {
		return (!StringUtils.hasLength(value)) ? environment.getProperty(property) : value;
	}

	/**
	 * Add {@link JsonWriter} members for the service.
	 * @param members the members to add to
	 */
	public void jsonMembers(JsonWriter.Members<?> members) {
		this.service.jsonMembers(members);
	}

	/**
	 * Return a new {@link ElasticCommonSchemaProperties} from bound from properties in
	 * the given {@link Environment}.
	 * @param environment the source environment
	 * @return a new {@link ElasticCommonSchemaProperties} instance
	 */
	public static ElasticCommonSchemaProperties get(Environment environment) {
		return Binder.get(environment)
			.bind("logging.structured.ecs", ElasticCommonSchemaProperties.class)
			.orElse(NONE)
			.withDefaults(environment);
	}

	/**
	 * Service details.
	 *
	 * @param name the application name
	 * @param version the version of the application
	 * @param environment the name of the environment the application is running in
	 * @param nodeName the name of the node the application is running on
	 */
	public record Service(String name, String version, String environment, String nodeName) {

		static final Service NONE = new Service(null, null, null, null);

		void jsonMembers(Members<?> members) {
			members.add("service").usingMembers((service) -> {
				service.add("name", this::name).whenHasLength();
				service.add("version", this::version).whenHasLength();
				service.add("environment", this::environment).whenHasLength();
				service.add("node").usingMembers((node) -> node.add("name", this::nodeName).whenHasLength());
			});
		}

		Service withDefaults(Environment environment) {
			String name = withFallbackProperty(environment, this.name, "spring.application.name");
			String version = withFallbackProperty(environment, this.version, "spring.application.version");
			return new Service(name, version, this.environment, this.nodeName);
		}

	}

	static class ElasticCommonSchemaPropertiesRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			BindableRuntimeHintsRegistrar.forTypes(ElasticCommonSchemaProperties.class)
				.registerHints(hints, classLoader);
		}

	}

}
