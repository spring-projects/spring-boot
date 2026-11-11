/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.boolex.PropertyConditionBase;

/**
 * Test condition used by {@link SpringBootJoranConfiguratorTests}.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class TestPropertyCondition extends PropertyConditionBase {

	@Override
	public boolean evaluate() {
		return property("MINE").contains("true");
	}

}
