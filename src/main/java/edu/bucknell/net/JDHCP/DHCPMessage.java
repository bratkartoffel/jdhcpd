package edu.bucknell.net.JDHCP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class represents a DHCP Message.
 * 
 * @author Jason Goldschmidt, Nick Stone and Simon Frankenberger
 */
public class DHCPMessage {
	// -----------------------------------------------------------
	//                         Constants
	// -----------------------------------------------------------
	/**
	 * Operation for a request
	 */
	public static final byte OP_REQUEST = 1;
	
	/**
	 * Operation for a reply
	 */
	public static final byte OP_REPLY = 2;
	
	/**
	 * Message Code representing a DHCPDISCOVER message
	 */
	public static final byte DHCPDISCOVER = 1;
	
	/**
	 * Message Code representing a DHCPOFFER message
	 */
	public static final byte DHCPOFFER = 2;
	
	/**
	 * Message Code representing a DHCPREQUEST message
	 */
	public static final byte DHCPREQUEST = 3;
	
	/**
	 * Message Code representing a DHCPDECLINE message
	 */
	public static final byte DHCPDECLINE = 4;
	
	/**
	 * Message Code representing a DHCPACK message
	 */
	public static final byte DHCPACK = 5;
	
	/**
	 * Message Code representing a DHCPNAK message
	 */
	public static final byte DHCPNAK = 6;
	
	/**
	 * Message Code representing a DHCPRELEASE message
	 */
	public static final byte DHCPRELEASE = 7;
	
	/**
	 * Message Code representing a DHCPINFORM message
	 */
	public static final byte DHCPINFORM = 8;

	/**
	 * Default DHCP client port
	 */
	public static final int CLIENT_PORT = 68; // client port (by default)
	
	/**
	 * Default DHCP server port
	 */
	public static final int SERVER_PORT = 67; // server port (by default)
	
	/**
	 * Broadcast Adress to send packets to
	 */
	public static InetAddress BROADCAST_ADDR = null;
	

	// -----------------------------------------------------------
	//               Fields defining a dhcp message
	// -----------------------------------------------------------
	/**
	 * Operation Code.<br>
	 * <br>
	 * Can either be {@link #OP_REQUEST} or {@link #OP_REPLY}.
	 */
	private byte op;
	
	/**
	 * Networktype as defined by 
	 * <a href="http://tools.ietf.org/html/rfc1340#page-54">RFC1340 page 54</a>.
	 */
	private byte htype;
	
	/**
	 * Hardware address length (e.g.  '6' for ethernet).
	 */
	private byte hlen;
	
	/**
	 * Client sets to zero, optionally used by relay-agents
	 * when booting via a relay-agent.
	 */
	private byte hops;
	
	/**
	 * Transaction ID, a random number chosen by the
	 * client, used by the client and server to associate
	 * messages and responses between a client and a
	 * server.
	*/
	private int xid;
	
	/**
	 *  Filled in by client, seconds elapsed since client
	 *  started trying to boot.
	 */
	private short secs;
	
	/**
	 * Flags for this message.<br>
	 * The leftmost bit is defined as the BROADCAST (B) flag.
	 */
	private short flags;
	
	/**
	 * Client IP address; filled in by client in
	 * DHCPREQUEST if verifying previously allocated
	 * configuration parameters.
	 */
	private byte ciaddr[] = new byte[4];
	
	/**
	 * 'your' (client) IP address.
	 */
	private byte yiaddr[] = new byte[4];
	
	/**
	 * IP address of next server to use in bootstrap;
	 * returned in DHCPOFFER, DHCPACK and DHCPNAK by
	 * server.
	 */
	private byte siaddr[] = new byte[4];
	
	/**
	 * Relay agent IP address, used in booting via a
	 * relay-agent.
	 */
	private byte giaddr[] = new byte[4];
	
	/**
	 * Client hardware address.
	 */
	private byte chaddr[] = new byte[16];
	
	/**
	 * Optional server host name, null terminated string.
	 */
	private byte sname[] = new byte[64];
	
	/**
	 * Boot file name, null terminated string; "generic"
	 * name or null in DHCPDISCOVER, fully qualified
	 * directory-path name in DHCPOFFER.
	 */
	private byte file[] = new byte[128];
	
	/**
	 * Internal representation of the given DHCP options.
	 */
	private DHCPOptions optionsList = null;
	
	/**
	 * global port variable for this message
	 */
	private int gPort;
	
