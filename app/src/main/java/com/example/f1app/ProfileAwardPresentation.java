package com.example.f1app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Presentation only: does not grant awards or change eligibility. */
final class ProfileAwardPresentation {
    private ProfileAwardPresentation() {}
    static String string(Map<String,Object> badge,String key) {
        Object value=badge.get(key);return value instanceof String?(String)value:"";
    }
    static int titles(Object value) {
        if(value instanceof Number) {
            double n=((Number)value).doubleValue();
            return Double.isFinite(n)&&n>=0&&n<=Integer.MAX_VALUE&&n==Math.rint(n)?(int)n:0;
        }
        if(value instanceof String) {
            try{return Math.max(0,Integer.parseInt(((String)value).trim()));}
            catch(NumberFormatException ignored){return 0;}
        }
        return 0;
    }
    static int drawable(String id) {
        switch(id) {
            case "first_correct":return R.drawable.pit_pass_first_correct;
            case "correct_10":return R.drawable.pit_pass_correct_10_v2;
            case "correct_50":return R.drawable.pit_pass_correct_50;
            case "correct_100":return R.drawable.pit_pass_correct_100;
            case "tenure_1":return R.drawable.pit_pass_one_year;
            case "tenure_3":return R.drawable.pit_pass_three_years;
            case "tenure_5":return R.drawable.pit_pass_five_years;
            case "season_2026_participant":return R.drawable.pit_pass_season_participant_2026;
            case "season_2026_place_1":return R.drawable.pit_pass_season_place_1;
            case "season_2026_place_2":return R.drawable.pit_pass_season_place_2;
            case "season_2026_place_3":return R.drawable.pit_pass_season_place_3;
            default:return 0;
        }
    }
    static int title(String id) {
        switch(id) {
            case "first_correct":return R.string.awards_first_correct;
            case "correct_10":return R.string.awards_correct_10;
            case "correct_50":return R.string.awards_correct_50;
            case "correct_100":return R.string.awards_correct_100;
            case "tenure_1":return R.string.awards_member_1_year;
            case "tenure_3":return R.string.awards_member_3_year;
            case "tenure_5":return R.string.awards_member_5_year;
            case "season_2026_participant":return R.string.awards_preview_member;
            case "season_2026_place_1":return R.string.awards_season_place_1;
            case "season_2026_place_2":return R.string.awards_season_place_2;
            case "season_2026_place_3":return R.string.awards_season_place_3;
            case "fan_driver":return R.string.awards_fan_driver;
            case "fan_team":return R.string.awards_fan_team;
            default:return R.string.profile_awards_title;
        }
    }
    static boolean supported(String id) {
        return drawable(id)!=0||"fan_driver".equals(id)||"fan_team".equals(id);
    }
    private static int priority(String id) {
        if(id.matches("season_2026_place_[123]"))return 1000-(id.charAt(id.length()-1)-'0');
        switch(id) {
            case "correct_100":return 900;
            case "correct_50":return 890;
            case "correct_10":return 880;
            case "tenure_5":return 800;
            case "tenure_3":return 790;
            case "tenure_1":return 780;
            case "first_correct":return 700;
            case "season_2026_participant":return 600;
            case "fan_driver":return 510;
            default:return 500;
        }
    }
    static List<Map<String,Object>> normalize(List<Map<String,Object>> raw) {
        Map<String,Map<String,Object>> unique=new LinkedHashMap<>();
        for(Map<String,Object> badge:raw) {
            if(badge==null)continue;
            String id=string(badge,"id");
            if(supported(id))unique.put(id,Collections.unmodifiableMap(new HashMap<>(badge)));
        }
        List<Map<String,Object>> result=new ArrayList<>(unique.values());
        result.sort(Comparator.comparingInt((Map<String,Object> b)->priority(string(b,"id"))).reversed());
        return Collections.unmodifiableList(result);
    }
}
