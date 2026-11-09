package com.example.f1app;

import java.util.Arrays;
import java.util.List;

/** Conditions correspond to the supplied achievement backend; no client-side granting. */
final class AwardCatalogue {
    static final int PREDICTIONS=0,TENURE=1,SEASON=2,FANCLUB=3;
    static final class Entry {
        final String id;
        final int category,condition,target;
        Entry(String id,int category,int condition,int target) {this.id=id;this.category=category;this.condition=condition;this.target=target;}
    }
    static final List<Entry> ALL=java.util.Collections.unmodifiableList(Arrays.asList(
        new Entry("first_correct",PREDICTIONS,R.string.award_condition_first,1),
        new Entry("correct_10",PREDICTIONS,R.string.award_condition_correct,10),
        new Entry("correct_50",PREDICTIONS,R.string.award_condition_correct,50),
        new Entry("correct_100",PREDICTIONS,R.string.award_condition_correct,100),
        new Entry("tenure_1",TENURE,R.string.award_condition_tenure,1),
        new Entry("tenure_3",TENURE,R.string.award_condition_tenure,3),
        new Entry("tenure_5",TENURE,R.string.award_condition_tenure,5),
        new Entry("season_2026_participant",SEASON,R.string.award_condition_participant,0),
        new Entry("season_2026_place_1",SEASON,R.string.award_condition_place,1),
        new Entry("season_2026_place_2",SEASON,R.string.award_condition_place,2),
        new Entry("season_2026_place_3",SEASON,R.string.award_condition_place,3),
        new Entry("fan_driver",FANCLUB,R.string.award_condition_driver,0),
        new Entry("fan_team",FANCLUB,R.string.award_condition_team,0)
    ));
    static int categoryTitle(int category) {
        switch(category) {
            case PREDICTIONS:return R.string.awards_category_predictions;
            case TENURE:return R.string.awards_category_tenure;
            case SEASON:return R.string.awards_category_season;
            default:return R.string.awards_category_fanclubs;
        }
    }
    static Entry find(String id) {for(Entry e:ALL)if(e.id.equals(id))return e;return null;}
}
