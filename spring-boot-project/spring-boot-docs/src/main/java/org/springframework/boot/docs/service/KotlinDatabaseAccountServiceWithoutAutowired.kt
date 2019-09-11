package org.springframework.boot.docs.service

import org.springframework.stereotype.Service

/**
 * Sample showing autowiring without the [org.springframework.beans.factory.annotation.Autowired] annotation
 */
// tag::customizer[]
@Service
class KotlinDatabaseAccountServiceWithoutAutowired(private val riskAssessor: RiskAssessor) : AccountService {
	//..
}
// end::customizer[]
