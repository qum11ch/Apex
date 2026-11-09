package com.example.f1app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileAwardPresentationTest {
    private Map<String,Object> item(String id) {Map<String,Object> result=new HashMap<>();result.put("id",id);return result;}
    @Test public void deduplicatesKnownAwards() {
        assertEquals(1,ProfileAwardPresentation.normalize(Arrays.asList(item("correct_10"),item("correct_10"))).size());
    }
    @Test public void ignoresUnknownAndNullEntries() {
        assertTrue(ProfileAwardPresentation.normalize(Arrays.asList(item("unknown"),null)).isEmpty());
    }
    @Test public void keepsSeasonPlaceAheadOfOtherAwards() {
        List<Map<String,Object>> result=ProfileAwardPresentation.normalize(Arrays.asList(item("fan_driver"),item("correct_100"),item("season_2026_place_1")));
        assertEquals("season_2026_place_1",result.get(0).get("id"));
        assertEquals("correct_100",result.get(1).get("id"));
    }
    @Test public void snapshotDoesNotChangeWithInputMap() {
        Map<String,Object> input=item("fan_driver");input.put("code","AAA");
        List<Map<String,Object>> result=ProfileAwardPresentation.normalize(Collections.singletonList(input));input.put("code","BBB");
        assertEquals("AAA",result.get(0).get("code"));
    }
    @Test public void validatesChampionshipCount() {
        assertEquals(7,ProfileAwardPresentation.titles("7"));assertEquals(7,ProfileAwardPresentation.titles(7L));
        assertEquals(0,ProfileAwardPresentation.titles(-1));assertEquals(0,ProfileAwardPresentation.titles(1.5));
        assertEquals(0,ProfileAwardPresentation.titles(Double.NaN));assertEquals(0,ProfileAwardPresentation.titles("bad"));
    }
    @Test public void zeroAwardsIsAValidEmptyList() {assertTrue(ProfileAwardPresentation.normalize(Collections.emptyList()).isEmpty());}
}
