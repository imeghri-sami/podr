import java.awt.*;
import java.awt.event.*;

public class Irc extends Frame {
	public TextArea text;
	public TextField data;
	public Button read_button;
	public Button write_button;
	SharedObject sentence;
	static String myName;

	public static void main(String argv[]) {

		if (argv.length != 1) {
			System.out.println("java Irc <name>");
			return;
		}
		myName = argv[0];

		// initialisation (attendre l'ensemble des sites participants)
		Client.init(myName);

		// créer et diffuser un nouvel objet partagé
		SharedObject s = Client.publish("IRC", new String(""), false);

		// créer l'IHM
		new Irc(s);
	}

	public Irc(SharedObject s) {

		setLayout(new FlowLayout());

		text = new TextArea(10, 60);
		text.setEditable(false);
		text.setForeground(Color.red);
		add(text);

		data = new TextField(60);
		add(data);

		write_button = new Button("write");
		write_button.addActionListener(new writeListener(this));
		add(write_button);
		read_button = new Button("read");
		read_button.addActionListener(new readListener(this));
		add(read_button);

		setSize(530, 270);
		text.setBackground(Color.black);
		show();

		sentence = s;
	}
}

class readListener implements ActionListener {
	Irc irc;

	public readListener(Irc i) {
		irc = i;
	}

	public void actionPerformed(ActionEvent e) {

		irc.read_button.setForeground(Color.lightGray); // pour le feedback...
		// display the read value
		irc.text.append(((String) irc.sentence.read()) + "\n");
		irc.read_button.setForeground(Color.black);
	}
}

class writeListener implements ActionListener {
	Irc irc;
	String s;

	public writeListener(Irc i) {
		irc = i;
	}

	public void actionPerformed(ActionEvent e) {

		// write the object
		s = "[" + Irc.myName + "]:  " + irc.data.getText();
		irc.data.setText(s);// pour le feedback...
		irc.sentence.write(s);
		// reset text input field
		irc.data.setText("");
	}
}