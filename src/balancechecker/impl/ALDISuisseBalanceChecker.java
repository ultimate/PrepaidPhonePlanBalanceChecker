package balancechecker.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.Properties;

import balancechecker.BalanceChecker;
import balancechecker.util.LoginUtil;
import balancechecker.util.RequestUtil;
import balancechecker.util.StringUtil;

public class ALDISuisseBalanceChecker extends BalanceChecker
{
	public static String	LOGIN_URL						= "https://app.aldi-mobile.ch/auth/login_password";
	public static String	LOGIN_METHOD					= "POST";
	public static int		LOGIN_RESPONSE_CODE_EXPECTED	= 200;
	public static String	LOGIN_USERNAME					= "username";
	public static String	LOGIN_PASSWORD					= "password";
	public static String	LOGIN_LANGUAGE					= "language";

	public static String	APP_URL							= "https://app.aldi-mobile.ch/cgi-bin/app2.cgi";
	public static String	APP_METHOD						= "POST";
	public static int		APP_RESPONSE_CODE_EXPECTED		= 200;
	public static String	APP_TASK						= "task";
	public static String	APP_TASK_VALUE					= "main";
	public static String	APP_MSISDN						= "msisdn";
	public static String	APP_LANGUAGE					= "lang";

	public static String	LOGOUT_URL						= "https://app.aldi-mobile.ch/auth/logout";
	public static String	LOGOUT_METHOD					= "POST";
	public static int		LOGOUT_RESPONSE_CODE_EXPECTED	= 200;
	public static String	LOGOUT_TASK_VALUE				= "logout";
	public static String	LOGOUT_CSRF						= "csrf";

	public static String	REQUEST_CHARSET					= "UTF-8";
	public static String	REQUEST_CONTENT_TYPE			= "application/x-www-form-urlencoded; charset=" + REQUEST_CHARSET;
	public static String	RESPONSE_STATUS					= "status";
	public static String	RESPONSE_STATUS_VALUE			= "OK";
	public static String	RESPONSE_BALANCE_TAG			= "div";
	public static String	RESPONSE_BALANCE_CLASS			= "balance";
	public static String	RESPONSE_BALANCE_WHOLE_CLASS	= "whole";
	public static String	RESPONSE_BALANCE_FRACTION_CLASS	= "rappen";

	public static String	DEFAULT_LANGUAGE				= "de";

	public ALDISuisseBalanceChecker(Properties properties)
	{
		super(properties);
	}

	public ALDISuisseBalanceChecker(Properties properties, boolean debug)
	{
		super(properties, debug);
	}

	@Override
	protected boolean validateProperties0()
	{
		// SHOW DIALOG IF USERNAME OR PW NOT SET
		if(properties.get(LOGIN_USERNAME) == null || properties.get(LOGIN_USERNAME).equals("") || properties.get(LOGIN_PASSWORD) == null
				|| properties.get(LOGIN_PASSWORD).equals(""))
		{
			if(!LoginUtil.showLoginDialog(properties, LOGIN_USERNAME, LOGIN_PASSWORD))
				return false;
		}

		// VALIDATE SETTINGS
		if(properties.get(LOGIN_USERNAME) == null || properties.get(LOGIN_USERNAME).equals(""))
		{
			System.out.println("ERROR  : credentials incomplete: no username");
			System.out.println("aborting...");
			return false;
		}
		else if(properties.get(LOGIN_PASSWORD) == null || properties.get(LOGIN_PASSWORD).equals(""))
		{
			System.out.println("ERROR  : credentials incomplete: no password");
			System.out.println("aborting...");
			return false;
		}
		else if(properties.get(LOGIN_LANGUAGE) == null || properties.get(LOGIN_LANGUAGE).equals(""))
		{
			System.out.println("WARN   : credentials incomplete: no language -> using default = " + DEFAULT_LANGUAGE + "...");
			properties.setProperty(LOGIN_LANGUAGE, DEFAULT_LANGUAGE);
		}
		else if(properties.get(SETTINGS_BALANCEMIN) == null || properties.get(SETTINGS_BALANCEMIN).equals(""))
		{
			System.out.println("WARN   : settings incomplete: no balance.min -> using default = " + DEFAULT_BALANCEMIN + "...");
			properties.setProperty(SETTINGS_BALANCEMIN, DEFAULT_BALANCEMIN);
		}
		return true;
	}

