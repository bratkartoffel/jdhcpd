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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.bucknell.net.JDHCP.DHCPMessage;
import edu.bucknell.net.JDHCP.DHCPOptions;
import edu.bucknell.net.JDHCP.DHCPSocket;

/**
 * Class which represents the server. Implemented as a thread which
 * listens on all interfaces for incoming DHCP-requests.<br>
 * Handles all requests and offers ip-adresses which are in the
 * given range.
 * 
 * @author sfrankenberger
 */
public class Server extends MyThread {
	/**
	 * Private class which defines IP-Adress ranges and provides the ability
	 * to check wheter a given IP is in the given range.
	 * 
	 * @author sfrankenberger
	 */
	private static class IPRange {
		/**
		 * The first ip adress in this range
		 */
		private int[] i_from;
		
		/**
		 * The last ip adress in this range
		 */
		private int[] i_till;
		
		/**
		 * Creates a new ip adress range between the given adresses. (including)
		 * 
		 * @param from the first ip in this range
		 * @param till the last ip in this range
		 */
		public IPRange(byte[] from, byte[] till) {
			i_from = Tools.byteAToIntA(from);
			i_till = Tools.byteAToIntA(till);
		}
		
		/**
		 * Checks whether the given ip adress is in this range.
		 * 
		 * @param ip the ip adress to check
		 * @return is the given ip adress in this range?
		 */
		public boolean inRange(byte[] ip) {
			int[] i_ip = Tools.byteAToIntA(ip);
			
			for (int i = 0; i < 4; i++) {
				if (i_ip[i] < i_from[i] || i_ip[i] > i_till[i])
					return false;
			}
			
			return true;
		}
		
		/**
		 * Returns the next ip adress in this range, starting from the given
		 * ip adress.
		 * 
		 * @param ip the ip adress to start from.
		 * @return the next ip adress in this range.
		 */
		public byte[] nextIp(byte[] ip) {
			int[] akt = {
			    0xFF & ip[0], 0xFF & ip[1], 
			    0xFF & ip[2], 0xFF & ip[3]
			};
			akt[3]++;
			
			if (akt[3] > i_till[3]) {
				akt[3] = 1;
				akt[2]++;
			}
			
			if (akt[2] > i_till[2]) {
				akt[2] = 0;
				akt[1]++;
			}
			
			if (akt[1] > i_till[1]) {
				akt[1] = 0;
				akt[0]++;
			}
			
			byte[] back = {
			    (byte) akt[0], (byte) akt[1], 
			    (byte) akt[2], (byte) akt[3]
			};
			
			if (inRange(back))
				return back;
			else
				return new byte[] {
				    0, 0, 0, 0
				};
		}
	}
	
	/**
	 * Private class to represent a lease. Stores the ip-adress and the time the
	 * lease was granted.
	 * 
	 * @author sfrankenberger
	 */
	private static class Lease {
		/**
		 * The time how long a lease should be valid.
		 */
		private int lease_time = Tools.byteToInt(Application.getLeaseTime()) * 1000;
		
		/**
		 * The ip adress assigned to this lease.
		 */
		private byte[] ip_byte;
		
		/**
		 * A string representation of {@link #ip_byte}
		 */
		private String ip_string;
		
		/**
		 * When was this lease given / renewed?
		 */
		private long leased;
		
		/**
		 * Is this a manual lease?
		 */
		private boolean is_manual = false;
		
		/**
		 * Creates a new lease for the given ip adress.
		 * @param ip the ip adress to lease
		 */
		public Lease(byte[] ip) {
			ip_byte = ip.clone();
			leased = System.currentTimeMillis() + 600000;
			
			ip_string = Tools.byteToIp(ip_byte);
		}
		
		public Lease(byte[] ip, boolean is_manual) {
			this(ip);
			this.is_manual = is_manual;
		}
		
		/**
		 * Creates a new lease with a custom leased timestamp.
		 * 
		 * @param ip the ip adress to lease
		 * @param leased_timestamp the timestamp when this lease was granted.
		 */
		public Lease(String ip, String leased_timestamp) {
			this(Tools.ipToByte(ip));
			
			leased = Long.valueOf(leased_timestamp);
		}
		
		/**
		 * @return the ip adress of this lease.
		 */
		public byte[] getIp() {
			return ip_byte;
		}
		
		/**
		 * @return the string representation of the leased ip adress
		 */
		public String getIpString() {
			return ip_string;
		}
		
		/**
		 * @return when was this lease granted?
		 */
		public long getLeasedTimestamp() {
			return leased;
		}
		
