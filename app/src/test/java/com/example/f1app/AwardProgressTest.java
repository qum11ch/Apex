package com.example.f1app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.Instant;
import java.util.HashSet;

public class AwardProgressTest {
    private long at(String value){return Instant.parse(value).toEpochMilli();}
    @Test public void catalogueHasThirteenUniqueEntries(){
        HashSet<String> ids=new HashSet<>();for(AwardCatalogue.Entry e:AwardCatalogue.ALL)assertTrue(ids.add(e.id));assertEquals(13,ids.size());
    }
    @Test public void missingProgressIsUnknown(){assertNull(AwardProgress.count(null));assertNull(AwardProgress.completedYears(null,at("2026-11-09T00:00:00Z")));}
    @Test public void invalidCountRejected(){assertNull(AwardProgress.count(-1));assertNull(AwardProgress.count(1.5));assertNull(AwardProgress.count(Double.NaN));assertEquals(Long.valueOf(7),AwardProgress.count(7L));}
    @Test public void anniversaryIncludesTime(){
        long created=at("2023-11-09T12:00:00Z");assertEquals(Integer.valueOf(2),AwardProgress.completedYears(created,at("2026-11-09T11:59:59Z")));assertEquals(Integer.valueOf(3),AwardProgress.completedYears(created,at("2026-11-09T12:00:00Z")));
    }
    @Test public void leapDateMatchesBackendMarchFirstRule(){
        long created=at("2024-02-29T12:00:00Z");assertEquals(Integer.valueOf(0),AwardProgress.completedYears(created,at("2025-02-28T12:00:00Z")));assertEquals(Integer.valueOf(1),AwardProgress.completedYears(created,at("2025-03-01T12:00:00Z")));
    }
    @Test public void futureCreationDateIsUnknown(){assertNull(AwardProgress.completedYears(at("2027-01-01T00:00:00Z"),at("2026-01-01T00:00:00Z")));}
    @Test public void thresholdsDoNotGrantAnAward(){
        AwardCatalogue.Entry e=AwardCatalogue.find("correct_10");assertFalse(AwardProgress.conditionMet(e,9L,0));assertTrue(AwardProgress.conditionMet(e,10L,0));assertFalse(AwardProgress.conditionMet(e,null,0));
    }
    @Test public void seasonalConditionsDoNotPretendToHaveNumericProgress(){assertNull(AwardProgress.current(AwardCatalogue.find("season_2026_place_1"),100L,5));}
}
