package com.starter.domain.repository.testdata;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public interface TimeTestData {

    LocalDateTime TODAY = LocalDateTime.of(2021, 11, 20, 12, 00);
    LocalDateTime YESTERDAY = TODAY.minusDays(1);
    Long TODAY_MS = TODAY.toInstant(ZoneOffset.UTC).toEpochMilli();
    Long YESTERDAY_MS = YESTERDAY.toInstant(ZoneOffset.UTC).toEpochMilli();

}
