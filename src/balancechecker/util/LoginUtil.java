package balancechecker.util;

import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public abstract class LoginUtil
{
	public static boolean showLoginDialog(Properties properties, String userKey, String pwKey)
	{
		String logintitle = "Login";
		final JLabel label = new JLabel("Please enter login credentials");
		final JLabel tfL = new JLabel("user / phone number");
		final JTextField tf = new JTextField(properties.getProperty(userKey));
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
		{
			System.out.println("login canceled...");
			return false;
		}

		properties.setProperty(userKey, tf.getText());
		properties.setProperty(pwKey, new String(pw.getPassword()));
		return true;
	}
}
