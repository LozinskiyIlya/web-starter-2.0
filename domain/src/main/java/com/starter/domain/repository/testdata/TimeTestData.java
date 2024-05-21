package com.starter.domain.repository.testdata;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public interface TimeTestData {

    LocalDateTime TODAY = LocalDateTime.of(2021, 11, 20, 12, 00);
    LocalDateTime YESTERDAY = TODAY.minusDays(1);
    Instant TODAY_INSTANT = TODAY.toInstant(ZoneOffset.UTC);
    Instant YESTERDAY_INSTANT = YESTERDAY.toInstant(ZoneOffset.UTC);
    Long TODAY_MS = TODAY_INSTANT.toEpochMilli();
    Long YESTERDAY_MS = YESTERDAY_INSTANT.toEpochMilli();

}
