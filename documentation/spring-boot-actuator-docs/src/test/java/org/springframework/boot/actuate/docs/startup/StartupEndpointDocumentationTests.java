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

package org.springframework.boot.actuate.docs.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.docs.MockMvcEndpointDocumentationTests;
import org.springframework.boot.actuate.startup.StartupEndpoint;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.metrics.StartupStep;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * Tests for generating documentation describing {@link StartupEndpoint}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
class StartupEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@BeforeEach
	void appendSampleStartupSteps(@Autowired BufferingApplicationStartup applicationStartup) {
		StartupStep starting = applicationStartup.start("spring.boot.application.starting");
		starting.tag("mainApplicationClass", "com.example.startup.StartupApplication");
		StartupStep instantiate = applicationStartup.start("spring.beans.instantiate");
		instantiate.tag("beanName", "homeController");
		instantiate.end();
		starting.end();
	}

	@Test
	void startupSnapshot() {
		assertThat(this.mvc.get().uri("/actuator/startup")).hasStatusOk()
			.apply(document("startup-snapshot", PayloadDocumentation.responseFields(responseFields())));
	}

	@Test
	void startup() {
		assertThat(this.mvc.post().uri("/actuator/startup")).hasStatusOk()
			.apply(document("startup", PayloadDocumentation.responseFields(responseFields())));
	}

	private FieldDescriptor[] responseFields() {
		return new FieldDescriptor[] {
				fieldWithPath("springBootVersion").type(JsonFieldType.STRING)
					.description("Spring Boot version for this application.")
					.optional(),
				fieldWithPath("timeline.startTime").description("Start time of the application."),
				fieldWithPath("timeline.events")
					.description("An array of steps collected during application startup so far."),
				fieldWithPath("timeline.events.[].startTime").description("The timestamp of the start of this event."),
				fieldWithPath("timeline.events.[].endTime").description("The timestamp of the end of this event."),
				fieldWithPath("timeline.events.[].duration").description("The precise duration of this event."),
				fieldWithPath("timeline.events.[].startupStep.name").description("The name of the StartupStep."),
				fieldWithPath("timeline.events.[].startupStep.id").description("The id of this StartupStep."),
				fieldWithPath("timeline.events.[].startupStep.parentId")
					.description("The parent id for this StartupStep.")
					.optional(),
				fieldWithPath("timeline.events.[].startupStep.tags")
					.description("An array of key/value pairs with additional step info."),
				fieldWithPath("timeline.events.[].startupStep.tags[].key")
					.description("The key of the StartupStep Tag."),
				fieldWithPath("timeline.events.[].startupStep.tags[].value")
					.description("The value of the StartupStep Tag.") };
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		StartupEndpoint startupEndpoint(BufferingApplicationStartup startup) {
			return new StartupEndpoint(startup);
		}

		@Bean
		BufferingApplicationStartup bufferingApplicationStartup() {
			return new BufferingApplicationStartup(16);
		}

	}

}
