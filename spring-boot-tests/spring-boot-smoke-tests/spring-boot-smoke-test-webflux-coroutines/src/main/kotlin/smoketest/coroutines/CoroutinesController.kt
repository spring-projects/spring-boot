package smoketest.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CoroutinesController {

	@GetMapping("/suspending")
	suspend fun suspendingFunction(): String {
		delay(10)
		return "Hello World"
	}

	@GetMapping("/flow")
	fun flow() = flow {
		delay(10)
		emit("Hello ")
		delay(10)
		emit("World")
	}

	@GetMapping("/mono")
	fun mono() = mono {
		delay(10)
		"Hello World"
	}

	@ExperimentalCoroutinesApi
	@GetMapping("/flux")
	fun flux() = flux {
		delay(10)
		send("Hello ")
		delay(10)
		send("World")
	}

}
