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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Main class of the server. Starts a new server and reads user input from
 * the console.
 * 
 * @author Simon Frankenberger
 */
public class Application implements Observer {
	/**
	 * Version number
	 */
	public static final String VERSION = "0.10";
	
	/**
	 * Program name
	 */
	public static final String NAME = "JDHCPD";
	
	/**
	 * Configuration file
	 */
	public static final File CONFIG = new File("config.ini");
	
	/**
	 * The netmask to announce
	 */
	protected static byte[] netmask;
	
	/**
	 * The IP-adress of the DHCP-server
	 */
	protected static byte[] server_ip;
	
	/**
	 * The first IP to offer
	 */
	protected static byte[] first_ip;
	
	/**
	 * The las IP to offer
	 */
	protected static byte[] last_ip;
	
	/**
	 * The lease time to announce
	 */
	protected static byte[] lease_time;
	
	/**
	 * The renewal time to announce
	 */
	protected static byte[] renewal_time;
	
	/**
	 * The DNS-Servers to announce
	 */
	protected static byte[] dns_servers;
	
	/**
	 * Parser for the configuration file
	 */
	protected static IniParser settings = null;
	
	/**
	 * An instance of this application for the singleton pattern.
	 */
	private static Application instance = null;
	
	/**
	 * Private static field to tell the server wether it should exit.
	 */
	private static boolean doExit = false;
	
	
	static {
		try {
	    settings = new IniParser(Application.CONFIG);
    }
    catch (IOException e) {
	    e.printStackTrace();
    }
	}
	
	/**
	 * Returns the instance of this class, implementation of the singleton
	 * pattern.
	 * 
	 * @return instance
	 */
	public static Application getInstance() {
		if(instance == null) instance = new Application();
		
		return instance;
	}
	
	/**
	 * @return first ip adress to offer
	 */
	public static byte[] getFirstIp() {
		return first_ip;
	}
	
	/**
	 * @return the last ip adress to offer
	 */
	public static byte[] getLastIp() {
		return last_ip;
	}
	
	/**
	 * @return the time how long this lease is valid
	 */
	public static byte[] getLeaseTime() {
		return lease_time;
	}
	
	/**
	 * @return the configured netmask to use
	 */
	public static byte[] getNetmask() {
		return netmask;
	}
	
	/**
	 * @return the time after which the client should renew its lease
	 */
	public static byte[] getRenewalTime() {
		return renewal_time;
	}
	
	/**
	 * @return the static ip adress of this server
	 */
	public static byte[] getServerIp() {
		return server_ip;
	}
	
	/**
	 * @return the dns servers to announce
	 */
	public static byte[] getDNSServers() {
		return dns_servers;
	}
	
	/**
	 * Main method used to instantiate the program.
	 * 
	 * @param args Arguments, currently only --nogui is implemented.
	 */
	public static void main(String[] args) {
		getInstance();
		
		if (args.length > 0 && args[0].equals("--nogui")) {
			instance.readConsole();
			instance.close();
		}
		else {
			gui = new Gui(instance);
		}

		
		s = new Server();
		s.addObserver(getInstance());
		
		s.start();
	}
	
	/**
	 * The server object
	 */
	private static Server s = null;
	
	/**
	 * A reference to the gui (if used)
	 */
	private static Gui gui = null;
	
	/**
	 * Creates a new instance of this program and starts the server
	 */
	public Application() {
		System.out.println("JDHCPD  Copyright (C) 2010  Simon Frankenberger");
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
		System.out.println("This is free software, and you are welcome to redistribute");
		System.out.println("it under certain conditions; read 'LICENCE' for details.");
	}
	
	/**
	 * Stops the server and exits the application
	 */
	protected void close() {
		s.abort();
		s.waitTillDone(1000);
		
		System.exit(0);
	}
	
	protected boolean doExit() {
		return doExit;
	}
	
	/**
	 * Retrieves and prints an option on the console.
	 * 
	 * @param setting The setting to retrieve
	 * @param new_val Was a new value set?
	 * 
	 * @return the value which was print
	 */
	private String get(String setting, boolean new_val) {
		String prefix = "";
		
		if(new_val) prefix = "new ";
		
		try {
			if (setting.equals("netmask")) {
				String val = Tools.byteToIp(netmask);
				update(null, prefix + "netmask=" + val);
				return val;
			}
			
			if (setting.equals("first_ip")) {
				String val = Tools.byteToIp(first_ip);
				update(null, prefix + "first_ip=" + val);
				return val;
			}
			
			if (setting.equals("last_ip")) {
				String val = Tools.byteToIp(last_ip);
				update(null, prefix + "last_ip=" + val);
				return val;
			}
			
			if (setting.equals("lease_time")) {
				String val = String.valueOf(Tools.byteToInt(lease_time));
				update(null, prefix + "lease_time=" + val);
				return val;
			}
			
			if (setting.equals("renewal_time")) {
				String val = String.valueOf(Tools.byteToInt(renewal_time));
				update(null, prefix + "renewal_time=" + val);
				return val;
			}
			
			if (setting.equals("dns_servers")) {
				StringBuilder tmp = new StringBuilder();
				
				for(int i=0; i<dns_servers.length; i += 4) {
					if(i > 0) tmp.append(" ");
					
					byte[] akt = new byte[4];
					System.arraycopy(dns_servers, i, akt, 0, 4);
					
					tmp.append(Tools.byteToIp(akt));
				}
				
				update(null, prefix + "dns_servers=" + tmp.toString());
				return tmp.toString();
			}
			
			update(null, "Variable not found.");
		}
		catch (Exception e) {
			update(null, e);
		}
		
		return null;
	}
	
