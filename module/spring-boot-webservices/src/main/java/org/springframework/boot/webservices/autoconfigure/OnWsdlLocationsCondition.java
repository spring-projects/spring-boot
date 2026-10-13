/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.OnPropertyListCondition;

/**
 * Condition to determine if {@code spring.webservices.wsdl-locations} is specified.
 *
 * @author Eneias Silva
 * @author Stephane Nicoll
 */
class OnWsdlLocationsCondition extends OnPropertyListCondition {

	OnWsdlLocationsCondition() {
		super("spring.webservices.wsdl-locations", () -> ConditionMessage.forCondition("WSDL locations"));
	}

}
