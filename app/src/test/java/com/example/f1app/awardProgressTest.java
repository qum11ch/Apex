package com.example.f1app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.Instant;
import java.util.HashSet;

public class awardProgressTest {
    private long at(String value){return Instant.parse(value).toEpochMilli();}
    @Test public void catalogueHasThirteenUniqueEntries(){
        HashSet<String> ids=new HashSet<>();for(awardCatalogue.Entry e: awardCatalogue.ALL)assertTrue(ids.add(e.id));assertEquals(13,ids.size());
    }
    @Test public void missingProgressIsUnknown(){assertNull(awardProgress.count(null));assertNull(awardProgress.completedYears(null,at("2026-11-09T00:00:00Z")));}
    @Test public void invalidCountRejected(){assertNull(awardProgress.count(-1));assertNull(awardProgress.count(1.5));assertNull(awardProgress.count(Double.NaN));assertEquals(Long.valueOf(7), awardProgress.count(7L));}
    @Test public void anniversaryIncludesTime(){
        long created=at("2023-11-09T12:00:00Z");assertEquals(Integer.valueOf(2), awardProgress.completedYears(created,at("2026-11-09T11:59:59Z")));assertEquals(Integer.valueOf(3), awardProgress.completedYears(created,at("2026-11-09T12:00:00Z")));
    }
    @Test public void leapDateMatchesBackendMarchFirstRule(){
        long created=at("2024-02-29T12:00:00Z");assertEquals(Integer.valueOf(0), awardProgress.completedYears(created,at("2025-02-28T12:00:00Z")));assertEquals(Integer.valueOf(1), awardProgress.completedYears(created,at("2025-03-01T12:00:00Z")));
    }
    @Test public void futureCreationDateIsUnknown(){assertNull(awardProgress.completedYears(at("2027-01-01T00:00:00Z"),at("2026-01-01T00:00:00Z")));}
    @Test public void thresholdsDoNotGrantAnAward(){
        awardCatalogue.Entry e= awardCatalogue.find("correct_10");assertFalse(awardProgress.conditionMet(e,9L,0));assertTrue(awardProgress.conditionMet(e,10L,0));assertFalse(awardProgress.conditionMet(e,null,0));
    }
    @Test public void seasonalConditionsDoNotPretendToHaveNumericProgress(){assertNull(awardProgress.current(awardCatalogue.find("season_2026_place_1"),100L,5));}
}
