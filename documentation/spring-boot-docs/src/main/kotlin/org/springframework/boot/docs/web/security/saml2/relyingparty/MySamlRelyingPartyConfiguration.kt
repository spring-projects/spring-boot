package org.springframework.boot.docs.web.security.saml2.relyingparty

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain


@Configuration(proxyBeanMethods = false)
open class MySamlRelyingPartyConfiguration {

	@Bean
	open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http {
			authorizeHttpRequests {
				authorize(anyRequest, authenticated)
			}
			saml2Login {  }
			saml2Logout {
				logoutRequest { logoutUrl =  "/SLOService.saml2" }
				logoutResponse { logoutUrl =  "/SLOService.saml2" }
			}
		}
		return http.build()
	}

}