	/**
	 * The destination IP-Adress of this message
	 */
	private InetAddress destination_IP;
	
	static {
		try {
			BROADCAST_ADDR = InetAddress.getByName("255.255.255.255");
			// broadcast address(by default)
		}
		catch (UnknownHostException e) {
		}
	}

	// -----------------------------------------------------------
	//                        Constructors
	// -----------------------------------------------------------
	
	/** 
	 * Creates empty DHCPMessage object,
	 * initializes the object, sets the host to the broadcast address,
	 * the local subnet, binds to the default server port.
	 */
	public DHCPMessage() {
		initialize();
		
		destination_IP = BROADCAST_ADDR;
		gPort = SERVER_PORT;
	}
	
	/** 
	 * Copy constructor 
	 * creates DHCPMessage from inMessage
	 * 
	 * @param inMessage The message to be copied
	 */
	public DHCPMessage(DHCPMessage inMessage) {
		initialize();
		
		destination_IP = BROADCAST_ADDR;
		gPort = SERVER_PORT;
		op = inMessage.getOp();
		htype = inMessage.getHtype();
		hlen = inMessage.getHlen();
		hops = inMessage.getHops();
		xid = inMessage.getXid();
		secs = inMessage.getSecs();
		flags = inMessage.getFlags();
		ciaddr = inMessage.getCiaddr();
		yiaddr = inMessage.getYiaddr();
		siaddr = inMessage.getSiaddr();
		giaddr = inMessage.getGiaddr();
		chaddr = inMessage.getChaddr();
		sname = inMessage.getSname();
		file = inMessage.getFile();
		optionsList.internalize(inMessage.getOptions());
	}
	
	/** 
	 * Copy constructor
	 * creates DHCPMessage from inMessage and sets server and port.
	 * 
	 * @param inMessage The message to be copied
	 * @param inServername The host name
	 * @param inPort The port number
	 */
	public DHCPMessage(DHCPMessage inMessage, InetAddress inServername, int inPort) {
		initialize();
		
		this.destination_IP = inServername;
		this.gPort = inPort;
		
		op = inMessage.getOp();
		htype = inMessage.getHtype();
		hlen = inMessage.getHlen();
		hops = inMessage.getHops();
		xid = inMessage.getXid();
		secs = inMessage.getSecs();
		flags = inMessage.getFlags();
		ciaddr = inMessage.getCiaddr();
		yiaddr = inMessage.getYiaddr();
		siaddr = inMessage.getSiaddr();
		giaddr = inMessage.getGiaddr();
		chaddr = inMessage.getChaddr();
		sname = inMessage.getSname();
		file = inMessage.getFile();
		optionsList.internalize(inMessage.getOptions());
	}

	/** 
	 * Copy constructor
	 * creates DHCPMessage from inMessage and sets server name.
	 * 
	 * @param inMessage The message to be copied
	 * @param inServername The host name
	 */
	public DHCPMessage(DHCPMessage inMessage, InetAddress inServername) {
		initialize();
		
		this.destination_IP = inServername;
		this.gPort = SERVER_PORT;
		
		op = inMessage.getOp();
		htype = inMessage.getHtype();
		hlen = inMessage.getHlen();
		hops = inMessage.getHops();
		xid = inMessage.getXid();
		secs = inMessage.getSecs();
		flags = inMessage.getFlags();
		ciaddr = inMessage.getCiaddr();
		yiaddr = inMessage.getYiaddr();
		siaddr = inMessage.getSiaddr();
		giaddr = inMessage.getGiaddr();
		chaddr = inMessage.getChaddr();
		sname = inMessage.getSname();
		file = inMessage.getFile();
		optionsList.internalize(inMessage.getOptions());
	}
	
	/** 
	 * Creates an empty DHCPMessage object,
	 * initializes the object, sets the host to a specified host name,
	 * and binds to a specified port.
	 * 
	 * @param inServername The host name
	 * @param inPort The port number
	 */
	public DHCPMessage(InetAddress inServername, int inPort) {
		initialize();
		
		destination_IP = inServername;
		gPort = inPort;
	}
	
	/**
	 * Creates an empty DHCPMessage object,
	 * initializes the object, sets the host to a specified host name,
	 * and binds to the default port.
	 * 
	 * @param inServername The host name
	 */
	public DHCPMessage(InetAddress inServername) {
		initialize();
		
		destination_IP = inServername;
		gPort = SERVER_PORT;
	}
	