	/**
	 * Prints the help on the console
	 */
	private void printHelp() {
		StringBuilder hlp = new StringBuilder();
		
		hlp.append(NAME + " v" + VERSION + "\n");
		hlp.append("=======================\n");
		hlp.append("help | ?:\n");
		hlp.append("-> Display this help message.\n");
		hlp.append("\n");
		hlp.append("list:\n");
		hlp.append("-> Print table of MAC-Adresses and their leases.\n");
		hlp.append("\n");
		hlp.append("nextip:\n");
		hlp.append("-> Display the next ip adress to offer.\n");
		hlp.append("\n");
		hlp.append("save [FILENAME]:\n");
		hlp.append("-> Save the leases in the given file.\n");
		hlp.append("\n");
		hlp.append("saveconfig:\n");
		hlp.append("-> Save the current configuration.\n");
		hlp.append("\n");
		hlp.append("load [FILENAME]:\n");
		hlp.append("-> Load the leases from the given file.\n");
		hlp.append("\n");
		hlp.append("set [SETTING] [VALUE]:\n");
		hlp.append("-> Set a setting to the given value. Valid settings:\n");
		hlp.append("-> netmask, first_ip, last_ip, lease_time, renewal_time, dns_servers\n");
		hlp.append("\n");
		hlp.append("get [SETTING]:\n");
		hlp.append("-> Display the value of the given setting. Valid settings:\n");
		hlp.append("-> netmask, first_ip, last_ip, lease_time, renewal_time, dns_servers\n");
		hlp.append("\n");
		hlp.append("add [MAC] [IP]:\n");
		hlp.append("-> Add an entry into the table of leases.\n");
		hlp.append("\n");
		hlp.append("remove [IP | MAC]:\n");
		hlp.append("-> Remove an entry from the table of leases.\n");
		hlp.append("\n");
		hlp.append("clear:\n");
		hlp.append("-> Remove all entries from the table of leases.\n");
		hlp.append("\n");
		hlp.append("cls:\n");
		hlp.append("-> Only with GUI: Clears the displayed log entries.\n");
		hlp.append("\n");
		hlp.append("quit | q:\n");
		hlp.append("-> Close the server and exit.\n");
		hlp.append("=======================");
		
		synchronized (System.out) {
			System.out.println(hlp.toString());
		}
		
		if(gui != null) {
			gui.writeMessage(hlp.toString());
		}
	}
	
