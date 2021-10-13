package balancechecker.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public abstract class RequestUtil
{
	public static String requestForm(String url, String method, String contentType, String charset, Properties form, int expectedResponseCode, boolean debugResult)
	{
		Response response = sendRequest(url, method, contentType, charset, form);
		if(response.getResponseCode() != expectedResponseCode)
			System.out.println("WARN   : unexpected response code for '" + url + "': " + response.responseCode + " expected: " + expectedResponseCode);
		if(debugResult)
		{
			System.out.println("--------------------------------------------");
			System.out.println(response.getResponseString());
			System.out.println("--------------------------------------------");
		}
		return response.getResponseString();
	}
	
	public static Response sendRequest(String url, String method, String contentType, String charset, Properties args)
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
				connection.setRequestProperty("Content-Type", contentType);
				connection.setDoOutput(true);
				PrintWriter out = new PrintWriter(connection.getOutputStream());
				out.print(StringUtil.encodeParameters(args, charset));
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

	public static class Response
	{
		private String	responseString;
		private int		responseCode;

		public Response(String responseString, int responseCode)
		{
			super();
			this.responseString = responseString;
			this.responseCode = responseCode;
		}

		public String getResponseString()
		{
			return responseString;
		}

		public int getResponseCode()
		{
			return responseCode;
		}
	}
}
