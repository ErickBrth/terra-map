package com.terramap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a real PostGIS database for tests.
 * <p>
 * A plain postgres image will not work because every spatial rule in this project relies on
 * PostGIS functions (ST_Relate, ST_DWithin) and GiST indexes.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final DockerImageName POSTGIS_IMAGE = DockerImageName
			.parse("postgis/postgis:16-3.4-alpine")
			.asCompatibleSubstituteFor("postgres");

	@Bean
	@ServiceConnection
	public PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(POSTGIS_IMAGE)
				.withDatabaseName("terramap")
				.withUsername("terramap")
				.withPassword("terramap");
	}

}
