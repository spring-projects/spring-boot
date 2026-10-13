/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webservices.service;

import java.time.LocalDate;

public interface HumanResourceService {

	void bookHoliday(LocalDate startDate, LocalDate endDate, String name);

}
