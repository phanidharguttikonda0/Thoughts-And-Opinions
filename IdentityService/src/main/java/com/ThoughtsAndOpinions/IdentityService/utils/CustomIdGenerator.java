package com.ThoughtsAndOpinions.IdentityService.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import java.io.Serializable;

public class CustomIdGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        // Grab the Snowflake generator bean dynamically via the static application holder
        SnowflakeIdGenerator generator = ApplicationContextHolder.getBean(SnowflakeIdGenerator.class);
        return generator.nextId();
    }
}
