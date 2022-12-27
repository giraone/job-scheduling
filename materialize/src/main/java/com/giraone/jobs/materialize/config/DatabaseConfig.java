package com.giraone.jobs.materialize.config;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// TODO: Needed?
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfig.class);

    public DatabaseConfig() {
        LOGGER.info("Initialized DatabaseConfig with @EnableTransactionManagement");
    }

    @Bean
    ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        final ReactiveTransactionManager transactionManager = new R2dbcTransactionManager(connectionFactory);
        LOGGER.info("TransactionManager for {} is {}", connectionFactory, transactionManager);
        return transactionManager;
    }
}