		/**
		 * @return is this lease still valid?
		 */
		public boolean isValid() {
			return (leased + lease_time >= System.currentTimeMillis());
		}
		
		/**
		 * @return was the lease manually added?
		 */
		public boolean isManual() {
			return is_manual;
		}
		
		/**
		 * Refreshes a lease (renew it)
		 */
		public void refreshLease() {
			leased = System.currentTimeMillis() + 1000;
		}
		
		public String toString() {
			return getIpString() + ", leased until: "
			    + Tools.dateTime(leased + lease_time);
		}
	}
	
	/**
	 * Private class which checks the leases and deletes expired leases.
	 * 
	 * @author sfrankenberger
	 */
	private class LeaseTimer extends MyThread {
		/**
		 * Constructor which initialises the timer.
		 */
		public LeaseTimer() {
			setName("Lease_Timer");
		}
		
		/**
		 * Private method to check the leases from {@link Server#adresses} and
		 * deletes expired ones.
		 */
		private void check() {
			ArrayList<String> toRemove = new ArrayList<String>();
			
			synchronized (adresses) {
				for (String mac : adresses.keySet()) {
					if (!adresses.get(mac).isValid()) {
						toRemove.add(mac);
					}
				}
				
				for (String mac : toRemove) {
					message("Lease for " + adresses.get(mac).getIpString() + " expired.");
					removeLeaseIfNotManual(mac);
				}
			}
		}
		
