/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.groovytemplates;

import org.jspecify.annotations.Nullable;

public interface MessageRepository {

	Iterable<Message> findAll();

	Message save(Message message);

	@Nullable Message findMessage(Long id);

}
