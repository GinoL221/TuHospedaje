package com.tuhospedaje;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {
}
