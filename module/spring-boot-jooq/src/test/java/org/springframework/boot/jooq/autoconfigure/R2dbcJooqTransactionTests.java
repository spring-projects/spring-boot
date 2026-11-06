/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import io.r2dbc.spi.ConnectionFactory;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.reactive.AwaitKt;
import kotlinx.coroutines.reactor.ReactorContext;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for jOOQ participation in Spring R2DBC transactions.
 *
 * @author George Papadopoulos
 */
class R2dbcJooqTransactionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class, R2dbcJooqAutoConfiguration.class,
				R2dbcTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class))
		.withPropertyValues("spring.r2dbc.url=r2dbc:h2:mem:///" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");

	@Test
	void commitsMultipleStatements() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			TransactionalOperator operator = TransactionalOperator
				.create(context.getBean(ReactiveTransactionManager.class));
			insert(dsl, 1).then(insert(dsl, 2))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectComplete()
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).containsExactly(1, 2);
		});
	}

	@Test
	void jooqAndDatabaseClientShareWritesAndRollback() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			DatabaseClient client = DatabaseClient.create(context.getBean(ConnectionFactory.class));
			TransactionalOperator operator = TransactionalOperator
				.create(context.getBean(ReactiveTransactionManager.class));
			RuntimeException failure = new RuntimeException("rollback");
			client.sql("insert into test_entity (id) values (1)")
				.fetch()
				.rowsUpdated()
				.thenMany(Flux.from(dsl.resultQuery("select id from test_entity"))
					.map((record) -> record.get(0, Long.class)))
				.concatWith(
						insert(dsl, 2).then(client.sql("select count(*) from test_entity").mapValue(Long.class).one()))
				.concatWith(Mono.error(failure))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext(1L, 2L)
				.expectErrorSatisfies((actual) -> assertThat(actual).isSameAs(failure))
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).isEmpty();
		});
	}

	@Test
	void directCoroutineAwaitParticipatesInRollback() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			TransactionalOperator operator = TransactionalOperator
				.create(context.getBean(ReactiveTransactionManager.class));
			Mono.deferContextual((reactorContext) -> Mono
				.fromCallable(() -> BuildersKt.runBlocking(new ReactorContext(Context.of(reactorContext)),
						(scope, continuation) -> AwaitKt
							.awaitSingle(dsl.query("insert into test_entity (id) values (1)"), continuation))))
				.then(Mono.error(new RuntimeException("rollback coroutine")))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectErrorMessage("rollback coroutine")
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).isEmpty();
		});
	}

	@Test
	void batchParticipatesInRollback() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			TransactionalOperator operator = TransactionalOperator
				.create(context.getBean(ReactiveTransactionManager.class));
			Flux.from(dsl.batch(dsl.query("insert into test_entity (id) values (1)"),
					dsl.query("insert into test_entity (id) values (2)")))
				.then(Mono.error(new RuntimeException("rollback batch")))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectErrorMessage("rollback batch")
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).isEmpty();
		});
	}

	@Test
	void requiresNewCommitsIndependently() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			ReactiveTransactionManager manager = context.getBean(ReactiveTransactionManager.class);
			TransactionalOperator outer = TransactionalOperator.create(manager);
			TransactionalOperator inner = TransactionalOperator.create(manager,
					new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
			insert(dsl, 1).then(insert(dsl, 2).as(inner::transactional))
				.then(Mono.error(new RuntimeException("rollback outer")))
				.as(outer::transactional)
				.as(StepVerifier::create)
				.expectErrorMessage("rollback outer")
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).containsExactly(2);
		});
	}

	@Test
	void nestedRollbackPreservesOuterTransaction() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			ReactiveTransactionManager manager = context.getBean(ReactiveTransactionManager.class);
			TransactionalOperator outer = TransactionalOperator.create(manager);
			TransactionalOperator inner = TransactionalOperator.create(manager,
					new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED));
			RuntimeException failure = new RuntimeException("rollback savepoint");
			insert(dsl, 1)
				.then(insert(dsl, 2).then(Mono.error(failure))
					.as(inner::transactional)
					.onErrorResume((actual) -> actual == failure, (actual) -> Mono.empty()))
				.then(insert(dsl, 3))
				.as(outer::transactional)
				.as(StepVerifier::create)
				.expectComplete()
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).containsExactly(1, 3);
		});
	}

	@Test
	void cancellationRollsBack() {
		this.contextRunner.run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			createTable(dsl);
			TransactionalOperator operator = TransactionalOperator
				.create(context.getBean(ReactiveTransactionManager.class));
			insert(dsl, 1).thenReturn("inserted")
				.concatWith(Mono.never())
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext("inserted")
				.thenCancel()
				.verify(Duration.ofSeconds(5));
			assertThat(ids(dsl)).isEmpty();
		});
	}

	private void createTable(DSLContext dsl) {
		Mono.from(dsl.query("create table test_entity (id integer primary key)")).block(Duration.ofSeconds(5));
	}

	private Mono<Void> insert(DSLContext dsl, int id) {
		return Mono.from(dsl.query("insert into test_entity (id) values (?)", id)).then();
	}

	private List<Integer> ids(DSLContext dsl) {
		return Flux.from(dsl.resultQuery("select id from test_entity order by id"))
			.map((record) -> record.get(0, Integer.class))
			.collectList()
			.blockOptional(Duration.ofSeconds(5))
			.orElseThrow();
	}

}
