/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.thymeleaf;

import org.jspecify.annotations.Nullable;

public interface MessageRepository {

	Iterable<Message> findAll();

	Message save(Message message);

	@Nullable Message findMessage(Long id);

	void deleteMessage(Long id);

}
