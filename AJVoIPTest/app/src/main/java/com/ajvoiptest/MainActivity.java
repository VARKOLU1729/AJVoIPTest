/**
 * IMPORTANT: You must copy the AJVoIP.aar into the \AJVoIPTest\AJVoIP folder!
 *
 * This is a very simple example using the AJVoIP SIP client library, capable to register and make call.
 *
 * Make sure to copy/include the AJVoIP.aar file to your project required libraries list!
 * You must copy the AJVoIP.aar into the \AJVoIPTest\AJVoIP folder!
 */


package com.ajvoiptest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Calendar;
import java.util.TimeZone;
import androidx.core.app.ActivityCompat;
import com.mizuvoip.jvoip.*; //import the Mizu SIP SDK! You must copy the AJVoIP.aar into the \AJVoIPTest\AJVoIP folder!


public class MainActivity extends Activity
{
    public static String LOGTAG = "AJVoIP";
    EditText mParams = null;
    EditText mDestNumber = null;
    Button mBtnStart = null;
    Button mBtnCall = null;
    Button mBtnHangup = null;
    Button mBtnTest = null;
    TextView mStatus = null;
    TextView mNotifications = null;
    SipStack mysipclient = null;
    Context ctx = null;
    public static MainActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //Message messageToMainThread = new Message();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;
        instance = this;

        mParams = (EditText) findViewById(R.id.parameters_view);
        mDestNumber = (EditText) findViewById(R.id.dest_number);
        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnCall = (Button) findViewById(R.id.btn_call);
        mBtnHangup = (Button) findViewById(R.id.btn_hangup);
        mBtnTest = (Button) findViewById(R.id.btn_test);
        mStatus = (TextView) findViewById(R.id.status);
        mNotifications = (TextView) findViewById(R.id.notifications);
        mNotifications.setMovementMethod(new ScrollingMovementMethod());

        DisplayLogs("oncreate");

        //SIP stack parameters separated by CRLF. Will be passed to AJVoIP with the SetParameters API call (you might  use the SetParameter API instead to pass the parameters separately)
        //Add other settings after your needs. See the documentation "Parameters" chapter for the full list of available settings.

        //default parameters:

        StringBuilder parameters = new StringBuilder();

        parameters.append("loglevel=5\r\n"); //for development you should set the loglevel to 5. for production you should set the loglevel to 1
        //parameters.append("notificationevents=4\r\n"); //we will use notification event objects only, but no need to set because it is set automatically by SetNotificationListener
        //parameters.append("startsipstack=1\r\n"); //auto start the sipstack (1/auto is the default)
        //parameters.append("register=1\r\n"); //auto register (set to 0 if you don't need to register or if you wish to call the Register explicitely later or set to 2 if must register)
        //parameters.append("proxyaddress=1\r\n"); //set this if you have a (outbound) proxy
        //parameters.append("transport=0\r\n"); //the default transport for signaling is -1/auto (UDP with failover to TCP). Set to 0 if your server is listening on UDP only, 1 if you need TCP or to 2 if you need TLS
        //parameters.append("realm=xxx\r\n"); //your sip realm. it might have to be set only if it is different from the serveraddress
        parameters.append("serveraddress=sip-bgn-int.ttsl.tel:49868\r\n");
        parameters.append("username=0605405970002\r\n");
        parameters.append("password=$eWPQD!Ypy\r\n");

        mParams.setText(parameters.toString());
        mDestNumber.setText("6301450563"); //default call-to number for our test (testivr3 is a music IVR access number on our test server at voip.mizu-voip.com)

        DisplayStatus("Ready.");

