package com.terramap;

import org.springframework.boot.SpringApplication;

public class TestTerramapApplication {

	public static void main(String[] args) {
		SpringApplication.from(TerramapApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
