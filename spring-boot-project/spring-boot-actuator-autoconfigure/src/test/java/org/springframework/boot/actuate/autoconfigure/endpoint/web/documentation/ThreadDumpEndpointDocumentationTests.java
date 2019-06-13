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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.management.ThreadDumpEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing {@link ThreadDumpEndpoint}.
 *
 * @author Andy Wilkinson
 */
class ThreadDumpEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void threadDump() throws Exception {
		ReentrantLock lock = new ReentrantLock();
		CountDownLatch latch = new CountDownLatch(1);
		new Thread(() -> {
			try {
				lock.lock();
				try {
					latch.await();
				}
				finally {
					lock.unlock();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}).start();
		this.mockMvc.perform(get("/actuator/threaddump")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("threaddump", preprocessResponse(limit("threads")),
						responseFields(fieldWithPath("threads").description("JVM's threads."),
								fieldWithPath("threads.[].blockedCount")
										.description("Total number of times that the thread has been " + "blocked."),
								fieldWithPath("threads.[].blockedTime")
										.description("Time in milliseconds that the thread has spent "
												+ "blocked. -1 if thread contention " + "monitoring is disabled."),
								fieldWithPath("threads.[].daemon")
										.description("Whether the thread is a daemon "
												+ "thread. Only available on Java 9 or " + "later.")
										.optional().type(JsonFieldType.BOOLEAN),
								fieldWithPath("threads.[].inNative")
										.description("Whether the thread is executing native code."),
								fieldWithPath("threads.[].lockName").description(
										"Description of the object on which the " + "thread is blocked, if any.")
										.optional().type(JsonFieldType.STRING),
								fieldWithPath("threads.[].lockInfo")
										.description("Object for which the thread is blocked " + "waiting.").optional()
										.type(JsonFieldType.OBJECT),
								fieldWithPath("threads.[].lockInfo.className")
										.description("Fully qualified class name of the lock" + " object.").optional()
										.type(JsonFieldType.STRING),
								fieldWithPath("threads.[].lockInfo.identityHashCode")
										.description("Identity hash code of the lock object.").optional()
										.type(JsonFieldType.NUMBER),
								fieldWithPath("threads.[].lockedMonitors")
										.description("Monitors locked by this thread, if any"),
								fieldWithPath("threads.[].lockedMonitors.[].className")
										.description("Class name of the lock object.").optional()
										.type(JsonFieldType.STRING),
								fieldWithPath("threads.[].lockedMonitors.[].identityHashCode")
										.description("Identity hash code of the lock " + "object.").optional()
										.type(JsonFieldType.NUMBER),
								fieldWithPath("threads.[].lockedMonitors.[].lockedStackDepth")
										.description("Stack depth where the monitor " + "was locked.").optional()
										.type(JsonFieldType.NUMBER),
								subsectionWithPath("threads.[].lockedMonitors.[].lockedStackFrame")
										.description("Stack frame that locked the " + "monitor.").optional()
										.type(JsonFieldType.OBJECT),
								fieldWithPath("threads.[].lockedSynchronizers")
										.description("Synchronizers locked by this thread."),
								fieldWithPath("threads.[].lockedSynchronizers.[].className").description(
										"Class name of the locked " + "synchronizer.").optional()
										.type(JsonFieldType.STRING),
								fieldWithPath("threads.[].lockedSynchronizers.[].identityHashCode").description(
										"Identity hash code of the locked " + "synchronizer.").optional()
										.type(JsonFieldType.NUMBER),
								fieldWithPath("threads.[].lockOwnerId").description(
										"ID of the thread that owns the object on which "
												+ "the thread is blocked. `-1` if the " + "thread is not blocked."),
								fieldWithPath("threads.[].lockOwnerName")
										.description("Name of the thread that owns the "
												+ "object on which the thread is " + "blocked, if any.")
										.optional().type(JsonFieldType.STRING),
								fieldWithPath("threads.[].priority")
										.description("Priority of the thread. Only " + "available on Java 9 or later.")
										.optional().type(JsonFieldType.NUMBER),
								fieldWithPath("threads.[].stackTrace").description("Stack trace of the thread."),
								fieldWithPath("threads.[].stackTrace.[].classLoaderName").description(
										"Name of the class loader of the " + "class that contains the execution "
												+ "point identified by this entry, if "
												+ "any. Only available on Java 9 or " + "later.")
										.optional().type(JsonFieldType.STRING),
								fieldWithPath("threads.[].stackTrace.[].className").description(
										"Name of the class that contains the " + "execution point identified "
												+ "by this entry."),
								fieldWithPath("threads.[].stackTrace.[].fileName")
										.description("Name of the source file that " + "contains the execution point "
												+ "identified by this entry, if any.")
										.optional().type(JsonFieldType.STRING),
								fieldWithPath("threads.[].stackTrace.[].lineNumber")
										.description("Line number of the execution "
												+ "point identified by this entry. " + "Negative if unknown."),
								fieldWithPath("threads.[].stackTrace.[].methodName").description("Name of the method."),
								fieldWithPath("threads.[].stackTrace.[].moduleName")
										.description("Name of the module that contains "
												+ "the execution point identified by "
												+ "this entry, if any. Only available " + "on Java 9 or later.")
										.optional().type(JsonFieldType.STRING),
								fieldWithPath("threads.[].stackTrace.[].moduleVersion")
										.description("Version of the module that " + "contains the execution point "
												+ "identified by this entry, if any. "
												+ "Only available on Java 9 or later.")
										.optional().type(JsonFieldType.STRING),
								fieldWithPath("threads.[].stackTrace.[].nativeMethod")
										.description("Whether the execution point is a native " + "method."),
								fieldWithPath("threads.[].suspended").description("Whether the thread is suspended."),
								fieldWithPath("threads.[].threadId").description("ID of the thread."),
								fieldWithPath("threads.[].threadName").description("Name of the thread."),
								fieldWithPath("threads.[].threadState").description(
										"State of the thread (" + describeEnumValues(Thread.State.class) + ")."),
								fieldWithPath("threads.[].waitedCount").description(
										"Total number of times that the thread has waited" + " for notification."),
								fieldWithPath("threads.[].waitedTime")
										.description("Time in milliseconds that the thread has spent "
												+ "waiting. -1 if thread contention " + "monitoring is disabled"))));
		latch.countDown();
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public ThreadDumpEndpoint endpoint() {
			return new ThreadDumpEndpoint();
		}

	}

}
