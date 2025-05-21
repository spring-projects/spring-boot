/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.info;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation.MockMvcEndpointDocumentationTests;
import org.springframework.boot.actuate.info.BuildInfoContributor;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.info.JavaInfoContributor;
import org.springframework.boot.actuate.info.OsInfoContributor;
import org.springframework.boot.actuate.info.ProcessInfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.beneathPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

/**
 * Tests for generating documentation describing the {@link InfoEndpoint}.
 *
 * @author Andy Wilkinson
 */
class InfoEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void info() {
		assertThat(this.mvc.get().uri("/actuator/info")).hasStatusOk()
			.apply(MockMvcRestDocumentation.document("info", gitInfo(), buildInfo(), osInfo(), processInfo(),
					javaInfo()));
	}

	private ResponseFieldsSnippet gitInfo() {
		return responseFields(beneathPath("git"),
				fieldWithPath("branch").description("Name of the Git branch, if any."),
				fieldWithPath("commit").description("Details of the Git commit, if any."),
				fieldWithPath("commit.time").description("Timestamp of the commit, if any.").type(JsonFieldType.VARIES),
				fieldWithPath("commit.id").description("ID of the commit, if any."));
	}

	private ResponseFieldsSnippet buildInfo() {
		return responseFields(beneathPath("build"),
				fieldWithPath("artifact").description("Artifact ID of the application, if any.").optional(),
				fieldWithPath("group").description("Group ID of the application, if any.").optional(),
				fieldWithPath("name").description("Name of the application, if any.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("version").description("Version of the application, if any.").optional(),
				fieldWithPath("time").description("Timestamp of when the application was built, if any.")
					.type(JsonFieldType.VARIES)
					.optional());
	}

	private ResponseFieldsSnippet osInfo() {
		return responseFields(beneathPath("os"), osInfoField("name", "Name of the operating system"),
				osInfoField("version", "Version of the operating system"),
				osInfoField("arch", "Architecture of the operating system"));
	}

	private FieldDescriptor osInfoField(String field, String desc) {
		return fieldWithPath(field).description(desc + " (as obtained from the 'os." + field + "' system property).")
			.type(JsonFieldType.STRING)
			.optional();
	}

	private ResponseFieldsSnippet processInfo() {
		return responseFields(beneathPath("process"),
				fieldWithPath("pid").description("Process ID.").type(JsonFieldType.NUMBER),
				fieldWithPath("parentPid").description("Parent Process ID (or -1).").type(JsonFieldType.NUMBER),
				fieldWithPath("owner").description("Process owner.").type(JsonFieldType.STRING),
				fieldWithPath("cpus").description("Number of CPUs available to the process.")
					.type(JsonFieldType.NUMBER),
				fieldWithPath("memory").description("Memory information."),
				fieldWithPath("memory.heap").description("Heap memory."),
				fieldWithPath("memory.heap.init").description("Number of bytes initially requested by the JVM."),
				fieldWithPath("memory.heap.used").description("Number of bytes currently being used."),
				fieldWithPath("memory.heap.committed").description("Number of bytes committed for JVM use."),
				fieldWithPath("memory.heap.max")
					.description("Maximum number of bytes that can be used by the JVM (or -1)."),
				fieldWithPath("memory.nonHeap").description("Non-heap memory."),
				fieldWithPath("memory.nonHeap.init").description("Number of bytes initially requested by the JVM."),
				fieldWithPath("memory.nonHeap.used").description("Number of bytes currently being used."),
				fieldWithPath("memory.nonHeap.committed").description("Number of bytes committed for JVM use."),
				fieldWithPath("memory.nonHeap.max")
					.description("Maximum number of bytes that can be used by the JVM (or -1)."),
				fieldWithPath("memory.garbageCollectors").description("Details for garbage collectors."),
				fieldWithPath("memory.garbageCollectors[].name").description("Name of of the garbage collector."),
				fieldWithPath("memory.garbageCollectors[].collectionCount")
					.description("Total number of collections that have occurred."),
				fieldWithPath("virtualThreads")
					.description("Virtual thread information (if VirtualThreadSchedulerMXBean is available)")
					.type(JsonFieldType.OBJECT)
					.optional(),
				fieldWithPath("virtualThreads.mounted")
					.description("Estimate of the number of virtual threads currently mounted by the scheduler.")
					.type(JsonFieldType.NUMBER)
					.optional(),
				fieldWithPath("virtualThreads.queued").description(
						"Estimate of the number of virtual threads queued to the scheduler to start or continue execution.")
					.type(JsonFieldType.NUMBER)
					.optional(),
				fieldWithPath("virtualThreads.parallelism").description("Scheduler's target parallelism.")
					.type(JsonFieldType.NUMBER)
					.optional(),
				fieldWithPath("virtualThreads.poolSize")
					.description(
							"Current number of platform threads that the scheduler has started but have not terminated")
					.type(JsonFieldType.NUMBER)
					.optional());
	}

	private ResponseFieldsSnippet javaInfo() {
		return responseFields(beneathPath("java"),
				fieldWithPath("version").description("Java version, if available.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("vendor").description("Vendor details."),
				fieldWithPath("vendor.name").description("Vendor name, if available.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("vendor.version").description("Vendor version, if available.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("runtime").description("Runtime details."),
				fieldWithPath("runtime.name").description("Runtime name, if available.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("runtime.version").description("Runtime version, if available.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("jvm").description("JVM details."),
				fieldWithPath("jvm.name").description("JVM name, if available.").type(JsonFieldType.STRING).optional(),
				fieldWithPath("jvm.vendor").description("JVM vendor, if available.")
					.type(JsonFieldType.STRING)
					.optional(),
				fieldWithPath("jvm.version").description("JVM version, if available.")
					.type(JsonFieldType.STRING)
					.optional());
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		InfoEndpoint endpoint(List<InfoContributor> infoContributors) {
			return new InfoEndpoint(infoContributors);
		}

		@Bean
		GitInfoContributor gitInfoContributor() {
			Properties properties = new Properties();
			properties.put("branch", "main");
			properties.put("commit.id", "df027cf1ec5aeba2d4fedd7b8c42b88dc5ce38e5");
			properties.put("commit.id.abbrev", "df027cf");
			properties.put("commit.time", Long.toString(Instant.now().getEpochSecond()));
			GitProperties gitProperties = new GitProperties(properties);
			return new GitInfoContributor(gitProperties);
		}

		@Bean
		BuildInfoContributor buildInfoContributor() {
			Properties properties = new Properties();
			properties.put("group", "com.example");
			properties.put("artifact", "application");
			properties.put("version", "1.0.3");
			BuildProperties buildProperties = new BuildProperties(properties);
			return new BuildInfoContributor(buildProperties);
		}

		@Bean
		OsInfoContributor osInfoContributor() {
			return new OsInfoContributor();
		}

		@Bean
		ProcessInfoContributor processInfoContributor() {
			return new ProcessInfoContributor();
		}

		@Bean
		JavaInfoContributor javaInfoContributor() {
			return new JavaInfoContributor();
		}

	}

}
