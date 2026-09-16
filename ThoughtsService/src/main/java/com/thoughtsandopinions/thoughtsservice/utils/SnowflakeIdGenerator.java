package com.thoughtsandopinions.thoughtsservice.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SnowflakeIdGenerator {

    private final long epochMilli = 1789285800000L;
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long sequenceBits = 12L;

    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    // Inject unique numbers per deployed server instance (0 to 31)
    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // Spring constructor injection assigns different worker values per node instance
    public SnowflakeIdGenerator(
            @Value("${snowflake.worker-id:1}") long workerId,
            @Value("${snowflake.datacenter-id:1}") long datacenterId) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        // ... (keep the rest of your bit-shifting and clock-validation logic exactly the same)
        long timestamp = Instant.now().toEpochMilli();
        // ...
        return ((timestamp - epochMilli) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }
}