	/**
	 * Creates an empty DHCPMessage object,
	 * initializes the object, sets the host to the broadcast address,
	 * and binds to a specified port.
	 * 
	 * @param inPort The port number
	 */
	public DHCPMessage(int inPort) {
		initialize();
		
		destination_IP = BROADCAST_ADDR;
		gPort = inPort;
	}
	
	/**
	 * Creates an empty DHCPMessage object,
	 * initializes the object with a specified byte array containing
	 * DHCP message information, sets the host to default host name, the
	 * local subnet, and bind to the default server port.
	 * 
	 * @param ibuf The byte array to initialize DHCPMessage object
	 */
	public DHCPMessage(byte[] ibuf) {
		initialize();
		internalize(ibuf);
		
		destination_IP = BROADCAST_ADDR;
		gPort = SERVER_PORT;
	}
	
	/**
	 * Creates an empty DHCPMessage object,
	 * initializes the object with a specified byte array containing
	 * DHCP message information, sets the host to specified host name,
	 * and binds to the specified port.
	 * 
	 * @param ibuf The byte array to initialize DHCPMessage object
	 * @param inServername The hostname
	 * @param inPort The port number
	 */
	public DHCPMessage(byte[] ibuf, InetAddress inServername, int inPort) {
		initialize();
		internalize(ibuf);
		
		destination_IP = inServername;
		gPort = inPort;
	}
	
	/**
	 * Creates an empty DHCPMessage object,
	 * initializes the object with a specified byte array containing
	 * DHCP message information, sets the host to broadcast address,
	 * and binds to the specified port.
	 * 
	 * @param ibuf The byte array to initialize DHCPMessage object
	 * @param inPort The port number
	 */
	public DHCPMessage(byte ibuf[], int inPort) {
		initialize();
		internalize(ibuf);
		
		destination_IP = BROADCAST_ADDR;
		gPort = inPort;
	}
	
	/** 
	 * Creates an empty DHCPMessage object,
	 * initializes the object with a specified byte array containing
	 * DHCP message information, sets the host to specified host name,
	 * and binds to the specified port.
	 * 
	 * @param ibuf The byte array to initialize DHCPMessage object
	 * @param inServername The hostname
	 */
	public DHCPMessage(byte[] ibuf, InetAddress inServername) {
		initialize();
		internalize(ibuf);
		
		destination_IP = inServername;
		gPort = SERVER_PORT;
	}
	
	/**
	 * Creates a new DHCPMessage object from the giben DataInputStream.
	 * 
	 * @param inStream The stream to read from
	 */
	public DHCPMessage(DataInputStream inStream) {
		initialize();
		
		try {
			op = inStream.readByte();
			htype = inStream.readByte();
			hlen = inStream.readByte();
			hops = inStream.readByte();
			xid = inStream.readInt();
			secs = inStream.readShort();
			flags = inStream.readShort();
			inStream.readFully(ciaddr, 0, 4);
			inStream.readFully(yiaddr, 0, 4);
			inStream.readFully(siaddr, 0, 4);
			inStream.readFully(giaddr, 0, 4);
			inStream.readFully(chaddr, 0, 16);
			inStream.readFully(sname, 0, 64);
			inStream.readFully(file, 0, 128);
			byte[] options = new byte[312];
			inStream.readFully(options, 0, 312);
			optionsList.internalize(options);
		}
		catch (IOException e) {
			System.err.println(e);
		}
	}

	// -----------------------------------------------------------
	//                            Methods
	// -----------------------------------------------------------
	/**
	 * Initializes datamembers in the constructors
	 * every empty DHCPMessage object will by default contain these params.
	 * Initializes options array from linked list form.
	 */
	private void initialize() {
		optionsList = new DHCPOptions();
	}
	
