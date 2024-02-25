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

package smoketest.web.thymeleaf;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * InMemoryMessageRepository class.
 */
public class InMemoryMessageRepository implements MessageRepository {

	private static final AtomicLong counter = new AtomicLong();

	private final ConcurrentMap<Long, Message> messages = new ConcurrentHashMap<>();

	/**
     * Returns an iterable collection of all messages in the repository.
     *
     * @return an iterable collection of all messages
     */
    @Override
	public Iterable<Message> findAll() {
		return this.messages.values();
	}

	/**
     * Saves a message in the repository.
     * 
     * @param message the message to be saved
     * @return the saved message
     */
    @Override
	public Message save(Message message) {
		Long id = message.getId();
		if (id == null) {
			id = counter.incrementAndGet();
			message.setId(id);
		}
		this.messages.put(id, message);
		return message;
	}

	/**
     * Retrieves a message from the in-memory message repository based on its ID.
     * 
     * @param id the ID of the message to be retrieved
     * @return the message with the specified ID, or null if no message is found
     */
    @Override
	public Message findMessage(Long id) {
		return this.messages.get(id);
	}

	/**
     * Deletes a message from the repository based on its ID.
     * 
     * @param id the ID of the message to be deleted
     */
    @Override
	public void deleteMessage(Long id) {
		this.messages.remove(id);
	}

}
