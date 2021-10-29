package balancechecker.util;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public abstract class MailUtil
{
	public static String	MAIL_USERNAME			= "mail.username";
	public static String	MAIL_PASSWORD			= "mail.password";
	public static String	MAIL_MSG_FROM			= "mail.message.from";
	public static String	MAIL_MSG_TO				= "mail.message.to";
	public static String	MAIL_MSG_SUBJECT		= "mail.message.subject";
	public static String	MAIL_MSG_CONTENT		= "mail.message.content";
	public static String	MAIL_MSG_CONTENT_TYPE	= "mail.message.content.type";

	public static void sendNotification(Properties mailProperties, Properties messageArgs) throws AddressException, MessagingException
	{
		// format message text
		String messageText = mailProperties.getProperty(MAIL_MSG_CONTENT);
		for(String key : messageArgs.stringPropertyNames())
		{
			messageText = messageText.replace(key, messageArgs.getProperty(key));
		}

		sendMessage(mailProperties, mailProperties.getProperty(MAIL_MSG_SUBJECT), messageText);
	}

	public static void sendMessage(Properties mailProperties, String subjectText, String messageText) throws AddressException, MessagingException
	{
		// Set the host smtp address
		mailProperties.put("mail.smtp.auth", "true");
		mailProperties.put("mail.smtp.ssl.enable", true);
		Authenticator auth = new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(mailProperties.getProperty(MAIL_USERNAME), mailProperties.getProperty(MAIL_PASSWORD));
			}
		};
		Session session = Session.getDefaultInstance(mailProperties, auth);
		session.setDebug(false);

		// create a message
		Message msg = new MimeMessage(session);

		// set the from and to address
		msg.setFrom(new InternetAddress(mailProperties.getProperty(MAIL_MSG_FROM)));
		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailProperties.getProperty(MAIL_MSG_TO)));

		// Setting the Subject and Content Type
		msg.setSubject(subjectText);
		msg.setContent(messageText, mailProperties.getProperty(MAIL_MSG_CONTENT_TYPE));

		// send message
		Transport.send(msg);
	}
}
