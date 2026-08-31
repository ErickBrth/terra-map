package com.terramap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Full application context smoke test. Renamed from *Tests to *IT so Surefire
 * (fast, no Docker) never picks it up -- only Failsafe runs it, at the
 * integration-test phase, alongside the other Testcontainers-backed tests.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TerramapApplicationIT {

    @Test
    void contextLoads() {
    }

}
