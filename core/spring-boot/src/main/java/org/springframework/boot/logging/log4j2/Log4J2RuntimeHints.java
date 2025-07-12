/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation for {@link Log4J2LoggingSystem}.
 *
 * @author Piotr P. Karwasz
 */
class Log4J2RuntimeHints implements RuntimeHintsRegistrar {

	// Log4j2LoggingSystem checks for the presence of these classes reflectively.
	static final String PROVIDER = "org.apache.logging.log4j.core.impl.Log4jProvider";

	// Tree parsers used by Log4j 2 for configuration files.
	static final String JSON_TREE_PARSER_V2 = "com.fasterxml.jackson.databind.ObjectMapper";
	static final String XML_TREE_PARSER = "javax.xml.parsers.DocumentBuilderFactory";
	static final String YAML_TREE_PARSER_V2 = "com.fasterxml.jackson.dataformat.yaml.YAMLMapper";

	// JUL implementations that use Log4j 2 API.
	static final String LOG4J_BRIDGE_HANDLER = "org.apache.logging.log4j.jul.Log4jBridgeHandler";
	static final String LOG4J_LOG_MANAGER = "org.apache.logging.log4j.jul.LogManager";

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (!ClassUtils.isPresent(PROVIDER, classLoader)) {
			return;
		}
		registerTypeForReachability(hints, classLoader, PROVIDER);
		// Register default Log4j2 configuration files
		hints.resources().registerPattern("org/springframework/boot/logging/log4j2/log4j2.xml");
		hints.resources().registerPattern("org/springframework/boot/logging/log4j2/log4j2-file.xml");
		hints.resources().registerPattern("log4j2.springboot");
		// Declares the types that Log4j2LoggingSystem checks for existence reflectively.
		registerTypeForReachability(hints, classLoader, JSON_TREE_PARSER_V2);
		registerTypeForReachability(hints, classLoader, XML_TREE_PARSER);
		registerTypeForReachability(hints, classLoader, YAML_TREE_PARSER_V2);
		registerTypeForReachability(hints, classLoader, LOG4J_BRIDGE_HANDLER);
		registerTypeForReachability(hints, classLoader, LOG4J_LOG_MANAGER);
		// Don't need to register the custom Log4j 2 plugins,
		// since they will be registered by the Log4j 2 `GraalvmPluginProcessor`.
	}

	/**
	 * Registers the type to prevent GraalVM from removing it during the native build.
	 */
	private void registerTypeForReachability(RuntimeHints hints, ClassLoader classLoader, String typeName) {
		hints.reflection().registerTypeIfPresent(classLoader, typeName);
	}

}