	/**
	 * Method to read from the console and interpret user commands.
	 */
	private void readConsole() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		
		try {
			while (br != null && (line = br.readLine()) != null && !doExit) {
				handleCommand(line);
			}
		}
		catch (IOException e) {
			update(null, e);
		}
		finally {
			Tools.close(br);
		}
	}
	
	protected void handleCommand(String line) {
		line = line.toLowerCase().trim();
		if (line.length() == 0)
			return;
		
		if (line.equals("quit") || line.equals("q")) {
			doExit = true;
			return;
		}
		
		if (line.equals("help") || line.equals("?")) {
			printHelp();

			return;
		}
		
		if (line.equals("cls")) {
			if(gui != null) {
				gui.clearLog();
			}
		}
		
		if (line.equals("list")) {
			HashMap<String, Object> liste = s.getTable();
			
			update(null, "Known MAC-Adresses and IPs:");
			for (String mac : liste.keySet()) {
				update(null, mac + " => " + liste.get(mac));
			}
			
			if (liste.size() == 0) {
				update(null, "No entries in table yet.");
			}
			
			update(null, "< ================= >");

			return;
		}
		
		if (line.equals("saveconfig")) {
			try {
	      settings.save(CONFIG);
      }
      catch (IOException e) {
  			update(null, e);
      }
		}
		
		if (line.startsWith("remove")) {
			String[] parts = line.split(" ");
			
			if (parts.length == 2)
				s.removeClient(parts[1]);
			else
				update(null, "Argument(s) missing: 'remove [IP | MAC]'");

			return;
		}
		
		if (line.startsWith("add")) {
			String[] parts = line.split(" ");
			
			if (parts.length == 3)
				s.addClient(parts[1], parts[2]);
			else
				update(null, "Argument(s) missing: 'add [MAC] [IP]'");

			return;
		}
		
		if (line.equals("nextip")) {
			update(null, "Next IP to offer: " + s.getNextIP());

			return;
		}
		
		if (line.equals("clear")) {
			s.removeAll();

			return;
		}
		
		if (line.startsWith("delete")) {
			String[] parts = line.split(" ");
			if (parts.length == 2)
				new File(parts[1]).delete();
			else
				update(null, "Argument(s) missing: 'delete [FILENAME]'");

			return;
		}
		
		if (line.startsWith("save")) {
			String[] parts = line.split(" ");
			
			try {
				if (parts.length == 2) {
					s.saveLeases(new File(parts[1]));
					update(null, "Leases successfully saved in " + parts[1]);
				}
				else
					update(null, "Argument(s) missing: 'save [FILENAME]'");
			}
			catch (IOException e) {
				update(null, "Error while accessing file: "
				    + e.getLocalizedMessage());
			}

			return;
		}
		
		if (line.startsWith("load")) {
			String[] parts = line.split(" ");
			
			try {
				if (parts.length == 2) {
					s.loadLeases(new File(parts[1]));
					update(null, "Leases successfully loaded from " + parts[1]);
				}
				else
					update(null, "Argument(s) missing: 'load [FILENAME]'");
			}
			catch (IOException e) {
				update(null, "Error while accessing file: "
				    + e.getLocalizedMessage());
			}

			return;
		}
		
		if (line.startsWith("set")) {
			String[] parts = line.split(" ", 3);
			
			if (parts.length == 3)
				set(parts[1], parts[2]);
			else
				update(null, "Argument(s) missing: 'set [SETTING] [VALUE]'");

			return;
		}
		
		if (line.startsWith("get")) {
			String[] parts = line.split(" ");
			
			if (parts.length == 2)
				get(parts[1], false);
			else
				update(null, "Argument(s) missing: 'get [SETTING]'");

			return;
		}
		
		update(null, new Exception("Unknown command: " + line.split(" ")[0]));
	}
	/**
	 * Sets an option to the given value and prints it on the console.
	 * 
	 * @param setting The setting to set
	 * @param value The value to use
	 */
	private void set(String setting, String value) {
		try {
			if (setting.equals("netmask")) {
				byte[] data = Tools.ipToByte(value);
				if (Tools.checkNetmask(data)) {
					netmask = data;
					settings.set("global", "netmask", Tools.byteToIp(netmask));
				}
				else {
					update(null, "Invalid netmask: " + value);
					return;
				}
			}
			
			if (setting.equals("first_ip")) {
				byte[] data = Tools.ipToByte(value);
				if (Tools.checkIp(data)) {
					first_ip = data;
					settings.set("global", "first_ip", Tools.byteToIp(first_ip));
				}
				else {
					update(null, "Invalid ip: " + value);
					return;
				}
			}
			
			if (setting.equals("last_ip")) {
				byte[] data = Tools.ipToByte(value);
				if (Tools.checkIp(data)) {
					last_ip = data;
					settings.set("global", "last_ip", Tools.byteToIp(last_ip));
				}
				else {
					update(null, "Invalid ip: " + value);
					return;
				}
			}
			
			if (setting.equals("lease_time")) {
				lease_time = Tools.intToByte(Integer.valueOf(value));
				settings.set("global", "lease_time", Tools.byteToIp(lease_time));
			}
			
			if (setting.equals("renewal_time")) {
				renewal_time = Tools.intToByte(Integer.valueOf(value));
				settings.set("global", "lease_time", Tools.byteToIp(renewal_time));
			}
			
			if (setting.equals("dns_servers")) {
				String[] dns_servers = value.split(" ");
				int count = dns_servers.length;
				for(int i=0; i<dns_servers.length; i++) {
					byte[] akt = Tools.ipToByte(dns_servers[i]);
					if(!Tools.checkIp(akt)) {
						update(null, "Invalid ip: " + dns_servers[i] + ", ignored.");
						dns_servers[i] = "";
						count--;
					}
				}
				
				Application.dns_servers = new byte[count * 4];
				int aktPos = 0;
				for(int i=0; i< dns_servers.length; i++) {
					String ip = dns_servers[i];
					
					if(ip.length() > 0) {
						byte[] akt = Tools.ipToByte(ip);
						
						System.arraycopy(akt, 0, Application.dns_servers, aktPos*4, 4);
						aktPos++;
					}
				}
				
				settings.set("global", "dns_servers", get(setting, true));
				settings.save(CONFIG);
				
				return;
			}
			
			get(setting, true);
			settings.save(CONFIG);
		}
		catch (Exception e) {
			update(null, e);
		}
	}
	
	@Override
	public void update(Observable o, Object arg) {
		synchronized (System.out) {
			if (arg instanceof Throwable) {
				System.out.println("[" + Tools.currentDateTime() + "] "
				    + ((Throwable) arg).getLocalizedMessage());
			}
			else {
				System.out.println("[" + Tools.currentDateTime() + "] "
				    + arg.toString());
			}
		}
		
		if (gui != null) {
			gui.writeMessage("[" + Tools.currentDateTime() + "] "
					+ arg.toString());
		}
	}
}
