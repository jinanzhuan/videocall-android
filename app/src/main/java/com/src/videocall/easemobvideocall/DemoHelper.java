package com.src.videocall.easemobvideocall;


import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMConferenceListener;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMError;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConferenceAttribute;
import com.hyphenate.chat.EMConferenceManager;
import com.hyphenate.chat.EMConferenceMember;
import com.hyphenate.chat.EMConferenceStream;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMOptions;


import com.hyphenate.chat.EMStreamStatistics;
import com.hyphenate.easeui.EaseUI;
import com.hyphenate.easeui.domain.EaseAvatarOptions;
import com.hyphenate.easeui.domain.EaseEmojicon;
import com.hyphenate.easeui.domain.EaseEmojiconGroupEntity;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.model.EaseAtMessageHelper;
import com.hyphenate.easeui.model.EaseNotifier;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.util.EMLog;
import com.src.videocall.easemobvideocall.db.DemoDBManager;

import com.src.videocall.easemobvideocall.db.UserDao;
import com.src.videocall.easemobvideocall.ui.LoginActivity;
import com.src.videocall.easemobvideocall.utils.ConferenceInfo;
import com.src.videocall.easemobvideocall.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hyphenate.easeui.utils.EaseUserUtils.getUserInfo;

public class DemoHelper {
    /**
     * data sync listener
     */
    public interface DataSyncListener {
        /**
         * sync complete
         * @param success true：data sync successful，false: failed to sync data
         */
        void onSyncComplete(boolean success);
    }

    protected static final String TAG = "DemoHelper";

    /**
     * EMEventListener
     */
    protected EMMessageListener messageListener = null;

	private static DemoHelper instance = null;
	
	private DemoModel demoModel = null;

	private String username;

    private ExecutorService executor;

    protected android.os.Handler handler;

	private final String KEY_REST_URL = "rest_url";

	private Context appContext;

	private String restUrl;

	private EaseUI easeUI;

    Queue<String> msgQueue = new ConcurrentLinkedQueue<>();


	private DemoHelper() {
        executor = Executors.newCachedThreadPool();
	}

	public synchronized static DemoHelper getInstance() {
		if (instance == null) {
			instance = new DemoHelper();
		}
		return instance;
	}

    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

	/**
	 * init helper
	 * 
	 * @param context
	 *            application context
	 */
	public void init(Context context) {
	    demoModel = new DemoModel(context);
	    EMOptions options = initChatOptions(context);

	    //wss://rtc-turn4-hsb.easemob.com/ws   //a1-hsb.easemob.com  //rtc-turn4-hsb.easemob.com  a1.easemob.com
		//沙箱 Rtcserver
		//turn4 Rtcserver
		//options.enableDNSConfig(true);

		appContext = context;
		options.setRestServer("a1-hsb.easemob.com"); //沙箱地址
		options.setIMServer("39.107.54.56");
		options.setImPort(6717);
		EMClient.getInstance().init(context, options);

		PreferenceManager.init(context);
		setGlobalListeners();
	}


    private EMOptions initChatOptions(Context context){
        Log.d(TAG, "init HuanXin Options");

		EMOptions options = new EMOptions();
        // set if accept the invitation automatically
        options.setAcceptInvitationAlways(false);
        // set if you need read ack
        options.setRequireAck(true);
        // set if you need delivery ack
        options.setRequireDeliveryAck(false);

        return options;
    }


	/**
	 * if ever logged in
	 * 
	 * @return
	 */
	public boolean isLoggedIn() {
		return EMClient.getInstance().isLoggedInBefore();
	}

