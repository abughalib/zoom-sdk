package abugh.zoomsdk;

import android.app.Activity;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;

import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;


public class ZoomModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, MeetingServiceListener, LifecycleEventListener {

    /* React-Native deprecation
    * reactContext.getCurrentActivity() is private in latest release
    * getCurrentActivity() may return null, check null before implementation
    * */


    private final static String TAG = "zoomsdk";
    private final ReactApplicationContext reactContext;

    //final private Context mActivity;

    private Boolean isInitialized = false;
    private Promise initializePromise;
    private Promise meetingPromise;

    public ZoomModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        //this.mActivity = reactContext.getCurrentActivity(); //Not Public
        //this.mActivity = reactContext;
        reactContext.addLifecycleEventListener(this);
//        if(this.mActivity == null)
//            Log.i("mActivity", "It is NULL");
//        else Log.i("mActivity", "Not NULL->"+mActivity.getPackageName());
    }

    @Override
    public String getName() {
        return "zoomsdk";
    }

    @ReactMethod
    public void initialize(final String sdkKey, final String sdkSecret, final String domain, final Promise promise) {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        Log.i("Initialization",
                "sdkKey: "+sdkKey+"\nsdkSecret: "+sdkSecret+"\nDomain: "+domain);
        if (isInitialized) {
            promise.resolve("Already initialize Zoom SDK successfully.");
            return;
        }

        isInitialized = zoomSDK.isInitialized();

        try {
            initializePromise = promise;

            if (reactContext.getCurrentActivity() != null) {
                reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /*Initialize Zoom API parameters
                        * API KEY
                        * JwtToken
                        * DOMAIN -> zoom.us
                        * LogSize
                        * */

                        ZoomSDKInitParams params = new ZoomSDKInitParams();
                        params.appKey = sdkKey;
                        params.appSecret = sdkSecret;
                        params.enableLog = true;
                        params.logSize = 500;
                        //params.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
                        params.domain = domain;
                        //params.jwtToken = jwtToken;
                        zoomSDK.initialize(reactContext.getCurrentActivity(), ZoomModule.this, params);
                        /* Depreciated in v5.0.24437.0708
                        zoomSDK.initialize(mActivity, appKey, appSecret, webDomain, ZoomModule.this);
                         */
                        Log.i("Initialization Result ", isInitialized.toString());
                    }
                });
            }
        } catch (Exception ex) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
    }

    @ReactMethod
    public void startMeeting(
            final String displayName,
            final String meetingNo,
            final String userId,
            final int userType,
            final String zoomAccessToken,
            final String zoomToken,
            Promise promise
    ) {
        Log.i("StartMeeting",
                "DisplayName: "+displayName+"\nMeetingNo: "+
                        meetingNo+"\nUserId: "+userId+"\nUserType: "+userType+
                        "\nZoomAccessToken: "+zoomAccessToken+"\nZoomToken: "+zoomToken);
        try {
            meetingPromise = promise;

            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            if(!zoomSDK.isInitialized()) {
                promise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
                return;
            }

            final MeetingService meetingService = zoomSDK.getMeetingService();
            if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
                long lMeetingNo=0; //initialize Meeting number
                try {
                    lMeetingNo = Long.parseLong(meetingNo);
                } catch (NumberFormatException e) {
                    promise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
                    return;
                }

                if(meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
                    meetingService.returnToMeeting(reactContext.getCurrentActivity());
                    promise.resolve("Already joined zoom meeting");
                    return;
                }
            }
            /* Initializing Parameters*/

            StartMeetingOptions opts = new StartMeetingOptions();
            StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
            params.displayName = displayName;
            params.meetingNo = meetingNo;
            params.userId = userId;
            params.userType = userType;
            params.zoomAccessToken = zoomAccessToken; //Zoom Token

            int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
            Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

            if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
                promise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
            }
        } catch (Exception ex) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
    }

    @ReactMethod
    public void joinMeeting(
            final String displayName,
            final String meetingNo,
            Promise promise
    ) {
        Log.i("StartMeeting",
                "DisplayName: "+displayName+"\nMeetingNo: "+meetingNo);
        try {
            meetingPromise = promise;

            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            if(!zoomSDK.isInitialized()) {
                promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
                return;
            }

            final MeetingService meetingService = zoomSDK.getMeetingService();

            JoinMeetingOptions opts = new JoinMeetingOptions();
            JoinMeetingParams params = new JoinMeetingParams();
            params.displayName = displayName;
            params.meetingNo = meetingNo;

            int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
            Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

            if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
                promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
            }
        } catch (Exception ex) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
    }

    @ReactMethod
    public void joinMeetingWithPassword(
            final String displayName,
            final String meetingNo,
            final String password,
            Promise promise
    ) {
        Log.i("StartMeeting",
                "DisplayName: "+displayName+"\nMeetingNo: "+meetingNo+"\nPassword"+password);
        try {
            meetingPromise = promise;

            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            if(!zoomSDK.isInitialized()) {
                promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
                return;
            }

            final MeetingService meetingService = zoomSDK.getMeetingService();

            JoinMeetingOptions opts = new JoinMeetingOptions();
            JoinMeetingParams params = new JoinMeetingParams();
            params.displayName = displayName;
            params.meetingNo = meetingNo;
            params.password = password;

            int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
            Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

            if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
                promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
            }
        } catch (Exception ex) {
            promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
    }

    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
        if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
            initializePromise.reject(
                    "ERR_ZOOM_INITIALIZATION",
                    "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
            );
        } else {
            registerListener();
            initializePromise.resolve("Initialize Zoom SDK successfully.");
        }
    }

    @Override
    public void onZoomAuthIdentityExpired(){
    }

    @Override
    public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
        Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

        if (meetingPromise == null) {
            return;
        }

        if(meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
            meetingPromise.reject(
                    "ERR_ZOOM_MEETING",
                    "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
            );
            meetingPromise = null;
        } else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
            meetingPromise.resolve("Connected to zoom meeting");
            meetingPromise = null;
        }
    }

    private void registerListener() {
        Log.i(TAG, "registerListener");
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        MeetingService meetingService = zoomSDK.getMeetingService();
        if(meetingService != null) {
            meetingService.addListener(this);
        }
    }

    private void unregisterListener() {
        Log.i(TAG, "unregisterListener");
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if(zoomSDK.isInitialized()) {
            MeetingService meetingService = zoomSDK.getMeetingService();
            meetingService.removeListener(this);
        }
    }

    @Override
    public void onCatalystInstanceDestroy() {
        unregisterListener();
    }

    // React LifeCycle
    @Override
    public void onHostDestroy() {
        unregisterListener();
    }
    @Override
    public void onHostPause() {}
    @Override
    public void onHostResume() {}
}