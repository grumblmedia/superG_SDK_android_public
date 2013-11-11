package com.cliqdigital.supergsdk.components;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.cliqdigital.supergsdk.R;
import com.cliqdigital.supergsdk.SuperG;
import com.cliqdigital.supergsdk.UnityPlugin;
import com.cliqdigital.supergsdk.utils.APIServer;
import com.cliqdigital.supergsdk.utils.Log;
import com.cliqdigital.supergsdk.utils.ProfileObject;
import com.cliqdigital.supergsdk.utils.SuperGLog;
import com.unity3d.player.UnityPlayer;

@SuppressLint("SetJavaScriptEnabled")
@SuppressWarnings("deprecation")

public class ThumbrWebViewDialog extends Dialog implements
android.view.View.OnClickListener {

	private static final String TAG = "ThumbrWebViewDialog";

	private String url_hasdcode ="access_token=";
	private static final int CONST_BTN_CLOSE_ID = android.R.id.closeButton;
	private static final int CONST_BTN_ABOUT_TITLE = android.R.id.button1;
	private String mURL;
	private WebView mWebView = null;
	private boolean mLoadCompleted = false;
	protected String mProcessingTitle;
	private View dialogTimeOut;
	private Context mContext = null;
	private int oldOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
	private boolean isShowButtonClose=true;
	boolean isRequested = false;
	private RelativeLayout aView = null;
	public boolean isNetwork = true;
	private Dialog dialog = null;
	private boolean finished = false;
	private AnimationDrawable anim;
	private FrameLayout frameLayout = null;
	private String url_Before="";
	public static String accToken="";
	public static String voidvar="";	
	public static String ERRORMESSAGE = SuperGLog.SystemErrorMessage.TYPE_4;
	Handler mHandler = new Handler();
	
//	private FacebookHelper fbh;
//	private boolean useNativeFacebook = false;

	class MyJavaScriptInterface  
	{ 

		public String getGmailName(){
			Account[] accounts = AccountManager.get(mContext).getAccounts();
			String gMailAddress = "";
			Log.i(TAG,"GET GMAIL WAS CALLED!");
			for (Account account : accounts)
			{
				if(account.name.endsWith("gmail.com"))
				{
					gMailAddress = account.name;
					Log.i(TAG,"Email: "+gMailAddress);
					break;

				}
			}
			return gMailAddress;
		}
	}	
	
	private class TimeOut implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (!mLoadCompleted) {
				if(mWebView != null)
					mWebView.stopLoading();
				mLoadCompleted = true;
				getWindow()
				.setBackgroundDrawable(
						new ColorDrawable(
								android.graphics.Color.TRANSPARENT));
				ThumbrWebViewDialog.this
				.setContentView(dialogTimeOut);

				ThumbrWebViewDialog.this.show();
				//mHandler.postDelayed(mCloseTimeOut, CLOSE_TIMEOUT);

			}
		}

	}

	public class CloseTimeOut implements Runnable{
		public void run() {
			// TODO Auto-generated method stub
			ThumbrWebViewDialog.this.dismiss();
			try {
				if(isClass("com.unity3d.player.UnityPlayer") && UnityPlayer.currentActivity != null ){
					UnityPlugin unity = new UnityPlugin();
					unity.dismiss(getProfile());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	CloseTimeOut mCloseTimeOut = new CloseTimeOut();
	TimeOut timeOut = new TimeOut();

	public String getURL() {
		return mURL;
	}

	public void setURL(String mURL) {
		this.mURL = mURL;
		SuperGLog.l(SuperGLog.TMB_LOGTYPE_INFO, "Loading URL: " + mURL);
		mLoadCompleted = false;

		try {
			if (mWebView != null) {

				Map<String, String> extraHeaders = new HashMap<String, String>();
				extraHeaders.put("X-Thumbr-Method", "sdk");
				extraHeaders.put("X-Thumbr-Version", mContext.getResources().getString(R.string.versionName));				  
				mWebView.loadUrl(mURL,extraHeaders);
				
			}
		} catch (Exception e) {
			// TODO: handle exception
			SuperGLog.fl(0,
					"Failed at: setURL(.. ) of class ThumbrWebViewDialog with exception: "
							+ e.getMessage());
		}

	}

	public void setProcessingTitle(String pProcessingTitle) {
		if (pProcessingTitle == null) {
			pProcessingTitle = "";
		}
		this.mProcessingTitle = pProcessingTitle;
	}

	public ThumbrWebViewDialog(Context context,Boolean showButtonClose) {
		super(context, R.style.Transparent);
		
//		fbh = new FacebookHelper(context);
//		if(fbh.isFacebookInstalled()){
//			useNativeFacebook = true; //Change facebook-button behavior
//		}
//		fbh.openSession();
		
		isShowButtonClose=showButtonClose;
		mContext = context;
		isNetwork = isNetworkAvailable();
		dialogTimeOut = getLayoutInflater().inflate(R.layout.image_time_out,
				null);
		//		TBrLog.l(TBrLog.TMB_LOGTYPE_INFO, "ThumbrWebViewDialog");
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		
		mProcessingTitle = "Please wait...";
		Context theContext = getContext();
		aView = (RelativeLayout) getWebViewLayout(theContext, 0);
		setContentView(aView);
		if (mWebView != null) {
			WebSettings aWS = mWebView.getSettings();
			aWS.setJavaScriptCanOpenWindowsAutomatically(true);
			aWS.setJavaScriptEnabled(true);
			MyJavaScriptInterface jsinterface = new MyJavaScriptInterface();
			
			//mWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
			mWebView.addJavascriptInterface(jsinterface, "ANDROID");
			
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.setAcceptCookie(true);
			mWebView.setWebViewClient(new onWebViewClient(){
				@Override
			    public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
			        handler.proceed();
			    }				
				
			});
		}
		frameLayout = (FrameLayout)findViewById(R.id.web_fram);
		//mHandler.postDelayed(timeOut, CONNECTION_TIMEOUT);

		String SDKLayout = theContext.getSharedPreferences("SuperGSettings", Context.MODE_PRIVATE).getString("SDKLayout", "");	
		if(SDKLayout.equals("appsilike")){		
			Button backbutton=(Button) findViewById(R.id.bottom_close);  
			backbutton.setOnClickListener(this);
		}
	}


	public void setOldOrientation(int oldOrientation, boolean isRequested) {
		this.oldOrientation = oldOrientation;
		this.isRequested = isRequested;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SuperGLog.l(SuperGLog.TMB_LOGTYPE_INFO,
				"onCreate Dialog. " + mWebView == null ? "(NULL)"
						: "(NOT NULL)");

		try {
			SharedPreferences settings = mContext.getSharedPreferences("SuperGSettings", Context.MODE_PRIVATE);
			if(settings.getInt("hideThumbrCloseButton", 0) == 1){
				View header = findViewById(R.id.include1);
				header.setVisibility(View.GONE);
			}
			
			if (mWebView == null) {
				Context theContext = getContext();
				aView = (RelativeLayout) getWebViewLayout(theContext, 0);
				setContentView(aView);
				if (mWebView != null) {
					MyJavaScriptInterface jsinterface = new MyJavaScriptInterface();
					//mWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
					mWebView.addJavascriptInterface(jsinterface, "ANDROID");
					
					WebSettings aWS = mWebView.getSettings();
					aWS.setJavaScriptCanOpenWindowsAutomatically(true);
					aWS.setJavaScriptEnabled(true);
					CookieManager cookieManager = CookieManager.getInstance();
					cookieManager.setAcceptCookie(true);
					mWebView.setWebViewClient(new onWebViewClient(){
						 @Override
						    public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
						        handler.proceed();
						    }						
						
					});
					
					mWebView.setBackgroundColor(0x00000000);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			SuperGLog.fl(0,
					"Failed at: onCreate(.. ) of class ThumbrWebViewDialog with exception: "
							+ e.getMessage());
		}
	}

	private ViewGroup getWebViewLayout(Context theContext) {
		try {
			RelativeLayout result = new RelativeLayout(theContext);
			RelativeLayout.LayoutParams lp_llheader = null;
			RelativeLayout.LayoutParams lp_relative = new RelativeLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			result.setLayoutParams(lp_relative);
			if (mWebView == null) {

				ImageView imIcon = new ImageView(theContext);
				imIcon.setImageResource(R.drawable.thumbr);
				lp_llheader = new RelativeLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				lp_llheader.addRule(RelativeLayout.ALIGN_PARENT_LEFT
						| RelativeLayout.ALIGN_PARENT_TOP);
				imIcon.setLayoutParams(lp_llheader);
				imIcon.setId(CONST_BTN_ABOUT_TITLE);
				imIcon.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						SuperGLog.lt(v.getContext(), 0, "About ThumBr Dialog");
					}
				});

				ImageView imClose = new ImageView(theContext);
				imClose.setImageResource(R.drawable.btn_close);
				lp_llheader = new RelativeLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				lp_llheader.addRule(RelativeLayout.ALIGN_PARENT_RIGHT
						| RelativeLayout.ALIGN_PARENT_TOP);
				imClose.setLayoutParams(lp_llheader);
				imClose.setId(CONST_BTN_CLOSE_ID);

				imClose.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						SuperGLog.lt(v.getContext(), 0,
								"play now (1)");
						Log.i(TAG,"Access token: "+accToken);
						//((Activity) mContext).setRequestedOrientation(oldOrientation);
						if(accToken != null && accToken != ""){
							setURL("thumbr://stop?access_token="+accToken);
						}else{
							setURL("thumbr://stop");					
						}
					}
				});

				mWebView = new WebView(theContext);
				WebSettings aWS = mWebView.getSettings();
				aWS.setJavaScriptCanOpenWindowsAutomatically(true);
				aWS.setJavaScriptEnabled(true);
				aWS.setUseWideViewPort(true);
				aWS.setBuiltInZoomControls(true);
				aWS.setSupportZoom(true);
				lp_llheader = new RelativeLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
				lp_llheader.addRule(RelativeLayout.ALIGN_PARENT_LEFT
						| RelativeLayout.ALIGN_PARENT_RIGHT
						| RelativeLayout.ALIGN_PARENT_BOTTOM);
				lp_llheader.addRule(RelativeLayout.BELOW, imIcon.getId());
				lp_llheader.addRule(RelativeLayout.BELOW, imClose.getId());
				mWebView.setLayoutParams(lp_llheader);

				result.addView(imIcon);
				result.addView(mWebView);
				result.addView(imClose);				
				imClose.bringToFront();
			}
			return (ViewGroup) result;
		} catch (Exception e) {
			// TODO: handle exception
			SuperGLog.fl(
					0,
					"Failed at: getWebViewLayout(.. ) of class ThumbrWebViewDialog with exception: "
							+ e.getMessage());
			return null;
		}

	}

	private ViewGroup getWebViewLayoutFromResource(Context theContext) {
		String SDKLayout = theContext.getSharedPreferences("SuperGSettings", Context.MODE_PRIVATE).getString("SDKLayout", "");

		RelativeLayout rl = (RelativeLayout) getLayoutInflater().inflate(R.layout.thumbr_layout_dialog_webview, null);	

		if(SDKLayout.equals("appsilike")){
			rl = (RelativeLayout) getLayoutInflater().inflate(R.layout.appsilike_layout_dialog_webview, null);
		}

		mWebView = (WebView) rl.findViewById(R.id.tbrlay_dialog_webview);


		WebSettings aWS = mWebView.getSettings();
		aWS.setSavePassword(false);
		ImageView imClose = (ImageView) rl
				.findViewById(R.id.img_dialog_header_close);
		if(!isShowButtonClose){
			imClose.setVisibility(View.GONE);
		}
		if (imClose != null) {
			imClose.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					try {

						SuperGLog.lt(v.getContext(), 0,
								"play now (2)");
						//ThumbrWebViewDialog.this.dismiss();
						Log.i(TAG,"Access token: "+accToken);
						//((Activity) mContext).setRequestedOrientation(oldOrientation);						
						if(accToken != null && accToken != ""){
							setURL("thumbr://stop?access_token="+accToken);
						}
						else if(mURL.contains("/start?")){
							setURL("thumbr://stop?");							
						}
						else{
							setURL("thumbr://stop");				
						}
					} catch (Exception e) {
						// TODO: handle exception
						SuperGLog.fl(
								0,
								"Failed at: getWebViewLayoutFromResource{... onClick(.. ) } of class ThumbrWebViewDialog with exception: "
										+ e.getMessage());
					}

				}
			});
		}
		return rl;
	}

	private ViewGroup getWebViewLayout(Context theContext, int pFrom) {
		switch (pFrom) {
		case 1:
			return getWebViewLayout(theContext);
		default:
			return getWebViewLayoutFromResource(theContext);
		}
	}

	public void onCreateDialog() {

		dialog = new Dialog(getContext(),R.style.Transparent);		
		dialog.setOnDismissListener((OnDismissListener) mContext);

		if (dialog != null) {

			String SDKLayout = mContext.getSharedPreferences("SuperGSettings", Context.MODE_PRIVATE).getString("SDKLayout", "");

			if(SDKLayout.equals("appsilike")){
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				if(Locale.getDefault().getLanguage().equals(mContext.getResources().getString(R.string.Dutch))){
					dialog.setContentView(R.layout.appsilike_popup_layout_for_dutch);
				}else if(Locale.getDefault().getLanguage().equals(mContext.getResources().getString(R.string.German))){
					dialog.setContentView(R.layout.appsilike_popup_layout_for_german);
				}
				else{
					dialog.setContentView(R.layout.appsilike_popup_layout_for_english);
				}
			}
			else
			{
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				if(Locale.getDefault().getLanguage().equals(mContext.getResources().getString(R.string.Dutch))){
					dialog.setContentView(R.layout.popup_layout_for_dutch);
				}else if(Locale.getDefault().getLanguage().equals(mContext.getResources().getString(R.string.German))){
					dialog.setContentView(R.layout.popup_layout_for_german);
				}
				else{
					dialog.setContentView(R.layout.popup_layout_for_english);
				}
			}


			ImageView imClose = (ImageView) dialog.findViewById(R.id.img_dialog_header_close);
			if (imClose != null) {
				imClose.setOnClickListener(new View.OnClickListener() {


					@Override
					public void onClick(View v) {
						try {
							Log.i(TAG,"NO CONNECTION VIEW DISMISSED");
							dialog.dismiss();

							((Activity) mContext).setRequestedOrientation(oldOrientation);
							//return;
						} catch (Exception e) {
							// TODO: handle exception
							SuperGLog.fl(
									0,
									"Cannot dismiss dialog for some reason: "
											+ e.getMessage());
						}

					}
				});
			}



			ImageButton bt = (ImageButton) dialog.findViewById(R.id.button1);
			bt.setOnClickListener(this);		
		}
	}


	private void onCreateAnimation(){


	}

	public void showNotNetwork(){
		onCreateDialog();
		if (dialog != null) {
			dialog.show();
		}
		mLoadCompleted = true;
		dismiss();
	}
	@Override
	public void show() {
		if (!isNetwork) {
			mLoadCompleted = true;
			onCreateDialog();
			if (dialog != null) {
				dialog.show();
			}
			dismiss();
			return;
		}
		super.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			Log.w("NTT", "onKeyDown");
			mWebView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		Log.w("NTT", "onBackPressed");
		// TODO Auto-generated method stub
		if (!mLoadCompleted && finished == false) {
			Log.w("NTT", "onBackPressed");
			mLoadCompleted = true;
			//dismiss();
			super.onBackPressed();
		}
	}

	public boolean isNetworkAvailable() {
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager) mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = null;
			if (connectivityManager != null) {
				networkInfo = connectivityManager
						.getActiveNetworkInfo();
			}
			return networkInfo == null ? false : networkInfo.isConnected();
		} catch (Exception e) {
			// TODO: handle exception
			SuperGLog.fl(
					0,
					"Failed at: isNetworkAvailable(.. )  of class ThumbrWebViewDialog with exception: "
							+ e.getMessage());
			return false;
		}
	}


	public class onWebViewClient extends WebViewClient {
		@JavascriptInterface
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

						

			SuperGLog.l(SuperGLog.TMB_LOGTYPE_INFO, "Load url: " + url
					+ " is completed.");
			if(url_Before.equals(url))
				return;
			finished = true;
			try {

				if(anim != null && anim.isRunning())
					anim.stop();

				isNetwork = isNetworkAvailable();
				if (mLoadCompleted || !isNetwork) {
					if (!isNetwork)
						((Activity) mContext)
						.setRequestedOrientation(oldOrientation);
					return;
				}

				view.setVisibility(View.VISIBLE);
				frameLayout.setVisibility(View.GONE);
				ThumbrWebViewDialog.this.hide();
				ThumbrWebViewDialog.this.setContentView(aView);
				SuperGLog.l(SuperGLog.TMB_LOGTYPE_DEBUG, view.getContentHeight()
						+ ", " + view.getContentDescription());
				ThumbrWebViewDialog.this.show();
				Log.w("NTT", "ThumbrWebViewDialog.this.show()");
				mLoadCompleted = true;
				url_Before=url;
				/* This call injects JavaScript into the page which just finished loading. */
				mWebView.loadUrl("javascript:(function() {" + "if(document.getElementById('gasp_profilebundle_profiletype_email').value.indexOf('@') === -1){document.getElementById('gasp_profilebundle_profiletype_email').value =ANDROID.getGmailName();} " +  "})()");
			} catch (Exception e) {
				// TODO: handle exception
				SuperGLog.fl(0,
						"Failed at: onPageFinished(.. ) of class ThumbrWebViewDialog with exception: "
								+ e.getMessage());
			}
		}



		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);	
			
			parserIntercept(url);
			if(url.contains("thumbr://stop")){
				mLoadCompleted=true;
				if(url.contains(url_hasdcode)){
					//isLogined();
					///mWebView.loadUrl(APIServer.getURLPrtal());
					//return;
				}
				SuperGLog.lt(mContext, 0,
						"Start Game");
				
				ThumbrWebViewDialog.this.dismiss();
				try {
					if(isClass("com.unity3d.player.UnityPlayer") && UnityPlayer.currentActivity != null ){
						UnityPlugin unity = new UnityPlugin();
						unity.dismiss(getProfile());
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				return;
			}else if((!url.contains("10.100.101") && !url.contains("file://") && !url.contains(".colo") && !url.contains("appsdorado") && !url.contains("scoreoid") && !url.contains("appsilike.mobi") && !url.equals("null") && !url.contains("demooij.it") && !url.contains("twimmer.com")&& !url.contains("thumbr.com") && !url.contains("superg.") && !url.contains("cliqdigital.com")) || url.contains("openinbrowser")){
				//Load in external browser
				mHandler.removeCallbacks(timeOut);
				view.stopLoading();				
				mLoadCompleted=true;
				view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

				Log.i(TAG,"Url is opened in default browser");

				//ThumbrWebViewDialog.this.dismiss();	
				return;
			}
			if(mWebView.isShown()){
				mLoadCompleted = false;
				//				mHandler.removeCallbacks(timeOut);
				//				mHandler.postDelayed(timeOut, CONNECTION_TIMEOUT);
			}
			SuperGLog.l(SuperGLog.TMB_LOGTYPE_INFO, "start to load url: " + url);
			if (mLoadCompleted || !isNetwork)
				return;
			try {
				String SDKLayout = mContext.getSharedPreferences("SuperGSettings", Context.MODE_PRIVATE).getString("SDKLayout", "");
				if (isNetwork) {
					if(finished == false){
						if(SDKLayout.equals("appsilike")){
							ThumbrWebViewDialog.this.setContentView(R.layout.appsilike_loading_layout);
						}
						else
						{
							ThumbrWebViewDialog.this.setContentView(R.layout.loading_layout);							
						}
					}
					else{
						frameLayout.setVisibility(View.VISIBLE);
					}

					onCreateAnimation();
					if(url.contains(url_hasdcode)){
						//isLogined();
						///mWebView.loadUrl(APIServer.getURLPrtal());
						//return;
					}
				}

			} catch (Exception e) {
				// TODO: handle exception
				SuperGLog.fl(0,
						"Failed at: onPageStarted(.. ) of class ThumbrWebViewDialog with exception: "
								+ e.getMessage());
			}

		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			SuperGLog.fl(0, "Couldn't connect to " + failingUrl + " due to "
					+ errorCode + " error");
			super.onReceivedError(view, errorCode, description, failingUrl);
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1) {
			isNetwork = isNetworkAvailable();
			if(isNetwork){
				mLoadCompleted=false;
				mWebView.reload();
				this.show();
				dialog.dismiss();
			}
		}

		if(v.getId()==R.id.bottom_close){		
			
			ThumbrWebViewDialog.this.dismiss();
			try {
				if(isClass("com.unity3d.player.UnityPlayer") && UnityPlayer.currentActivity != null ){
					UnityPlugin unity = new UnityPlugin();
					unity.dismiss(getProfile());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG,"clicked the back button");
		}
		//needs to be facebookbutton
/*
		if ((v.getId() == R.id.bt_facebook) && useNativeFacebook) {
			fbh.openSession();
		}
*/
	}



	private boolean parserIntercept(String s){

		if(s.contains("access_token=")){
			if(s.contains("?") == false){
				s = s.replace("#","?");
			}
			else{
				s = s.replace("#","&");	
			}
			

			Log.i(TAG,"intercept url string: "+s);
			Uri uri=Uri.parse(s);
			if(uri.getQueryParameter("access_token") != null){
				accToken = uri.getQueryParameter("access_token");
				mContext.getSharedPreferences(SuperG.ACCESSTOKEN,Context.MODE_PRIVATE).edit()
				.putString(SuperG.ACCESSTOKEN,uri.getQueryParameter("access_token")).commit();				
			}
			Log.i(TAG,"Access token: "+accToken);
			return true;
		}else{			
			return false;
		}

	}
	public String get_AccToken(){
		
		accToken = mContext.getSharedPreferences(SuperG.ACCESSTOKEN,Context.MODE_PRIVATE).getString(SuperG.ACCESSTOKEN, "");;
		return accToken;
	}


	/**
	 * get profile of user then logined
	 * @return
	 */
	public ProfileObject getProfile(){
		ProfileObject obj=null;		
		APIServer server=new APIServer(mContext);
		obj=server.getProfile(get_AccToken());
		
		return obj;	
	}


	public boolean hasEmail(){
		ProfileObject obj=null;		
		APIServer server=new APIServer(mContext);
		obj=server.getProfile(get_AccToken());			
		if(obj!=null){
			if(obj.getmEmail()!=null && obj.getmEmail()!="null"){
				Log.i(TAG,"Has e-mail: "+obj.getmEmail());
				return true;
			}
		}
		return false;
	}

	//check login
	public boolean isLogined(){
		if(getProfile()!=null && hasEmail()){
			Log.i(TAG,"log in succeeded with acc token: "+get_AccToken());
			mContext.getSharedPreferences(SuperG.LOGGEDIN,Context.MODE_PRIVATE).edit()
			.putBoolean(SuperG.LOGGEDIN,true).commit();
			return true;
		}
		mContext.getSharedPreferences(SuperG.LOGGEDIN,Context.MODE_PRIVATE).edit()
		.putBoolean(SuperG.LOGGEDIN,false).commit();
		return false;
	}

	public boolean isClass(String className)
	{
	    boolean exist = true;
	    try 
	    {
	        Class.forName(className);
	    } 
	    catch (ClassNotFoundException e) 
	    {
	        exist = false;
	    }
	    return exist;
	}
	
}
