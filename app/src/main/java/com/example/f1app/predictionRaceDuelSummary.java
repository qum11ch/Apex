package com.example.f1app;

import java.util.List;

public final class predictionRaceDuelSummary {
    public int winsA,winsB,retirementsA,retirementsB,numericA,numericB,events;
    private long positionSumA,positionSumB;
    private predictionRaceDuelSummary(){}
    public Double averageA(){return numericA==0?null:(double)positionSumA/numericA;}
    public Double averageB(){return numericB==0?null:(double)positionSumB/numericB;}
    public static predictionRaceDuelSummary calculate(List<raceDuelEvent> data) {
        predictionRaceDuelSummary result=new predictionRaceDuelSummary();
        for(raceDuelEvent event:data) {
            if(event==null)continue;
            result.events++;
            Integer a=positive(event.getPositionA()),b=positive(event.getPositionB());
            if(a!=null&&b!=null){if(a<b)result.winsA++;else if(b<a)result.winsB++;}
            else if(a!=null)result.winsA++;
            else if(b!=null)result.winsB++;
            if(a!=null){result.positionSumA+=a;result.numericA++;}
            if(b!=null){result.positionSumB+=b;result.numericB++;}
            if("R".equals(event.getStatusA()))result.retirementsA++;
            if("R".equals(event.getStatusB()))result.retirementsB++;
        }
        return result;
    }
    private static Integer positive(Integer value){return value!=null&&value>0?value:null;}
}
