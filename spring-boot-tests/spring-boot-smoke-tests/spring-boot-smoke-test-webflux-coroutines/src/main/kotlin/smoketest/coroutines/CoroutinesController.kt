package smoketest.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
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

}
