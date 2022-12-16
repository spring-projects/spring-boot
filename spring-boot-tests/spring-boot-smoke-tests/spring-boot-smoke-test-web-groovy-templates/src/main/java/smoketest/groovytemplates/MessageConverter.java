/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.groovytemplates;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class MessageConverter implements Converter<String, Message> {

	private final MessageRepository messageRepository;

	MessageConverter(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}

	@Override
	public Message convert(String source) {
		return this.messageRepository.findMessage(Long.valueOf(source));
	}

}
