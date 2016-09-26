package com.tilab.ca.call_on_detect;


public class CallEventWaitTimer {
    
    private final int waitMillis;
    private long creationTime;
    
    public static final int T_1_SECOND = 1000;
    public static final int T_1_MINUTE = T_1_SECOND*60;
    public static final int T_3_MINUTE = 3*T_1_MINUTE;

    public CallEventWaitTimer(int waitMillis) {
        this.waitMillis = waitMillis;
        this.creationTime = System.currentTimeMillis();
    }
    
    public boolean hasExpired(){
        return System.currentTimeMillis() - creationTime > waitMillis;
    }
    
    public long getRemainingTime(){
        return waitMillis - (System.currentTimeMillis() - creationTime);
    }
    
    public void reset(){
        this.creationTime = System.currentTimeMillis();
    }
}
