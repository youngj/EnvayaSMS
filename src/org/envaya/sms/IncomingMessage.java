package org.envaya.sms;

import android.content.Intent;
import org.envaya.sms.receiver.IncomingMessageRetry;

public abstract class IncomingMessage extends QueuedMessage {

    protected String from;
    protected long timestamp; // unix timestamp in milliseconds
    
    private ProcessingState state = ProcessingState.None;
        
    public enum ProcessingState
    {
        None,           // not doing anything with this sms now... just sitting around
        Queued,         // waiting to forward to server
        Forwarding,     // currently sending to server
        Scheduled,      // waiting for a while before retrying after failure forwarding
        Forwarded
    }    
    
    public IncomingMessage(App app, String from, long timestamp)
    {
        super(app);
        this.from = from;
        this.timestamp = timestamp;
    }
    
    public long getTimestamp()
    {
        return timestamp;
    }
    
    public ProcessingState getProcessingState()
    {
        return state;
    }
    
    public void setProcessingState(ProcessingState status)
    {
        this.state = status;
    }    

    public boolean isForwardable()
    {
        if (app.isTestMode() && !app.isTestPhoneNumber(from))
        {
            return false;
        }
        
        /* 
         * Don't forward messages from shortcodes or users with 
         * addresses like 'Vodacom' because they're likely to be 
         * messages from network, or spam. At least for network 
         * messages we should let them go in to the Messaging inbox 
         * because the person managing  this phone needs to know 
         * when they're out of credit, etc.
         * 
         * The minimum length of normal subscriber numbers doesn't 
         * seem to  be specified, but in practice seems to be 
         * at least 7 digits everywhere.
         */        
        int fromDigits = 0;
        int fromLength = from.length();
        
        for (int i = 0; i < fromLength; i++)
        {
            if (Character.isDigit(from.charAt(i)))
            {
                fromDigits++;
            }
        }
        
        return fromDigits >= 0;
    }
    
    public String getFrom()
    {
        return from;
    }
     
    protected Intent getRetryIntent() {
        Intent intent = new Intent(app, IncomingMessageRetry.class);
        intent.setData(this.getUri());
        return intent;
    }    
    
    public String getStatusText()
    {
        switch (state)
        {
            case Scheduled:
                return "scheduled retry";
            case Queued:
                return "queued to forward";
            case Forwarding:
                return "forwarding to server";            
            default:
                return "";
        }
    }    
    
    public String getDescription()
    {
        return getDisplayType() + " from " + getFrom();
    }
    
    public abstract void tryForwardToServer();
}
