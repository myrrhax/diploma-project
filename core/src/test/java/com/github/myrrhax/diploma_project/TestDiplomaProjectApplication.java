package com.github.myrrhax.diploma_project;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@ActiveProfiles("test")
public class TestDiplomaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.from(DiplomaProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

	@Bean
	public DataSource dsPostgresGenerator() {
		return DataSourceBuilder.create()
				.username("postgres")
				.password("postgres")
				.url("jdbc:postgresql://localhost:5433/postgres")
				.driverClassName("org.postgresql.Driver")
				.build();
	}

	@Bean
	public JdbcTemplate jdbcTemplatePostgresGenerator(@Qualifier("dsPostgresGenerator") DataSource ds) {
		return new JdbcTemplate(ds);
	}
}
