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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson2.JsonComponent;
import org.springframework.boot.jackson2.JsonObjectSerializer;

/**
 * {@link JsonObjectSerializer} for {@link ExampleResult}.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of Jackson 3
 */
@JsonComponent
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public class ExampleResult2Serializer extends JsonObjectSerializer<ExampleResult2> {

	@Override
	protected void serializeObject(ExampleResult2 item, JsonGenerator jgen, SerializerProvider provider)
			throws IOException {
		jgen.writeStringField("identifier2", item.id());
	}

}
