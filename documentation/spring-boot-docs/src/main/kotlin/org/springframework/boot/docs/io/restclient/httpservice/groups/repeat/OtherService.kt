package org.springframework.boot.docs.io.restclient.httpservice.groups.repeat

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface OtherService {

	@PostExchange("/other")
	fun echo(@RequestBody message: Map<String, String>): Map<*, *>

}
