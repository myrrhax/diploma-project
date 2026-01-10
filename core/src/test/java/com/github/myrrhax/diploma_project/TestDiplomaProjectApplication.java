package com.github.myrrhax.diploma_project;

import org.springframework.boot.SpringApplication;

public class TestDiplomaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.from(DiplomaProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
