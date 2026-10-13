/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webservices.service;

import java.time.LocalDate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Service;

@Service
class StubHumanResourceService implements HumanResourceService {

	private static final Log logger = LogFactory.getLog(StubHumanResourceService.class);

	@Override
	public void bookHoliday(LocalDate startDate, LocalDate endDate, String name) {
		logger.info("Booking holiday for [" + startDate + " - " + endDate + "] for [" + name + "]");
	}

}
