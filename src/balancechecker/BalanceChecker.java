package balancechecker;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import balancechecker.util.MailUtil;
import balancechecker.util.StringUtil;

public abstract class BalanceChecker
{
	public static String	SETTINGS_BALANCEMIN		= "balance.min";

	public static String	DEFAULT_BALANCEMIN		= "30";

	public static String	MAIL_MSG_BALANCE		= "%balance%";
	public static String	MAIL_MSG_BALANCE_MIN	= "%" + SETTINGS_BALANCEMIN + "%";

	protected Properties	properties;
	protected boolean		debug;
	protected BigDecimal	balanceMin;

	public BalanceChecker(Properties properties)
	{
		this(properties, false);
	}

	public BalanceChecker(Properties properties, boolean debug)
	{
		this.properties = properties;
		this.debug = debug;
	}

	public boolean checkBalance()
	{
		System.out.println("--------------------------------------------");
		System.out.println("STARTING CHECK");
		// GET ALL REQUIRED INFORMATION
		if(!validateProperties())
			return false;
		// GET BALANCE
		BigDecimal balance = getBalance();
		if(balance == null)
			return false;
		// CHECK AGAINT LIMIT
		if(balance.compareTo(balanceMin) < 0)
		{
			System.out.println("RESULT : insufficient balance... sending notification");
			// SEND NOTIFICATION
			if(!this.sendNotification(balance, balanceMin))
				return false;
		}
		else
		{
			System.out.println("RESULT : sufficient balance!");
		}
		System.out.println("CHECK FINISHED");
		System.out.println("--------------------------------------------");
		return true;
	}

	protected boolean validateProperties()
	{
		if(!this.validateProperties0())
			return false;
		try
		{
			this.balanceMin = new BigDecimal(properties.getProperty(SETTINGS_BALANCEMIN));
		}
		catch(NumberFormatException e)
		{
			System.out.println("WARN   : settings invalid: balance.min -> using default = " + DEFAULT_BALANCEMIN + "...");
			this.balanceMin = new BigDecimal(properties.getProperty(DEFAULT_BALANCEMIN));
		}
		return true;
	}

	protected abstract boolean validateProperties0();

	public abstract BigDecimal getBalance();

	public boolean sendNotification(BigDecimal balance, BigDecimal balanceMin)
	{
		try
		{
			// set message arguments
			Properties messageArgs = new Properties();
			messageArgs.setProperty(MAIL_MSG_BALANCE, balance.toString());
			messageArgs.setProperty(MAIL_MSG_BALANCE_MIN, balanceMin.toString());

			MailUtil.sendNotification(properties, messageArgs);
			return true;
		}
		catch(AddressException e)
		{
			e.printStackTrace();
		}
		catch(MessagingException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Properties properties = StringUtil.loadProperties(args[0]);

		@SuppressWarnings("unchecked")
		Class<? extends BalanceChecker> cls = (Class<? extends BalanceChecker>) Class.forName(args[1]);
		Constructor<? extends BalanceChecker> constructor = cls.getConstructor(Properties.class);
		BalanceChecker checker = constructor.newInstance(properties);
		
		checker.checkBalance();
		
		System.exit(0);
	}
}
