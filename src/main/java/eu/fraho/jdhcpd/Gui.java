package eu.fraho.jdhcpd;

/*
JDHCP is a simple to configure and to use DHCP Server.
Copyright (C) 2010  Simon Frankenberger

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 * Class to represent the grafical user interface.
 * 
 * @author Simon Frankenberger
 */
public class Gui implements WindowListener, ActionListener,
		KeyListener {
	private JFrame window = new JFrame();
	
	private JTextPane gui_log = new JTextPane();
	private JTextField gui_cmd = new JTextField();
	private JPanel pnl_bottom = new JPanel(new BorderLayout());
	
	private JButton btn_cmd = new JButton("Execute");
	private Application parent = null;
	
	public Gui(Application parent) {
		// Store reference to parent
		this.parent = parent;
		
		// Add the several listeners
		window.addWindowListener(this);
		btn_cmd.addActionListener(this);
		gui_cmd.addKeyListener(this);
		
		// Configure the frame
		window.setSize(600, 400);
		window.setTitle(Application.NAME + " v" + Application.VERSION);
		window.setLayout(new BorderLayout());
		
		// Set up the textpane with the log
		gui_log.setText("["  + Tools.currentDateTime() + "] Interface started.");
		gui_log.setEditable(false);
		gui_log.setFocusable(false);
		
		// Add the elements to a wrapper panel
		pnl_bottom.add(new JLabel("Command: "), BorderLayout.WEST);
		pnl_bottom.add(gui_cmd);
		pnl_bottom.add(btn_cmd, BorderLayout.EAST);
		
		// Add all elements to the frame
		window.add(new JScrollPane(gui_log), BorderLayout.CENTER);
		window.add(pnl_bottom, BorderLayout.SOUTH);
		
		centerWindow();
		window.setVisible(true);
		gui_cmd.requestFocus();
	}

	private void centerWindow() {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Point p = new Point();
		
		p.x = (d.width - window.getSize().width) / 2;
		p.y = (d.height - window.getSize().height) / 2;
		
		window.setLocation(p);
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		window.setVisible(false);
		parent.close();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		parent.handleCommand(gui_cmd.getText());
		gui_cmd.setText("");
		
		if(parent.doExit()) {
			window.setVisible(false);
			parent.close();
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ENTER) {
			btn_cmd.doClick();
		}
	}
	
	protected void writeMessage(String msg) {
		synchronized (gui_log) {
			String newText = gui_log.getText() + "\n" + msg;
	    gui_log.setText(newText);
			gui_log.setCaretPosition(newText.length());
    }
	}
	
	protected void clearLog() {
		synchronized (gui_log) {
			gui_log.setText("");
		}
	}
	
	@Override
	public void windowActivated(WindowEvent e) {
	} // unused
	
	@Override
	public void windowClosed(WindowEvent e) {
	} // unused
	
	@Override
	public void windowDeactivated(WindowEvent e) {
	} // unused
	
	@Override
	public void windowDeiconified(WindowEvent e) {
	} // unused
	
	@Override
	public void windowIconified(WindowEvent e) {
	} // unused
	
	@Override
	public void windowOpened(WindowEvent e) {
	} // unused
	
	@Override
	public void keyTyped(KeyEvent e) {
	}	// unused
	
	@Override
	public void keyPressed(KeyEvent e) {
	}	// unused
}
