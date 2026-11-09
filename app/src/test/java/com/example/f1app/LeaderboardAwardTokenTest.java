package com.example.f1app;
import org.junit.Test;
import static org.junit.Assert.*;

public class LeaderboardAwardTokenTest {
    @Test public void acceptsCurrentOwner(){LeaderboardAwardToken t=new LeaderboardAwardToken();int request=t.bind("a");assertTrue(t.matches("a",request));}
    @Test public void rejectsAnotherOwner(){LeaderboardAwardToken t=new LeaderboardAwardToken();int request=t.bind("a");assertFalse(t.matches("b",request));}
    @Test public void rebindingRejectsLateResponse(){LeaderboardAwardToken t=new LeaderboardAwardToken();int old=t.bind("a");int current=t.bind("b");assertFalse(t.matches("a",old));assertTrue(t.matches("b",current));}
    @Test public void sameOwnerNewBindingStillRejectsOldResponse(){LeaderboardAwardToken t=new LeaderboardAwardToken();int old=t.bind("a");int current=t.bind("a");assertFalse(t.matches("a",old));assertTrue(t.matches("a",current));}
    @Test public void recyclingInvalidatesResponse(){LeaderboardAwardToken t=new LeaderboardAwardToken();int old=t.bind("a");t.clear();assertFalse(t.matches("a",old));}
    @Test public void clearThenReuseDoesNotReuseToken(){LeaderboardAwardToken t=new LeaderboardAwardToken();int old=t.bind("a");t.clear();int current=t.bind("a");assertNotEquals(old,current);assertTrue(t.matches("a",current));}
}