        mBtnStart.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)  //Start button click
            {
                DisplayLogs("Start on click");

                try{
                    // start SipStack if it's not already running
                    if (mysipclient == null) //check if AJVoIP instance already exists
                    {
                        DisplayLogs("Starting SIPStack");

                        //initialize the SIP engine
                        mysipclient = new SipStack();
                        mysipclient.Init(ctx);

                        //subscribe to notification events
                        MyNotificationListener listener = new MyNotificationListener();
                        mysipclient.SetNotificationListener(listener);

                        SetParameters(); //pass the configuration (parameters can be changed also later at run-time)

                        DisplayLogs("SIPStack Start");

                        //start the SIP engine
                        mysipclient.Start();
                        //mysipclient.Register();
                        instance.CheckPermissions();

                        DisplayLogs("SIPStack Started");
                    }
                    else
                    {
                        DisplayLogs("SIPStack already started");
                    }
                }catch (Exception e) { DisplayLogs("ERROR, StartSIPStack"); }
            }
        });

        mBtnCall.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) //Call button click
            {
                DisplayLogs("Call on click");

                String number = mDestNumber.getText().toString().trim();
                if (number == null || number.length() < 1)
                {
                    DisplayStatus("ERROR, Invalid destination number");
                    return;
                }

                if (mysipclient == null) {
                    DisplayStatus("ERROR, cannot initiate call because SipStack is not started");
                    return;
                }

                instance.CheckPermissions();

                if (mysipclient.Call(-1, number))
                {
                    /*
                        optinal flags (you might set these also for incoming calls):
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        if(Build.VERSION.SDK_INT >= 24) getWindow().setSustainedPerformanceMode(true);
                        instance.setShowWhenLocked(true);
                        instance.setTurnScreenOn(true);
                    */
                }
            }
        });

        mBtnHangup.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) //Hangup button click
            {
                DisplayLogs("Hangup on click");

                if (mysipclient == null)
                    DisplayStatus("ERROR, cannot hangup because SipStack is not started");
                else
                    mysipclient.Hangup();
            }
        });

        mBtnTest.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) //Test button click
            {
                //any test code here
                DisplayLogs("Toogle loudspeaker");
                if (mysipclient == null)
                    DisplayStatus("ERROR, SipStack not started");
                else
                    mysipclient.SetSpeakerMode(!mysipclient.IsLoudspeaker());
            }
        });
    }

    /**
    * Pass the parameters to AJVoIP
    */
    public void SetParameters()
    {
        String params = mParams.getText().toString();
        if (params == null || mysipclient == null) return;
        params = params.trim();

        DisplayLogs("SetParameters: " + params);

        mysipclient.SetParameters(params);

        //we could also set the parameters individually like:
        //mysipclient.SetParameter("loglevel",5);
    }


    /**
     * Handle audio record permissions
     */
    void CheckPermissions()
    {
        if (Build.VERSION.SDK_INT >= 23 && ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            //we need RECORD_AUDIO permission before to make/receive any call
            DisplayStatus("Microphone permission required");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 555);
            /*
                better permission management: https://developer.android.com/training/permissions/requesting
                some example code:
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA))
                        {
                            Toast tst = Toast.makeText(ctx, "Audio record permission required", Toast.LENGTH_LONG);
                            tst.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                            tst.show();
                            Handler handlerRating = new Handler();
                            handlerRating.postDelayed(new Runnable()
                            {
                                public void run()
                                {
                                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 555);
                                }
                            }, 1000);
                        }else
                        {
                            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 555);
                        }

                        //put this function outside:
                        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
                        {
                            switch (requestCode) {
                                case 555:{
                                        // If request is cancelled, the result arrays are empty.
                                        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                                        {
                                            //continue from here
                                        }else
                                        {
                                            //display failure
                                        }
                                }
                            }
                        }

            */
        }
    }

    /**
     * You will receive the notification events from JVoIP in this class by overriding the SIPNotificationListener base class member functions.
     * Don't block or wait in the overwritten functions for too long.
     * See the "Notifications" chapter in the documentation and/or the following javadoc for the details:
     * https://www.mizu-voip.com/Portals/0/Files/ajvoip_javadoc/index.html
     */
    class MyNotificationListener extends SIPNotificationListener
    {
        //here are some examples about how to handle the notifications:
        @Override
        public void onAll(SIPNotification e) {
            //we receive all notifications (also) here. we just print them from here
            DisplayLogs(e.getNotificationTypeText()+" notification received: " + e.toString());
        }

        //handle connection (REGISTER) state
        @Override
        public void onRegister( SIPNotification.Register e)
        {
            //check if/when we are registered to the SIP server
            if(!e.getIsMain()) return; //we ignore secondary accounts here

            switch(e.getStatus())
            {
                case SIPNotification.Register.STATUS_INPROGRESS: DisplayStatus("Registering..."); break;
                case SIPNotification.Register.STATUS_SUCCESS: DisplayStatus("Registered successfully."); break;
                case SIPNotification.Register.STATUS_FAILED: DisplayStatus("Register failed because "+e.getReason()); break;
                case SIPNotification.Register.STATUS_UNREGISTERED: DisplayStatus("Unregistered."); break;
            }
        }

        //an example for STATUS handling
        @Override
        public void onStatus( SIPNotification.Status e)
        {
            if(e.getLine() == -1) return; //we are ignoring the global state here (but you might check only the global state instead or look for the particular lines separately if you must handle multiple simultaneous calls)

            //log call state
            if(e.getStatus() >= SIPNotification.Status.STATUS_CALL_SETUP && e.getStatus() <= SIPNotification.Status.STATUS_CALL_FINISHED)
            {
                DisplayStatus("Call state is: "+ e.getStatusText());
            }

            //catch outgoing call connect
            if(e.getStatus() == SIPNotification.Status.STATUS_CALL_CONNECT && e.getEndpointType() == SIPNotification.Status.DIRECTION_OUT)
            {
                DisplayStatus("Outgoing call connected to "+ e.getPeer());


            //there are many things we can do on call connect. for example:
            //mysipclient.Dtmf(e.getLine(),"1"); //send DTMF digit 1
            //mysipclient.PlaySound(e.getLine(), "mysound.wav", 0, false, true, true, -1,"",false); //stream an audio file

            }
            //catch incoming calls
            else if(e.getStatus() == SIPNotification.Status.STATUS_CALL_RINGING && e.getEndpointType() == SIPNotification.Status.DIRECTION_IN)
            {
                DisplayStatus("Incoming call from "+ e.getPeerDisplayname());

                //auto accepting the incoming call (instead of auto accept, you might present an Accept/Reject button for the user which will call Accept / Reject)
                mysipclient.Accept(e.getLine());
            }
            //catch incoming call connect
            else if(e.getStatus() == SIPNotification.Status.STATUS_CALL_CONNECT && e.getEndpointType() == SIPNotification.Status.DIRECTION_IN)
            {
                DisplayStatus("Incoming call connected");
            }

        }

        //print important events (EVENT)
        @Override
        public void onEvent( SIPNotification.Event e)
        {
            DisplayStatus("Important event: "+e.getText());
        }

        //IM handling
        @Override
        public void onChat( SIPNotification.Chat e)
        {
            DisplayStatus("Message from "+e.getPeer()+": "+e.getMsg());

            //auto answer
            mysipclient.SendChat(-1, e.getPeer(),"Received");

        }


        //change the above function bodies after your app logic requirements and handle other notifications as required for your use-case

        /*
        //here is a template to handle all notifications
        //uncomment and replace the function bodies after your needs (by default it will just log the events, using most of the SIPNotification class member functions)
        @Override
        public void onAll( SIPNotification e)
        {
            DisplayLogs("ANY notification received. type: "+Integer.toString(e.getNotificationType())+"/"+e.getNotificationTypeText()+", as string: "+e.toString());
        }
        @Override
        public void onLog( SIPNotification.Log e)
        {
            DisplayLogs("LOG notification received. type: "+Integer.toString(e.getType())+"/"+e.getTypeText()+", message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onEvent( SIPNotification.Event e)
        {
            DisplayLogs("EVENT notification received. type: "+Integer.toString(e.getType())+"/"+e.getTypeText()+", message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onPopup( SIPNotification.Popup e)
        {
            DisplayLogs("POPUP notification received. message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onLine( SIPNotification.Line e)
        {
            DisplayLogs("LINE notification received. line: "+Integer.toString(e.getLine())+", as string: "+e.toString());
        }
        @Override
        public void onStatus( SIPNotification.Status e)
        {
            DisplayLogs("STATUS notification received. line: "+Integer.toString(e.getLine())+", status: "+Integer.toString(e.getStatus())+"/"+e.getStatusText()+", peer: "+e.getPeer()+"/"+e.getPeerDisplayname()+", local: "+e.getLocalname()+", eptype: "+Integer.toString(e.getEndpointType())+"/"+e.getEndpointTypeText()+", callid: "+e.getCallID()+", online: "+Integer.toString(e.getOnline())+"/"+e.getOnlineText()+", registered: "+Integer.toString(e.getRegistered())+"/"+e.getRegisteredText()+", incall: "+Integer.toString(e.getIncall())+"/"+e.getIncallText()+", mute: "+Integer.toString(e.getMute())+"/"+e.getMuteText()+", hold: "+Integer.toString(e.getHold())+"/"+e.getHoldText()+", encryption: "+Integer.toString(e.getEncrypted())+"/"+e.getEncryptedText()+", video: "+Integer.toString(e.getVideo())+"/"+e.getVideodText()+", group: "+e.getGroup()+", rtp_sent: "+Common.LongToStr(e.getRtpsent())+", rtp_rec: "+Common.LongToStr(e.getRtprec())+", rtp_lost: "+Common.LongToStr(e.getRtploss())+"/"+Common.LongToStr(e.getRtplosspercent())+"%, video_hold: "+Integer.toString(e.getVideoHold())+"/"+e.getVideoHoldText()+", video_rtp_sent: "+Common.LongToStr(e.getVideoRtpsent())+", video_rtp_rec: "+Common.LongToStr(e.getVideoRtprec())+", serverstats: "+e.getServerstats()+", as string: "+e.toString());
        }
        @Override
        public void onRegister( SIPNotification.Register e)
        {
            DisplayLogs("REGISTER notification received. line: "+Integer.toString(e.getLine())+", status: "+Integer.toString(e.getStatus())+"/"+e.getText()+"/"+e.getReason()+", ismain: "+Common.BoolToString(e.getIsMain())+", isfcm: "+Integer.toString(e.getFcm())+", user: "+e.getUser()+", as string: "+e.toString());
        }
        @Override
        public void onPresence( SIPNotification.Presence e)
        {
            DisplayLogs("PRESENCE notification received. peer: "+e.getPeer()+"/"+e.getPeerDisplayname()+"/"+e.getEmail()+", status: "+Integer.toString(e.getStatus())+"/"+e.getStatusText()+"/"+e.getDetails()+", as string: "+e.toString());
        }
        @Override
        public void onBLF( SIPNotification.BLF e)
        {
            DisplayLogs("BLF notification received. peer: "+e.getPeer()+"/"+e.getCallid()+" "+Integer.toString(e.getDirecton())+"/"+e.getDirectionText()+", status: "+Integer.toString(e.getStatus())+"/"+e.getStatusText()+", as string: "+e.toString());
        }
        @Override
        public void onDTMF( SIPNotification.DTMF e)
        {
            DisplayLogs("DTMF notification received. line: "+Integer.toString(e.getLine())+", message: "+e.getMsg()+", as string: "+e.toString());
        }
        @Override
        public void onINFO( SIPNotification.INFO e)
        {
            DisplayLogs("INFO notification received. line: "+Integer.toString(e.getLine())+", peer: "+e.getPeer()+", type: "+Integer.toString(e.getType())+"/"+e.getTypeText()+" message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onUSSD( SIPNotification.USSD e)
        {
            DisplayLogs("USSD notification received. line: "+Integer.toString(e.getLine())+", status: "+Integer.toString(e.getStatus())+"/"+e.getStatusText()+", message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onChat( SIPNotification.Chat e)
        {
            DisplayLogs("CHAT notification received. line: "+Integer.toString(e.getLine())+", peer: "+e.getPeer()+", message: "+e.getMsg()+", as string: "+e.toString());
        }
        @Override
        public void onChatReport( SIPNotification.ChatReport e)
        {
            DisplayLogs("CHATREPORT notification received. line: "+Integer.toString(e.getLine())+", peer: "+e.getPeer()+", status: "+Integer.toString(e.getStatus())+"/"+e.getStatusText()+", group: "+e.getGroup()+", md5: "+e.getMD5()+", id: "+Integer.toString(e.getID())+", as string: "+e.toString());
        }
        @Override
        public void onChatComposing( SIPNotification.ChatComposing e)
        {
            DisplayLogs("CHATCOMPOSING notification received. line: "+Integer.toString(e.getLine())+", peer: "+e.getPeer()+", status: "+e.getStatus()+"/"+e.getStatusText()+", as string: "+e.toString());
        }
        @Override
        public void onCDR( SIPNotification.CDR e)
        {
            DisplayLogs("CDR notification received. line: "+Integer.toString(e.getLine())+", peer: "+e.getPeer()+"/"+e.getPeerAddress()+", caller: "+e.getCaller()+", called: "+e.getCalled()+", connect: "+Common.LongToStr(e.getConnectTime())+" msec, duration: "+Common.LongToStr(e.getDuration())+" msec, disc_by: "+Integer.toString(e.getDiscParty())+"/"+e.getDiscPartyText()+", disc_reason: "+e.getDiscReason()+", as string: "+e.toString());
        }
        @Override
        public void onStart( SIPNotification.Start e)
        {
            DisplayLogs("START notification received. what: "+Integer.toString(e.getWhat())+"/"+e.getWhatText()+", as string: "+e.toString());
        }
        @Override
        public void onStop( SIPNotification.Stop e)
        {
            DisplayLogs("STOP notification received. what: "+Integer.toString(e.getWhat())+"/"+e.getWhatText()+", as string: "+e.toString());
        }
        @Override
        public void onShouldReset( SIPNotification.ShouldReset e)
        {
            DisplayLogs("SHOULDRESET notification received. reason: "+e.getReason()+", as string: "+e.toString());
        }
        @Override
        public void onPlayReady( SIPNotification.PlayReady e)
        {
            DisplayLogs("PLAYREADY notification received. line: "+Integer.toString(e.getLine())+", callid: "+e.getCallID()+", as string: "+e.toString());
        }
        @Override
        public void onSRS( SIPNotification.SRS e)
        {
            DisplayLogs("SRS notification received. line: "+Integer.toString(e.getLine())+", siprec session: "+e.getSessionID()+", call-id: "+e.getCallID()+", sipsession1: "+e.getSIPSessionID1()+", user1: "+e.getUserID1()+", aor1: "+e.getAOR1()+", name1: "+e.getName1()+", sipsession2: "+e.getSIPSessionID2()+", user2: "+e.getUserID2()+", aor2: "+e.getAOR2()+", name2: "+e.getName2()+", codec: "+e.getCodec()+", as string: "+e.toString());
        }
        @Override
        public void onSIP( SIPNotification.SIP e)
        {
            DisplayLogs("SIP notification received. dir: "+Integer.toString(e.getDirection())+"/"+e.getDirectionText()+", address: "+e.getAddress()+", message: "+e.getMessage()+", as string: "+e.toString());
        }
        @Override
        public void onBlock( SIPNotification.Block e)
        {
            DisplayLogs("BLOCK notification received. type: "+Integer.toString(e.getType())+"/"+e.getTypeText()+", message: "+e.getMessage()+", as string: "+e.toString());
        }
        @Override
        public void onVAD( SIPNotification.VAD e)
        {
            DisplayLogs("VAD notification received. line: "+Integer.toString(e.getLine())+", "+ (e.getLocalValid() ? "LOCAL: avg: "+Common.LongToString(e.getLocalAvg())+" max: "+Common.LongToString(e.getLocalMax())+" speaking: "+Common.BoolToString(e.getLocalSpeaking()) : "")+ (e.getRemoteValid() ? " REMOTE: avg: "+Common.LongToString(e.getRemoteAvg())+" max: "+Common.LongToString(e.getRemoteMax())+" speaking: "+Common.BoolToString(e.getRemoteSpeaking()) : "") +", as string: "+e.toString());
        }
        @Override
        public void onRTPE( SIPNotification.RTPE e)
        {
            DisplayLogs("RTPE notification received. profile: "+Integer.toString(e.getProfile())+", extension: "+e.getExtension()+", as string: "+e.toString());
        }
        @Override
        public void onRTPT( SIPNotification.RTPT e)
        {
            DisplayLogs("RTPT notification received. type: "+Integer.toString(e.getType())+"/"+e.getTypeText()+", SQU: "+Common.BoolToString(e.getSQU())+", id: "+Common.IntToString(e.getID())+", SCT: "+Common.BoolToString(e.getSCT())+", VF: "+Common.BoolToString(e.getVF())+", extension: "+e.getExtension()+", as string: "+e.toString());
        }
        @Override
        public void onRTPStat( SIPNotification.RTPStat e)
        {
            DisplayLogs("RTPSTAT notification received. quality: "+Integer.toString(e.getQuality())+"/"+e.getQualityText()+", sent: "+Common.LongToString(e.getSent())+", rec: "+Common.LongToString(e.getRec())+", issues: "+Common.LongToString(e.getIssues())+", lost: "+Common.LongToString(e.getLoss())+", as string: "+e.toString());
        }
        @Override
        public void onCredit( SIPNotification.Credit e)
        {
            DisplayLogs("CREDIT notification received. message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onRating( SIPNotification.Rating e)
        {
            DisplayLogs("RATING notification received. message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onServerContacts( SIPNotification.ServerContacts e)
        {
            DisplayLogs("SERVERCONTACTS notification received. message: "+e.getText()+", as string: "+e.toString());
        }
        @Override
        public void onMWI( SIPNotification.MWI e)
        {
            DisplayLogs("MWI notification received. has: "+Common.BoolToString(e.getHasMessage())+", vmnumber: "+e.getVMNumber()+", to: "+e.getTo()+", count: "+Integer.toString(e.getCount())+", message: "+e.getMessage()+", as string: "+e.toString());
        }
        @Override
        public void onNewContact( SIPNotification.NewContact e)
        {
            DisplayLogs("NEWUSER notification received. username: "+e.getUsername()+", displayname: "+e.getDisplayname()+", as string: "+e.toString());
        }
        @Override
        public void onAnswer( SIPNotification.Answer e)
        {
            DisplayLogs("ANSWER notification received. answer: "+e.getResult()+", request: "+e.getRequest()+", as string: "+e.toString());
        }
        @Override
        public void onVideo( SIPNotification.Video e)
        {
            DisplayLogs("VIDEO notification received. startstop: "+Integer.toString(e.getStartOrStop())+"/"+e.getStartOrStopText()+", type: "+Integer.toString(e.getType())+"/"+e.getTypeText()+", line: "+Integer.toString(e.getLine())+", reason: "+e.getReason()+", ip: "+e.getIp()+", port: "+Integer.toString(e.getPort())+", codec: "+e.getCodec()+", payload: "+Integer.toString(e.getPayload())+", quality: "+Integer.toString(e.getQuality())+", bw: "+Integer.toString(e.getBw())+", max_bw: "+Integer.toString(e.getMaxBw())+", fps: "+Integer.toString(e.getFps())+", max_fps: "+Integer.toString(e.getMaxFps())+", width: "+Integer.toString(e.getWidth())+", height: "+Integer.toString(e.getHeight())+", profilelevelid: "+Integer.toString(e.getProfilelevelid())+", profile: "+e.getProfile()+", pixelfmt: "+e.getPixelfmt()+", level: "+e.getLevel()+", pm: "+e.getPm()+", sprop: "+e.getSprop()+", srtp_alg: "+e.getSrtpAlg()+", srtp_key: "+e.getSrtpKey()+", srtp_remotekey: "+e.getSrtpRemoteKey()+", device: "+e.getDevice()+", fmtp: "+e.getFmtp()+", as string: "+e.toString());
        }
        */
    }

    public void DisplayStatus(String stat)
    {
        try{
        if (stat == null) return;
        if (mStatus != null) {
            if ( stat.length() > 70)
                mStatus.setText(stat.substring(0,60)+"...");
            else
                mStatus.setText(stat);
        }
        DisplayLogs(stat);
        }catch(Throwable e){  Log.e(LOGTAG, "ERROR, DisplayStatus", e); }
    }

    public void DisplayLogs(String logmsg)
    {
        try{
        if (logmsg == null || logmsg.length() < 1) return;

        if ( logmsg.length() > 2500) logmsg = logmsg.substring(0,300)+"...";
        logmsg = "["+ new java.text.SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime()) +  "] " + logmsg + "\r\n";

        Log.v(LOGTAG, logmsg);
        if (mNotifications != null) mNotifications.append(logmsg);
        }catch(Throwable e){  Log.e(LOGTAG, "ERROR, DisplayLogs", e); }
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        try{
            super.onDestroy();
            DisplayLogs("ondestroy");
            if (mysipclient != null)
            {
                DisplayLogs("Stop SipStack");
                mysipclient.Stop(true);
            }

            mysipclient = null;
        }catch(Throwable e){  Log.e(LOGTAG, "ERROR, on destroy", e); }
    }
}
