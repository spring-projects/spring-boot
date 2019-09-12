package org.springframework.boot.docs.service.kotlin

import org.springframework.stereotype.Service

/**
 * Sample showing autowiring without the [org.springframework.beans.factory.annotation.Autowired] annotation
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
@Service
class DatabaseAccountServiceWithoutAutowired(private val riskAssessor: RiskAssessor) :
	AccountService {
	//..
}
// end::customizer[]