	public BigDecimal getBalance()
	{
		try
		{
			// SETUP COOKIE HANDLER
			CookieHandler cookieHandler = new CookieManager();
			CookieHandler.setDefault(cookieHandler);

			// PREPARE FORM DATA (LOGIN)
			Properties loginForm = new Properties();
			loginForm.setProperty(LOGIN_USERNAME, this.properties.getProperty(LOGIN_USERNAME));
			loginForm.setProperty(LOGIN_PASSWORD, this.properties.getProperty(LOGIN_PASSWORD));
			loginForm.setProperty(LOGIN_LANGUAGE, this.properties.getProperty(LOGIN_LANGUAGE));

			// PREPARE FORM DATA (APP)
			Properties appForm = new Properties();
			appForm.setProperty(APP_TASK, APP_TASK_VALUE);
			appForm.setProperty(APP_MSISDN, loginForm.getProperty(LOGIN_USERNAME));
			appForm.setProperty(APP_LANGUAGE, loginForm.getProperty(LOGIN_LANGUAGE));

			// PREPARE FORM DATA (LOGOUT)
			Properties logoutForm = new Properties();
			logoutForm.putAll(appForm);
			logoutForm.setProperty(APP_TASK, LOGOUT_TASK_VALUE);

			if(this.debug)
			{
				System.out.println("LOGIN  = " + loginForm);
				System.out.println("APP    = " + appForm);
				System.out.println("LOGOUT = " + logoutForm);
			}

			// REQUEST INFORMATION
			String response;

			// LOGIN
			response = RequestUtil.requestForm(LOGIN_URL, LOGIN_METHOD, REQUEST_CONTENT_TYPE, REQUEST_CHARSET, loginForm, LOGIN_RESPONSE_CODE_EXPECTED, this.debug);
			if(!RESPONSE_STATUS_VALUE.equalsIgnoreCase(StringUtil.getJSONProperty(response, RESPONSE_STATUS)))
				System.out.println("WARN   : unexpected response status: '" + StringUtil.getJSONProperty(response, RESPONSE_STATUS) + "'");
			logoutForm.put(LOGOUT_CSRF, StringUtil.getJSONProperty(response, LOGOUT_CSRF));
			System.out.println("LOGIN  : " + LOGOUT_CSRF + " = " + logoutForm.getProperty(LOGOUT_CSRF));
			Thread.sleep(1000);

			// GET BALANCE
			response = RequestUtil.requestForm(APP_URL, LOGIN_METHOD, REQUEST_CONTENT_TYPE, REQUEST_CHARSET, appForm, LOGIN_RESPONSE_CODE_EXPECTED, this.debug);
			String snippet = StringUtil.getHTMLSnippet(response, RESPONSE_BALANCE_TAG, RESPONSE_BALANCE_CLASS);
			String whole = StringUtil.getHTMLSnippet(snippet, RESPONSE_BALANCE_TAG, RESPONSE_BALANCE_WHOLE_CLASS);
			String fraction = StringUtil.getHTMLSnippet(snippet, RESPONSE_BALANCE_TAG, RESPONSE_BALANCE_FRACTION_CLASS);
			BigDecimal balance = new BigDecimal(whole + fraction);
			System.out.println("APP    : balance = " + balance);

			Thread.sleep(1000);

			// LOGOUT
			response = RequestUtil.requestForm(LOGOUT_URL, LOGOUT_METHOD, REQUEST_CONTENT_TYPE, REQUEST_CHARSET, logoutForm, LOGOUT_RESPONSE_CODE_EXPECTED,
					this.debug);
			System.out.println("LOGOUT : OK");

			return balance;
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException
	{
		String[] overrideArgs = new String[2];
		overrideArgs[0] = args[0];
		overrideArgs[1] = ALDISuisseBalanceChecker.class.getName();
		BalanceChecker.main(overrideArgs);
	}
}
