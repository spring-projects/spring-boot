package org.springframework.boot.docs.web.reactive.webflux

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@RestController
@RequestMapping("/users")
class MyRestController(private val userRepository: UserRepository, private val customerRepository: CustomerRepository) {
	@GetMapping("/{user}")
	fun getUser(@PathVariable userId: Long): Mono<User?> {
		return userRepository.findById(userId)
	}

	@GetMapping("/{user}/customers")
	fun getUserCustomers(@PathVariable userId: Long): Flux<Customer> {
		return userRepository.findById(userId).flatMapMany { user: User? ->
			customerRepository.findByUser(
				user
			)
		}
	}

	@DeleteMapping("/{user}")
	fun deleteUser(@PathVariable userId: Long) {
		userRepository.deleteById(userId)
	}
}
