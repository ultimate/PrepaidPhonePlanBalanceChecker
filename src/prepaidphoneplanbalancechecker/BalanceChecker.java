package prepaidphoneplanbalancechecker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class BalanceChecker
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
	
	public static String	SETTINGS_BALANCEMIN				= "balance.min";
	
	public static String	DEFAULT_LANGUAGE				= "de";
	public static String	DEFAULT_BALANCEMIN				= "30";

	public static void main(String[] args) throws InterruptedException, IOException
	{
		System.out.println("--------------------------------------------");
		System.out.println("STARTING CHECK");
		// GET ALL REQUIRED INFORMATION
		Properties properties = loadProperties(args[0]);

		// SHOW DIALOG IF USERNAME OR PW NOT SET
		if(properties.get(LOGIN_USERNAME) == null || properties.get(LOGIN_USERNAME).equals("") || properties.get(LOGIN_PASSWORD) == null
				|| properties.get(LOGIN_PASSWORD).equals(""))
		{
			showLoginDialog(properties);
		}

		// VALIDATE SETTINGS
		if(properties.get(LOGIN_USERNAME) == null || properties.get(LOGIN_USERNAME).equals(""))
		{
			System.out.println("ERROR  : credentials incomplete: no username");
			System.out.println("aborting...");
			return;
		}
		else if(properties.get(LOGIN_PASSWORD) == null || properties.get(LOGIN_PASSWORD).equals(""))
		{
			System.out.println("ERROR  : credentials incomplete: no password");
			System.out.println("aborting...");
			return;
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
		
		BigDecimal balanceMin;
		try
		{
			balanceMin = new BigDecimal(properties.getProperty(SETTINGS_BALANCEMIN));
		}
		catch(NumberFormatException e)
		{
			System.out.println("WARN   : settings invalid: balance.min -> using default = " + DEFAULT_BALANCEMIN + "...");
			balanceMin = new BigDecimal(properties.getProperty(DEFAULT_BALANCEMIN));
		}
		
		// GET BALANCE
		BigDecimal balance = getBalance(properties, false);
		
		if(balance.compareTo(balanceMin) < 0)
		{
			System.out.println("RESULT : insufficient balance... sending notification");
			
			// TODO SEND NOTIFICATION
		}
		else
		{
			System.out.println("RESULT : sufficient balance!");
		}
		System.out.println("CHECK FINISHED");
		System.out.println("--------------------------------------------");
		System.exit(0);
	}

	private static BigDecimal getBalance(Properties properties, boolean debug) throws InterruptedException
	{
		// SETUP COOKIE HANDLER
		CookieHandler cookieHandler = new CookieManager();
		CookieHandler.setDefault(cookieHandler);

		// PREPARE FORM DATA (LOGIN)
		Properties loginForm = new Properties();
		loginForm.setProperty(LOGIN_USERNAME, properties.getProperty(LOGIN_USERNAME));
		loginForm.setProperty(LOGIN_PASSWORD, properties.getProperty(LOGIN_PASSWORD));
		loginForm.setProperty(LOGIN_LANGUAGE, properties.getProperty(LOGIN_LANGUAGE));

		// PREPARE FORM DATA (APP)
		Properties appForm = new Properties();
		appForm.setProperty(APP_TASK, APP_TASK_VALUE);
		appForm.setProperty(APP_MSISDN, loginForm.getProperty(LOGIN_USERNAME));
		appForm.setProperty(APP_LANGUAGE, loginForm.getProperty(LOGIN_LANGUAGE));

		// PREPARE FORM DATA (LOGOUT)
		Properties logoutForm = new Properties();
		logoutForm.putAll(appForm);
		logoutForm.setProperty(APP_TASK, LOGOUT_TASK_VALUE);

		if(debug)
		{
			System.out.println("LOGIN  = " + loginForm);
			System.out.println("APP    = " + appForm);
			System.out.println("LOGOUT = " + logoutForm);
		}

		// REQUEST INFORMATION
		String response;

		// LOGIN
		response = requestForm(LOGIN_URL, LOGIN_METHOD, loginForm, LOGIN_RESPONSE_CODE_EXPECTED, debug);
		if(!RESPONSE_STATUS_VALUE.equalsIgnoreCase(getJSONProperty(response, RESPONSE_STATUS)))
			System.out.println("WARN   : unexpected response status: '" + getJSONProperty(response, RESPONSE_STATUS) + "'");
		logoutForm.put(LOGOUT_CSRF, getJSONProperty(response, LOGOUT_CSRF));
		System.out.println("LOGIN  : " + LOGOUT_CSRF + " = " + logoutForm.getProperty(LOGOUT_CSRF));
		Thread.sleep(1000);

		// GET BALANCE
		response = requestForm(APP_URL, LOGIN_METHOD, appForm, LOGIN_RESPONSE_CODE_EXPECTED, debug);
		String snippet = getHTMLSnippet(response, RESPONSE_BALANCE_TAG, RESPONSE_BALANCE_CLASS);
		String whole = getHTMLSnippet(snippet, RESPONSE_BALANCE_TAG, RESPONSE_BALANCE_WHOLE_CLASS);
		String fraction = getHTMLSnippet(snippet, RESPONSE_BALANCE_TAG, RESPONSE_BALANCE_FRACTION_CLASS);
		BigDecimal balance = new BigDecimal(whole + fraction);
		System.out.println("APP    : balance = " + balance);

		Thread.sleep(1000);

		// LOGOUT
		response = requestForm(LOGOUT_URL, LOGOUT_METHOD, logoutForm, LOGOUT_RESPONSE_CODE_EXPECTED, debug);
		System.out.println("LOGOUT : OK");

		return balance;
	}

	private static void showLoginDialog(Properties properties)
	{
		String logintitle = "Login";
		final JLabel label = new JLabel("Please enter login credentials");
		final JLabel tfL = new JLabel("user / phone number");
		final JTextField tf = new JTextField(properties.getProperty(LOGIN_USERNAME));
		final JLabel pwL = new JLabel("password");
		final JPasswordField pw = new JPasswordField();

		// request Focus on username-TF when dialog is shown
		tfL.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent arg0)
			{
				tf.requestFocusInWindow();
			}

			@Override
			public void ancestorMoved(AncestorEvent arg0)
			{
			}

			@Override
			public void ancestorRemoved(AncestorEvent arg0)
			{
			}
		});

		System.out.println("PREPARE: asking for login credentials...");

		int result = JOptionPane.showConfirmDialog(null, new Object[] { label, tfL, tf, pwL, pw }, logintitle, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if(result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION)
			System.out.println("login canceled...");

		properties.setProperty(LOGIN_USERNAME, tf.getText());
		properties.setProperty(LOGIN_PASSWORD, new String(pw.getPassword()));
	}

	private static String requestForm(String url, String method, Properties form, int expectedResponseCode, boolean debugResult)
	{
		Response response = sendRequest(url, method, form);
		if(response.responseCode != expectedResponseCode)
			System.out.println("WARN   : unexpected response code for '" + url + "': " + response.responseCode + " expected: " + expectedResponseCode);
		if(debugResult)
		{
			System.out.println("--------------------------------------------");
			System.out.println(response.responseString);
			System.out.println("--------------------------------------------");
		}
		return response.responseString;
	}

	private static Response sendRequest(String url, String method, Properties args)
	{
		try
		{
			StringBuilder response = new StringBuilder();
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setAllowUserInteraction(false);
			connection.setRequestMethod(method);

			if(method.equals("POST"))
			{
				connection.setRequestProperty("Content-Type", REQUEST_CONTENT_TYPE);
				connection.setDoOutput(true);
				PrintWriter out = new PrintWriter(connection.getOutputStream());
				out.print(encodeParameters(args));
				out.close();
			}

			connection.connect();
			InputStream is = connection.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			int curr = bis.read();
			while(curr != -1)
			{
				response.append((char) curr);
				curr = bis.read();
			}
			bis.close();
			is.close();

			return new Response(response.toString(), connection.getResponseCode());
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static String encodeParameters(Properties args)
	{
		StringBuilder sb = new StringBuilder();
		try
		{

			for(String key : args.stringPropertyNames())
			{
				if(sb.length() > 0)
					sb.append("&");
				sb.append(URLEncoder.encode(key, REQUEST_CHARSET));
				sb.append("=");
				sb.append(URLEncoder.encode(args.getProperty(key), REQUEST_CHARSET));
			}
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return sb.toString();
	}

	private static Properties loadProperties(String file) throws IOException
	{
		Properties properties = new Properties();
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(file)));
		properties.load(bis);
		bis.close();
		return properties;
	}

	/**
	 * Note: this supports only simple first level JSON
	 * 
	 * @param json
	 * @return
	 */
	private static String getJSONProperty(String json, String property)
	{
		String startString = "\"" + property + "\":";
		String endString1 = ",\"";
		String endString2 = "}";

		int startIndex = json.indexOf(startString);

		if(startIndex == -1)
			return null;
		startIndex += startString.length();

		int endIndex1 = json.indexOf(endString1, startIndex);
		int endIndex2 = json.indexOf(endString2, startIndex);
		int endIndex = json.length();

		if(endIndex1 >= 0)
			endIndex = endIndex1;
		else if(endIndex2 >= 0)
			endIndex = endIndex2;

		String value = json.substring(startIndex, endIndex);
		if(value.startsWith("\"") && value.endsWith("\""))
			value = value.substring(1, value.length() - 1);

		return value;
	}

	private static String getHTMLSnippet(String html, String tagName, String className)
	{
		String startString = "<" + tagName + " class=\"" + className + "\">";
		String endString = "</" + tagName + ">";
		String matchString = "<" + tagName;

		int startIndex = html.indexOf(startString);

		if(startIndex == -1)
			return null;
		startIndex += startString.length();

		int currentIndex = startIndex;
		int endIndex;
		int matchIndex;
		int openTags = 1;

		while(openTags > 0)
		{
			endIndex = html.indexOf(endString, currentIndex);
			matchIndex = html.indexOf(matchString, currentIndex);
			if(matchIndex > 0 && matchIndex < endIndex)
			{
				openTags++;
				currentIndex = matchIndex + matchString.length();
			}
			else if(endIndex > 0 && (endIndex < matchIndex || matchIndex == -1))
			{
				openTags--;
				currentIndex = endIndex + endString.length();
			}
		}

		return html.substring(startIndex, currentIndex - endString.length());
	}

	private static class Response
	{
		private String	responseString;
		private int		responseCode;

		public Response(String responseString, int responseCode)
		{
			super();
			this.responseString = responseString;
			this.responseCode = responseCode;
		}
	}
}