	/**
	 * logout
	 * 
	 * @param unbindDeviceToken
	 *            whether you need unbind your device token
	 * @param callback
	 *            callback
	 */
	public void logout(boolean unbindDeviceToken, final EMCallBack callback) {
		Log.d(TAG, "logout: " + unbindDeviceToken);
		EMClient.getInstance().logout(unbindDeviceToken, new EMCallBack() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "logout: onSuccess");
			    reset();
				if (callback != null) {
					callback.onSuccess();
				}
			}
			@Override
			public void onProgress(int progress, String status) {
				if (callback != null) {
					callback.onProgress(progress, status);
				}
			}
			@Override
			public void onError(int code, String error) {
				Log.d(TAG, "logout: onSuccess");
                reset();
				if (callback != null) {
					callback.onError(code, error);
				}
			}
		});
	}

	public DemoModel getModel(){
        return (DemoModel) demoModel;
    }

    
    /**
     * set current username
     * @param username
     */
    public void setCurrentUserName(String username){
    	this.username = username;
    	//demoModel.setCurrentUserName(username);
    }
    
    /**
     * get current user's id
     */
    public String getCurrentUsernName(){
    	if(username == null){
    		//username = demoModel.getCurrentUsernName();
    	}
    	return username;
    }

    protected void onUserException(String exception){
        EMLog.e(TAG, "onUserException: " + exception);
        Intent intent = new Intent(appContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.putExtra(exception, true);
        appContext.startActivity(intent);
    }


	EMConnectionListener connectionListener;
	/**
	 * set global listener
	 */
	protected void setGlobalListeners(){
		//syncGroupsListeners = new ArrayList<>();
		//syncContactsListeners = new ArrayList<>();
		//syncBlackListListeners = new ArrayList<>();

		//isGroupsSyncedWithServer = demoModel.isGroupsSynced();
		//isContactsSyncedWithServer = demoModel.isContactSynced();
		//isBlackListSyncedWithServer = demoModel.isBacklistSynced();

		// create the global connection listener
		connectionListener = new EMConnectionListener() {
			@Override
			public void onDisconnected(int error) {
				/*EMLog.d("global listener", "onDisconnect" + error);
				if (error == EMError.USER_REMOVED) {
					onUserException(Constant.ACCOUNT_REMOVED);
				} else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
					onUserException(Constant.ACCOUNT_CONFLICT);
				} else if (error == EMError.SERVER_SERVICE_RESTRICTED) {
					onUserException(Constant.ACCOUNT_FORBIDDEN);
				} else if (error == EMError.USER_KICKED_BY_CHANGE_PASSWORD) {
					onUserException(Constant.ACCOUNT_KICKED_BY_CHANGE_PASSWORD);
				} else if (error == EMError.USER_KICKED_BY_OTHER_DEVICE) {
					onUserException(Constant.ACCOUNT_KICKED_BY_OTHER_DEVICE);
				}*/
			}

			@Override
			public void onConnected() {
				// in case group and contact were already synced, we supposed to notify sdk we are ready to receive the events
				/*if (isGroupsSyncedWithServer && isContactsSyncedWithServer) {
					EMLog.d(TAG, "group and contact already synced with servre");
				} else {
					if (!isGroupsSyncedWithServer) {
						asyncFetchGroupsFromServer(null);
					}

					if (!isContactsSyncedWithServer) {
						asyncFetchContactsFromServer(null);
					}

					if (!isBlackListSyncedWithServer) {
						asyncFetchBlackListFromServer(null);
					}
				}
			}*/
			}
		};


		//IntentFilter callFilter = new IntentFilter(EMClient.getInstance().callManager().getIncomingCallBroadcastAction());
		EMClient.getInstance().conferenceManager().addConferenceListener(new EMConferenceListener() {
			@Override public void onMemberJoined(EMConferenceMember member) {
				EMLog.i(TAG, String.format("member joined username: %s, member: %d", member.memberName,
						EMClient.getInstance().conferenceManager().getConferenceMemberList().size()));
			}

			@Override public void onMemberExited(EMConferenceMember member) {
				EMLog.i(TAG, String.format("member exited username: %s, member size: %d", member.memberName,
						EMClient.getInstance().conferenceManager().getConferenceMemberList().size()));
			}

			@Override public void onStreamAdded(EMConferenceStream stream) {
				EMLog.i(TAG, String.format("Stream added streamId: %s, streamName: %s, memberName: %s, username: %s, extension: %s, videoOff: %b, mute: %b",
						stream.getStreamId(), stream.getStreamName(), stream.getMemberName(), stream.getUsername(),
						stream.getExtension(), stream.isVideoOff(), stream.isAudioOff()));
				EMLog.i(TAG, String.format("Conference stream subscribable: %d, subscribed: %d",
						EMClient.getInstance().conferenceManager().getAvailableStreamMap().size(),
						EMClient.getInstance().conferenceManager().getSubscribedStreamMap().size()));

				if(!ConferenceInfo.Initflag){
					if(ConferenceInfo.getInstance().getConference().getConferenceRole() !=EMConferenceManager.EMConferenceRole.Admin){
						ConferenceInfo.getInstance().getConferenceStreamList().add(stream);
					}
				}
			}

			@Override public void onStreamRemoved(EMConferenceStream stream) {
				EMLog.i(TAG, String.format("Stream removed streamId: %s, streamName: %s, memberName: %s, username: %s, extension: %s, videoOff: %b, mute: %b",
						stream.getStreamId(), stream.getStreamName(), stream.getMemberName(), stream.getUsername(),
						stream.getExtension(), stream.isVideoOff(), stream.isAudioOff()));
				EMLog.i(TAG, String.format("Conference stream subscribable: %d, subscribed: %d",
						EMClient.getInstance().conferenceManager().getAvailableStreamMap().size(),
						EMClient.getInstance().conferenceManager().getSubscribedStreamMap().size()));
			}

			@Override public void onStreamUpdate(EMConferenceStream stream) {
				EMLog.i(TAG, String.format("Stream added streamId: %s, streamName: %s, memberName: %s, username: %s, extension: %s, videoOff: %b, mute: %b",
						stream.getStreamId(), stream.getStreamName(), stream.getMemberName(), stream.getUsername(),
						stream.getExtension(), stream.isVideoOff(), stream.isAudioOff()));
				EMLog.i(TAG, String.format("Conference stream subscribable: %d, subscribed: %d",
						EMClient.getInstance().conferenceManager().getAvailableStreamMap().size(),
						EMClient.getInstance().conferenceManager().getSubscribedStreamMap().size()));
			}

			@Override public void onPassiveLeave(int error, String message) {
				EMLog.i(TAG, String.format("passive leave code: %d, message: %s", error, message));
			}

			@Override public void onConferenceState(ConferenceState state) {
				EMLog.i(TAG, String.format("State code=%d", state.ordinal()));
			}

			@Override public void onStreamStatistics(EMStreamStatistics statistics) {
				EMLog.d(TAG, statistics.toString());
			}

			@Override public void onStreamSetup(String streamId) {
				EMLog.i(TAG, String.format("Stream id - %s", streamId));
			}

			@Override
			public void onSpeakers(List<String> speakers) {}

			@Override public void onReceiveInvite(String confId, String password, String extension) {
				EMLog.i(TAG, String.format("Receive conference invite confId: %s, password: %s, extension: %s", confId, password, extension));
				//goConference(confId, password, extension);
			}

			@Override
			public void onRoleChanged(EMConferenceManager.EMConferenceRole role) {
			}

			@Override
			public void onAttributesUpdated(EMConferenceAttribute[] attributes) {

			}
		});
		//register incoming call receiver
		//appContext.registerReceiver(callReceiver, callFilter);
		//register connection listener
		EMClient.getInstance().addConnectionListener(connectionListener);
		//register group and contact event listener
		//registerGroupAndContactListener();
		//register message event listener
		//registerMessageListener();

	}

    synchronized void reset(){
        DemoDBManager.getInstance().closeDB();
    }
}