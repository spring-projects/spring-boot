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

package org.springframework.boot.webflux.test.autoconfigure;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.ObjectValueSerializer;

/**
 * {@link ObjectValueSerializer} for {@link ExampleResult}.
 *
 * @author Stephane Nicoll
 */
@JacksonComponent
public class ExampleResultSerializer extends ObjectValueSerializer<ExampleResult> {

	@Override
	protected void serializeObject(ExampleResult item, JsonGenerator jsonWriter, SerializationContext context) {
		jsonWriter.writeStringProperty("identifier", item.id());
	}

}
