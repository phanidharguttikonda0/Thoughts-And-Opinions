package com.ThoughtsAndOpinions.IdentityService.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

public class CustomIdGenerator implements IdentifierGenerator {

    private static final Logger log = LoggerFactory.getLogger(CustomIdGenerator.class);

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        log.info("Hibernate interceptor triggered: Generating Snowflake ID...");
        try {
            SnowflakeIdGenerator generator = ApplicationContextHolder.getBean(SnowflakeIdGenerator.class);
            long id = generator.nextId();
            log.info("Snowflake ID generated successfully: {}", id);
            return id;
        } catch (Exception e) {
            log.error("CRITICAL CRASH during Snowflake ID generation: ", e);
            throw e;
        }
    }
}