	/**
	 * Converts a DHCPMessage object to a byte array.
	 * 
	 * @return A byte array with information from DHCPMessage object, 
	 *         ready to send.
	 */
	public synchronized byte[] externalize() {
		ByteArrayOutputStream outBStream = new ByteArrayOutputStream();
		DataOutputStream outStream = new DataOutputStream(outBStream);
		
		try {
			outStream.writeByte(op);
			outStream.writeByte(htype);
			outStream.writeByte(hlen);
			outStream.writeByte(hops);
			outStream.writeInt(xid);
			outStream.writeShort(secs);
			outStream.writeShort(flags);
			outStream.write(ciaddr, 0, 4);
			outStream.write(yiaddr, 0, 4);
			outStream.write(siaddr, 0, 4);
			outStream.write(giaddr, 0, 4);
			outStream.write(chaddr, 0, 16);
			outStream.write(sname, 0, 64);
			outStream.write(file, 0, 128);
			
			byte[] options = new byte[312];
			if (optionsList == null) {
				initialize();
			}
			
			options = optionsList.externalize();
			outStream.write(options, 0, 312);
		} catch (IOException e) {
			System.err.println(e);
		}
		
		// extract the byte array from the Stream
		byte data[] = outBStream.toByteArray();
		
		return data;
	}
	
	/**
	 * Convert a specified byte array containing a DHCP message into a
	 * DHCPMessage object.
	 * 
	 * @param ibuff Byte array to convert to a DHCPMessage object
	 * @return A DHCPMessage object with information from byte array.
	 */
	
	public synchronized DHCPMessage internalize(byte[] ibuff) {
		ByteArrayInputStream inBStream = 
			new ByteArrayInputStream(ibuff, 0, ibuff.length);
		DataInputStream inStream = new DataInputStream(inBStream);
		
		try {
			op = inStream.readByte();
			htype = inStream.readByte();
			hlen = inStream.readByte();
			hops = inStream.readByte();
			xid = inStream.readInt();
			secs = inStream.readShort();
			flags = inStream.readShort();
			inStream.readFully(ciaddr, 0, 4);
			inStream.readFully(yiaddr, 0, 4);
			inStream.readFully(siaddr, 0, 4);
			inStream.readFully(giaddr, 0, 4);
			inStream.readFully(chaddr, 0, 16);
			inStream.readFully(sname, 0, 64);
			inStream.readFully(file, 0, 128);
			
			byte[] options = new byte[312];
			inStream.readFully(options, 0, 312);
			if (optionsList == null) {
				initialize();
			}
			
			optionsList.internalize(options);
		}
		catch (IOException e) {
			System.err.println(e);
		} // end catch
		
		return this;
	}
	
	/**
	 * Set message Op code / message type.
	 * 
	 * @param inOp message Op code / message type
	 */
	public void setOp(byte inOp) {
		op = inOp;
	}
	
	/**
	 * Set hardware address type.
	 * 
	 * @param inHtype hardware address type
	 */
	public void setHtype(byte inHtype) {
		htype = inHtype;
	}
	
	/**
	 * Set hardware address length.
	 * 
	 * @param inHlen  hardware address length
	 */
	public void setHlen(byte inHlen) {
		hlen = inHlen;
	}
	
	/**
	 * Set hops field.
	 * 
	 * @param inHops hops field
	 */
	public void setHops(byte inHops) {
		hops = inHops;
	}
	
	/**
	 * Set transaction ID.
	 * 
	 * @param inXid  transactionID
	 */
	public void setXid(int inXid) {
		xid = inXid;
	}
	
	/**
	 * Set seconds elapsed since client began address acquisition or
	 * renewal process.
	 * 
	 * @param inSecs Seconds elapsed since client began address acquisition
	 *               or renewal process
	 */
	public void setSecs(short inSecs) {
		secs = inSecs;
	}
	
	/**
	 * Set flags field.
	 * 
	 * @param inFlags flags field
	 */
	public void setFlags(short inFlags) {
		flags = inFlags;
	}
	
	/**
	 * Set client IP address.
	 * 
	 * @param inCiaddr client IP address
	 */
	public void setCiaddr(byte[] inCiaddr) {
		ciaddr = inCiaddr;
	}
	
	/**
	 * Set 'your' (client) IP address.
	 * 
	 * @param inYiaddr 'your' (client) IP address
	 */
	public void setYiaddr(byte[] inYiaddr) {
		yiaddr = inYiaddr;
	}
	
	/**
	 * Set address of next server to use in bootstrap.
	 * 
	 * @param inSiaddr address of next server to use in bootstrap
	 */
	public void setSiaddr(byte[] inSiaddr) {
		siaddr = inSiaddr;
	}
	
	/**
	 * Set relay agent IP address.
	 * 
	 * @param inGiaddr relay agent IP address
	 */
	public void setGiaddr(byte[] inGiaddr) {
		giaddr = inGiaddr;
	}
	
