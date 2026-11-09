package com.example.f1app;
import org.junit.Test;
import static org.junit.Assert.*;

public class leaderboardAwardTokenTest {
    @Test public void acceptsCurrentOwner(){
        leaderboardAwardToken t=new leaderboardAwardToken();int request=t.bind("a");assertTrue(t.matches("a",request));}
    @Test public void rejectsAnotherOwner(){
        leaderboardAwardToken t=new leaderboardAwardToken();int request=t.bind("a");assertFalse(t.matches("b",request));}
    @Test public void rebindingRejectsLateResponse(){
        leaderboardAwardToken t=new leaderboardAwardToken();int old=t.bind("a");int current=t.bind("b");assertFalse(t.matches("a",old));assertTrue(t.matches("b",current));}
    @Test public void sameOwnerNewBindingStillRejectsOldResponse(){
        leaderboardAwardToken t=new leaderboardAwardToken();int old=t.bind("a");int current=t.bind("a");assertFalse(t.matches("a",old));assertTrue(t.matches("a",current));}
    @Test public void recyclingInvalidatesResponse(){
        leaderboardAwardToken t=new leaderboardAwardToken();int old=t.bind("a");t.clear();assertFalse(t.matches("a",old));}
    @Test public void clearThenReuseDoesNotReuseToken(){
        leaderboardAwardToken t=new leaderboardAwardToken();int old=t.bind("a");t.clear();int current=t.bind("a");assertNotEquals(old,current);assertTrue(t.matches("a",current));}
}
