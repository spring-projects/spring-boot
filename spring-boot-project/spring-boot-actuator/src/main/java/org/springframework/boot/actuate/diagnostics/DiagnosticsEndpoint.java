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

package org.springframework.boot.actuate.diagnostics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * {@link RestControllerEndpoint @RestControllerEndpoint} to expose diagnostics info.
 *
 * @author Luis De Bello
 * @since 2.2.0
 */
@RestControllerEndpoint(id = "diagnostics")
public class DiagnosticsEndpoint {

	private static final Log logger = LogFactory.getLog(DiagnosticsEndpoint.class);

	private static final String CURRENT_DIRECTORY = ".";

	private VMDiagnostics virtualMachineDiagnostics;

	@Value("${management.endpoints.diagnostics.resources.extensions:}#{T(java.util.Collections).emptyList()}")
	private List<String> extensions;

	public DiagnosticsEndpoint() {
		this.virtualMachineDiagnostics = VMDiagnostics.newInstance();
	}

	@GetMapping
	public String getOperations() {
		return this.virtualMachineDiagnostics.getOperations();
	}

	@GetMapping("/{operation}")
	public String getOperationDetails(@PathVariable String operation) {
		Assert.notNull(operation, "Operation must not be null");
		return this.virtualMachineDiagnostics.getOperationDetails(operation);
	}

	@PostMapping("/{operation}")
	public String executeOperation(@PathVariable String operation,
			@RequestBody(required = false) Map<String, String> parameters) {
		Assert.notNull(operation, "Operation must not be null");
		return this.virtualMachineDiagnostics.executeOperation(operation, parameters);
	}

	@GetMapping("/resources")
	public String getResources() {
		logger.info("Listing resources");
		StringJoiner result = new StringJoiner(System.lineSeparator());
		File[] files = new File(CURRENT_DIRECTORY)
				.listFiles((file, name) -> this.extensions.contains(getExtensionFile(name)));
		for (File file : files) {
			if (file.isFile()) {
				result.add(file.getName());
			}
		}
		return result.toString();
	}

	@GetMapping("/resources/{name}")
	public byte[] getResource(@PathVariable String name) throws IOException {
		logger.info(String.format("Getting resource: %s", name));
		if (this.extensions.contains(getExtensionFile(name))) {
			File resource = new File((name));
			return (resource.exists() && resource.isFile()) ? Files.readAllBytes(resource.toPath()) : null;
		}
		return null;
	}

	@DeleteMapping("/resources/{name}")
	public void deleteResource(@PathVariable String name) throws IOException {
		logger.info(String.format("Deleting resource: %s", name));
		if (this.extensions.contains(getExtensionFile(name))) {
			File resource = new File((name));
			if (resource.exists() && resource.isFile()) {
				Files.delete(resource.toPath());
				logger.info(String.format("Resource %s deleted", name));
			}
		}
	}

	private String getExtensionFile(String fileName) {
		return (fileName.contains(CURRENT_DIRECTORY) && fileName.lastIndexOf(CURRENT_DIRECTORY) != 0)
				? fileName.substring(fileName.lastIndexOf(CURRENT_DIRECTORY) + 1) : "";
	}

}