	/**
	 * Set client harware address.
	 * 
	 * @param inChaddr client hardware address
	 */
	public void setChaddr(byte[] inChaddr) {
		chaddr = inChaddr;
	}
	
	/**
	 * Set optional server host name.
	 * 
	 * @param inSname server host name
	 */
	public void setSname(byte[] inSname) {
		sname = inSname;
	}
	
	/**
	 * Set boot file name.
	 * 
	 * @param inFile boot file name
	 */
	public void setFile(byte[] inFile) {
		file = inFile;
	}
	
	/**
	 * Set message destination port.
	 * 
	 * @param inPortNum port on message destination host
	 */
	public void setPort(int inPortNum) {
		gPort = inPortNum;
	}
	
	/**
	 * Set message destination IP
	 * @param inHost string representation of message destination IP or 
	 *               hostname
	 */
	public void setDestinationHost(String inHost) {
		try {
			destination_IP = InetAddress.getByName(inHost);
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}
	
	/**
	 * @return message Op code / message type.
	 */
	public byte getOp() {
		return op;
	}
	
	/**
	 * @return hardware address type.
	 */
	public byte getHtype() {
		return htype;
	}
	
	/**
	 * @return hardware address length.
	 */
	public byte getHlen() {
		return hlen;
	}
	
	/**
	 * @return hops field.
	 */
	public byte getHops() {
		return hops;
	}
	
	/** 
	 * @return transaction ID.
	 */
	public int getXid() {
		return xid;
	}
	
	/**
	 * @return seconds elapsed since client began address 
	 * acquisition or renewal process.
	 */
	public short getSecs() {
		return secs;
	}
	
	/**
	 * @return flags field.
	 */
	public short getFlags() {
		return flags;
	}
	
	/** 
	 * @return client IP address.
	 */
	public byte[] getCiaddr() {
		return ciaddr;
	}
	
	/** 
	 * @return 'your' (client) IP address.
	 */
	public byte[] getYiaddr() {
		return yiaddr;
	}
	
	/**
	 * @return address of next server to use in bootstrap.
	 */
	public byte[] getSiaddr() {
		return siaddr;
	}
	
	/** 
	 * @return relay agent IP address.
	 */
	public byte[] getGiaddr() {
		return giaddr;
	}
	
	/** 
	 * @return client harware address.
	 */
	public byte[] getChaddr() {
		return chaddr;
	}
	
	/** 
	 * @return optional server host name.
	 */
	public byte[] getSname() {
		return sname;
	}
	
	/**
	 * @return boot file name.
	 */
	public byte[] getFile() {
		return file;
	}
	
	/**
	 * @return a byte array containing options 
	 */
	public byte[] getOptions() {
		if (optionsList == null) {
			initialize();
		}
		return optionsList.externalize();
	}
	
	/**
	 * @return An interger representation of the message 
	 *         destination port 
	 */
	public int getPort() {
		return gPort;
	}
	
	/**
	 * Get message destination hostname
	 * 
	 * @return A string representing the hostname of the 
	 *         message destination server 
	 */
	public String getDestinationAddress() {
		return destination_IP.getHostAddress();
	}
	
	/**
	 * Sets DHCP options in DHCPMessage. If option already exists 
	 * then remove old option and insert a new one.
	 * 
	 * @param inOptNum option number
	 * @param inOptionData option data
	 */
	public void setOption(int inOptNum, byte[] inOptionData) {
		optionsList.setOption((byte) inOptNum, inOptionData);
	}
	
	/** 
	 * Returns specified DHCP option that matches the input code. Null is
	 * returned if option is not set.
	 * 
	 * @param inOptNum option number
	 * 
	 * @return the option matching input code
	 */
	public byte[] getOption(int inOptNum) {
		if (optionsList == null) {
			initialize();
		}
		return optionsList.getOption((byte) inOptNum);
	}
	
	/** 
	 * Removes the specified DHCP option that matches the input code. 
	 * 
	 * @param inOptNum option number
	 */
	public void removeOption(int inOptNum) {
		if (optionsList == null) {
			initialize();
		}
		optionsList.removeOption((byte) inOptNum);
	}
	
	/** 
	 * Report whether or not the input option is set.
	 * 
	 * @param inOptNum  option number
	 * 
	 * @return is the given option set?
	 */
	public boolean IsOptSet(int inOptNum) {
		if (optionsList == null) {
			initialize();
		}
		
		return optionsList.contains((byte) inOptNum);
	}
}
