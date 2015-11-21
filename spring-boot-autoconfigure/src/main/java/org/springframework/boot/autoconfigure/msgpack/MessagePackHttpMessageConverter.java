/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.msgpack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;


/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that
 * can read and write <a href="http://msgpack.org/">MessagePack</a>.
 * @author Toshiaki Maki
 * @since 1.3.2
 */
public class MessagePackHttpMessageConverter
		extends AbstractJackson2HttpMessageConverter {
	public MessagePackHttpMessageConverter() {
		super(new ObjectMapper(new MessagePackFactory()),
				new MediaType("application", "x-msgpack"));
	}
}