		/**
		 * Periodically checks the leases.
		 */
		public void run() {
			while (doContinueWork()) {
				check();
				
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * A hashmap stiring the mac adresses and leases.
	 */
	private HashMap<String, Lease> adresses;
	
	/**
	 * Socket to send / receive dhcp messages.
	 */
	private DHCPSocket socket;
	
	/**
	 * The adress range we should offer
	 */
	private IPRange iprange;
	
	/**
	 * An timer which deletes expired leases.
	 */
	private LeaseTimer lease_timer;
	
	/**
	 * Creates a new instance of this server
	 */
	public Server() {
		loadSettings();
		adresses = new HashMap<String, Lease>();
		socket = null;
		iprange = new IPRange(Application.getFirstIp(), Application.getLastIp());
		
		lease_timer = new LeaseTimer();
		
		setName("JDHCPD_Server");
	}
	
	private void loadSettings() {
		try {
			IniParser settings = Application.settings;
			
			Application.first_ip = Tools.ipToByte(settings.getString("global", "first_ip",
			    "192.168.0.20"));
			Application.last_ip = Tools.ipToByte(settings.getString("global", "last_ip",
			    "192.168.0.254"));
			Application.netmask = Tools.ipToByte(settings.getString("global", "netmask",
			    "255.255.255.0"));
			Application.server_ip = Tools.ipToByte(settings.getString("global", "server_ip",
			    "192.168.0.1"));
			Application.lease_time = Tools.intToByte(settings.getInt("global", "lease_time",
			    Integer.MAX_VALUE));
			Application.renewal_time = Tools.intToByte(settings.getInt("global", "renewal_time",
			    (int) (Integer.MAX_VALUE * 0.75)));
			
			String[] dns_servers = settings.getString("global", "dns_servers",
					"").split(" |\t");
			
			
			int count = dns_servers.length;
			for(int i=0; i<dns_servers.length; i++) {
				byte[] akt = Tools.ipToByte(dns_servers[i]);
				if(!Tools.checkIp(akt)) {
					Application.getInstance().update(null, "Invalid DNS Server given in configuration file: " + dns_servers[i]);
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
			
			settings.save(Application.CONFIG);
		}
		catch (IOException e) {
			e.printStackTrace();
			
			System.exit(1);
		}
	}
	
	@Override
	public void abort() {
		lease_timer.abort();
		super.abort();
	}
	
	/**
	 * Manually adds a mac adress with a new lease.
	 * 
	 * @param mac the mac adress to use
	 * @param ip the ip adress to assign
	 */
	public void addClient(String mac, String ip) {
		synchronized (adresses) {
			adresses.put(mac, new Lease(Tools.ipToByte(ip), true));
		}
	}
	
	/**
	 * Private method which answers a DHCPREQUEST message
	 * 
	 * @param m the request received from the client
	 */
	private void answerRequest(DHCPMessage m) {
		String mac = Tools.byteToMac(m.getChaddr());
		
		if (adresses.containsKey(mac)) {
			sendAck(m, adresses.get(mac).getIp(), false);
			adresses.get(mac).refreshLease();
			return;
		}
		
		byte[] ip = m.getOption(DHCPOptions.OPTION_DHCP_IP_ADRESS_REQUESTED);
		
		if (ip == null || !isIpFree(Tools.byteToIp(ip))) {
			ip = new byte[4];
			
			sendNAck(m, ip);
		}
		else {
			sendAck(m, ip, false);
			
			synchronized (adresses) {
				adresses.put(Tools.byteToMac(m.getChaddr()), new Lease(ip));
			}
		}
		
		return;
	}
	
	/**
	 * Helper method to get the ip adress of the given mac, or
	 * if unknown the next free ip in our range.
	 * 
	 * @param mac the mac adress to look up
	 * @return a new ip adress or an empty byte[] if no more ip adresses
	 *         are left.
	 */
	private synchronized byte[] getNextFreeIp(String mac) {
		synchronized (adresses) {
			if (adresses.containsKey(mac))
				return adresses.get(mac).getIp();
		}
		
		byte[] back = Application.getFirstIp().clone();
		String ip = Tools.byteToIp(back);
		
		while (!isIpFree(ip)) {
			back = iprange.nextIp(back);
			ip = Tools.byteToIp(back);
		}
		
		if (iprange.inRange(back))
			return back;
		else
			return new byte[] {
			    0, 0, 0, 0
			};
	}
	
	/**
	 * @return The next free ip adress which will be offered to new clients.
	 */
	public String getNextIP() {
		return Tools.byteToIp(getNextFreeIp(null));
	}
	
	/**
	 * @return a copy of our hashmap with the mac adresses and 
	 *         leases. Can be used to display the table in the console.
	 */
	public HashMap<String, Object> getTable() {
		HashMap<String, Object> back = new HashMap<String, Object>();
		
		synchronized (adresses) {
			for (String mac : adresses.keySet()) {
				back.put(mac, adresses.get(mac));
			}
		}
		
		return back;
	}
	
	/**
	 * Private method which handles an incoming dhcp request from a client.
	 * 
	 * @param m the message to interprete
	 */
	private void handleRequest(DHCPMessage m) {
		byte message_type = m.getOption(DHCPOptions.OPTION_DHCP_MESSAGE_TYPE)[0];
		String mac = Tools.byteToMac(m.getChaddr());
		String hostname = null;
		// udhcpc doesn't send this option -> Nullpointer exception
		if (m.getOption(DHCPOptions.OPTION_HOSTNAME) != null) {
			hostname = new String(m.getOption(DHCPOptions.OPTION_HOSTNAME));
		}
		String ident = mac;
		
		if(hostname != null && hostname.length() > 0)
			ident += " (" + hostname + ")";
		
		switch (message_type) {
			case DHCPMessage.DHCPDISCOVER:
				message("DHCPDISCOVER from " + ident);
				sendDiscover(m);
				break;
			case DHCPMessage.DHCPREQUEST:
				message("DHCPREQUEST from " + ident);
				answerRequest(m);
				break;
			case DHCPMessage.DHCPDECLINE:
				message("DHCPDECLINE from " + ident);
				adresses.put(String.valueOf(System.currentTimeMillis()), 
						new Lease(Tools.ipToByte(Tools.byteToIp(m.getCiaddr()))));
				removeLeaseIfNotManual(Tools.byteToMac(m.getChaddr()));
				break;
			case DHCPMessage.DHCPRELEASE:
				message("DHCPRELEASE from " + ident);
				removeLeaseIfNotManual(Tools.byteToMac(m.getChaddr()));
				break;
			case DHCPMessage.DHCPINFORM:
				sendAck(m, new byte[4], true);
				break;
			default:
				message("Received packet is an unhandled case. Message Type: "
				    + (int) message_type);
				break;
		}
	}
	
	private Lease removeLeaseIfNotManual(String mac) {
		synchronized (adresses) {
			Lease l = adresses.get(mac);
			if (l == null) {
				return null;
			}
			if (!l.isManual()) {
				adresses.remove(mac);
			}
			return l;
		}
	}
	
	/**
	 * Checks wheter the given ip adress is not in use. Does a lookup
	 * in our HashTable {@link #adresses}.
	 * 
	 * @param ip the ip adress to check
	 * @return is the given ip free?
	 */
	private boolean isIpFree(String ip) {
		if (ip == null || ip.equals("0.0.0.0"))
			return false;
		
		for (Lease akt : adresses.values()) {
			if (akt.getIpString().equals(ip) && akt.isValid()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Loads the leases from the given file.
	 * 
	 * @param f the file to lead the leases from
	 * @throws IOException If anything fails while reading the file.
	 */
	public void loadLeases(File f) throws IOException {
		lease_timer.pause(true);
		synchronized (adresses) {
			adresses.clear();
			
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;
			
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#") || line.trim().length() == 0)
					continue;
				
				String[] parts = line.trim().split(" |\t");
				
				adresses.put(parts[0], new Lease(parts[1], parts[2]));
			}
		}
		lease_timer.pause(false);
	}
	
	/**
	 * Helper method. Sends a message to the observer.
	 * @param m the message to send
	 */
	public void message(String m) {
		setChanged();
		notifyObservers(m);
	}
	
	/**
	 * Deletes all leases and removes the clients.
	 */
	public void removeAll() {
		synchronized (adresses) {
			adresses.clear();
		}
	}
	
	/**
	 * Removes a client from the HashTable and marks the allocated ip adress
	 * as to be free.
	 * 
	 * @param c the ip oder mac adress to remove
	 */
	public void removeClient(String c) {
		synchronized (adresses) {
			if (c.contains(":")) { // MAC
				Lease lease = adresses.get(c);
				
				if (lease != null) {
					adresses.remove(c);
					message("Client " + c + " (" + lease.getIpString() + ") removed.");
				}
			}
			else { // IP
				String toDelete = null;
				for (String mac : adresses.keySet()) {
					Lease lease = adresses.get(mac);
					
					if (lease.getIpString().equals(c)) {
						toDelete = mac;
					}
				}
				
				if (toDelete != null) {
					adresses.remove(toDelete);
					message("Client " + toDelete + " (" + c + ") removed.");
				}
			}
		}
	}
	
	@Override
	public void run() {
		message("Server started on " + Tools.byteToIp(Application.getServerIp()));
		
		try {
			//This is needed if the host has more than one active network card (Tested on Windows)
			socket = new DHCPSocket(DHCPMessage.SERVER_PORT,  Tools.byteToIp(Application.getServerIp()));
			//socket = new DHCPSocket(DHCPMessage.SERVER_PORT);
			adresses.clear();
		}
		catch (IOException e) {
			setChanged();
			notifyObservers(e);
			
			message("Server aborted on " + Tools.byteToIp(Application.getServerIp()));
			return;
		}
		
		lease_timer.start();
		while (doContinueWork()) {
			DHCPMessage anfrage = new DHCPMessage();
			
			if (socket.receive(anfrage))
				handleRequest(anfrage);
		}
		lease_timer.abort();
		
		socket.close();
		socket = null;
		adresses.clear();
		adresses = null;
		iprange = null;
		
		message("Server closed on " + Tools.byteToIp(Application.getServerIp()));
	}
	
	/**
	 * Saves the leases in the given file.
	 * 
	 * @param f the file to save the leases to.
	 * @throws IOException If anything fails while writing the file.
	 */
	public void saveLeases(File f) throws IOException {
		lease_timer.pause(true);
		synchronized (adresses) {
			PrintWriter pw = new PrintWriter(f);
			
			pw.println("# Saved leases");
			pw.println();
			pw.println("# Format:");
			pw.println("# MAC     IP     leased_timestamp");
			for (String mac : adresses.keySet()) {
				Lease l = adresses.get(mac);
				pw.println(mac + "\t" + l.getIpString() + "\t"
				        + l.getLeasedTimestamp());
			}
			
			pw.flush();
			pw.close();
		}
		lease_timer.pause(false);
	}
	
	/**
	 * Sends a DHCPACK message with the given ip adress. if unicast is not
	 * set, the options <code>LEASETIME</code> and <code>RENEWAL_TIME</code>
	 * are set in the answer.
	 * 
	 * @param m the message to answer
	 * @param ip the ip adress of the new client
	 * @param unicast should the message be sent via the given ip adress?
	 *                If <code>false</code> the message is sent via broadcast
	 *                to all clients.
	 */
	private void sendAck(DHCPMessage m, byte[] ip, boolean unicast) {
		DHCPMessage back = new DHCPMessage();
		
		back.setOp(DHCPMessage.OP_REPLY);
		back.setHtype(m.getHtype());
		back.setHlen(m.getHlen());
		back.setHops((byte) 0);
		back.setXid(m.getXid());
		back.setFlags(m.getFlags());
		back.setYiaddr(ip);
		back.setChaddr(m.getChaddr());
		back.setOption(DHCPOptions.OPTION_NETMASK, Application.getNetmask());
		if (!unicast) {
			back.setOption(DHCPOptions.OPTION_DHCP_IP_LEASE_TIME, Application.getLeaseTime());
			back.setOption(DHCPOptions.OPTION_DHCP_RENEWAL_TIME, Application.getRenewalTime());
		}
		back.setOption(DHCPOptions.OPTION_DHCP_MESSAGE_TYPE, new byte[] {
			DHCPMessage.DHCPACK
		});
		
		if(Application.getDNSServers().length > 4) {
			back.setOption(DHCPOptions.OPTION_DNS_SERVERS, Application.getDNSServers());
		}
		
		try {
			byte[] data = back.externalize();
			
			if (!unicast) {
				socket.send(new DatagramPacket(data, data.length,
				    DHCPMessage.BROADCAST_ADDR, DHCPMessage.CLIENT_PORT));
			}
			else {
				InetAddress unicast_addr = null;
				
				try {
					unicast_addr = InetAddress.getByAddress(ip);
				}
				catch (UnknownHostException e) {
					setChanged();
					notifyObservers(e);
				}
				
				socket.send(new DatagramPacket(data, data.length, 
						unicast_addr, DHCPMessage.CLIENT_PORT));
			}
		}
		catch (IOException e) {
			setChanged();
			notifyObservers(e);
			
			return;
		}
		
		message("DHCPACK to " + Tools.byteToMac(m.getChaddr()) + " for IP "
		    + Tools.byteToIp(ip));
	}
	
	/**
	 * Answers a DHCPDISCOVER message and offers a new ip adress to the client
	 * 
	 * @param m the message to answer.
	 */
	private void sendDiscover(DHCPMessage m) {
		DHCPMessage back = new DHCPMessage();
		
		back.setOp(DHCPMessage.OP_REPLY);
		back.setHtype(m.getHtype());
		back.setHlen(m.getHlen());
		back.setHops((byte) 0);
		back.setXid(m.getXid());
		back.setFlags(m.getFlags());
		back.setYiaddr(getNextFreeIp(Tools.byteToMac(m.getChaddr())));
		back.setChaddr(m.getChaddr());
		back.setOption(DHCPOptions.OPTION_NETMASK, Application.getNetmask());
		back.setOption(DHCPOptions.OPTION_DHCP_MESSAGE_TYPE, new byte[] {
			DHCPMessage.DHCPOFFER
		});
		//This is needed by e.g. udhcpc
		back.setOption(DHCPOptions.OPTION_DHCP_SERVER_IDENTIFIER, Application.server_ip);
		
		if(Application.getDNSServers().length > 4) {
			back.setOption(DHCPOptions.OPTION_DNS_SERVERS, Application.getDNSServers());
		}
		
		if (back.getYiaddr()[0] == (byte) 0) {
			message("No more IPs left to serve client "
			    + Tools.byteToMac(m.getChaddr()));
			return;
		}
		
		try {
			byte[] data = back.externalize();
			socket.send(new DatagramPacket(data, data.length,
			    DHCPMessage.BROADCAST_ADDR, DHCPMessage.CLIENT_PORT));
		}
		catch (IOException e) {
			setChanged();
			notifyObservers(e);
			
			return;
		}
		
		synchronized (adresses) {
			adresses.put(Tools.byteToMac(m.getChaddr()), new Lease(back.getYiaddr()));
		}
		
		message("DHCPOFFER to " + Tools.byteToMac(m.getChaddr()) + " for IP "
		    + Tools.byteToIp(back.getYiaddr()));
		
		return;
	}
	
	/**
	 * Sends a DHCHNACK message to the given client.
	 * 
	 * @param m the message to answer
	 * @param ip the ip adress to send the message to
	 */
	private void sendNAck(DHCPMessage m, byte[] ip) {
		DHCPMessage back = new DHCPMessage();
		
		back.setOp(DHCPMessage.OP_REPLY);
		back.setHtype(m.getHtype());
		back.setHlen(m.getHlen());
		back.setHops((byte) 0);
		back.setXid(m.getXid());
		back.setFlags(m.getFlags());
		back.setYiaddr(ip);
		back.setChaddr(m.getChaddr());
		back.setOption(DHCPOptions.OPTION_DHCP_MESSAGE_TYPE, new byte[] {
			DHCPMessage.DHCPNAK
		});
		
		try {
			byte[] data = back.externalize();
			socket.send(new DatagramPacket(data, data.length,
			    DHCPMessage.BROADCAST_ADDR, DHCPMessage.CLIENT_PORT));
		}
		catch (IOException e) {
			setChanged();
			notifyObservers(e);
			
			return;
		}
		
		message("DHCPNACK to " + Tools.byteToMac(m.getChaddr()));
	}
}
