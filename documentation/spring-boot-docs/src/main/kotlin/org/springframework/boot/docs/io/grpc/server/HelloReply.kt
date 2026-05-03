package org.springframework.boot.docs.io.grpc.server

interface HelloReply {

	fun getMessage(): String

	companion object {

		fun newBuilder(): Builder {
			throw IllegalStateException()
		}

	}

	interface Builder {

		fun setMessage(message: String): Builder

		fun build(): HelloReply

	}

}
