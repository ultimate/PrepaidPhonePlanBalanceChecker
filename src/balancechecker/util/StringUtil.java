package balancechecker.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

public abstract class StringUtil
{
	public static String encodeParameters(Properties args, String charset)
	{
		StringBuilder sb = new StringBuilder();
		try
		{

			for(String key : args.stringPropertyNames())
			{
				if(sb.length() > 0)
					sb.append("&");
				sb.append(URLEncoder.encode(key, charset));
				sb.append("=");
				sb.append(URLEncoder.encode(args.getProperty(key), charset));
			}
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static Properties loadProperties(String file) throws IOException
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
	public static String getJSONProperty(String json, String property)
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

	public static String getHTMLSnippet(String html, String tagName, String className)
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
}
