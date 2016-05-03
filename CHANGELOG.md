Release history and changes of JDHCPD
===========================================

0.10 (2010-05-09)
  - Added an option to serve one or more dns server
  - Moved some parts from the Application class into the Server class
  - Changed some comments in the classes to remove some warnings
  - Removed the stupid version number from the classes
    (I always forget to increment them)
  - The hostname is now displayed if sent in an request
  - Updated the README file to explain all possible settings inside the
    config.ini file


0.9 (2010-04-28)
  - Completed the list of DHCP-Options
  - Included the important RFCs which define the protocol
  - Removed some redundancy in the server


0.8 (2010-04-21)
  - Changed the visility of some inner classes from Server
  - Moved some methods from Server to the Tools class
  - User interface parts separated to a new class
  - Some commands printed their messages only to the console
    and ignored the gui
  - Bugfix: A NullPointerException killed the server if a requested
            ip adress is out of the defined range
  - Bugfix: Bug mentioned in v0.7 finally fixed and tested
            

0.7 (2010-04-19)
	- Updated the comments
	- Sorted the source code
	- Improved the GUI
	- Bugfix: If an offered ip-adress is already in use, the client
	          responded with a DHCPNACK message, which was not handeled.
	          So the same (invalid) ip adress was offered as often as the
	          client asked for one.
	          Need further tests to confirm fix!
  - Replaced JDialog by a JFrame, now it is displayed in the taskbar


0.6 (2010-03-16)
  - Created the CHANGELOG file
	- Completed the javadoc comments in all classes
	- Replaced the german console help, so the complete project
	  is now written in english
	- Notified messages from the server are now written synchronized
    to the console
  - Removed an unused method from the Tools class
  - Changed socket timeout to 1 second so it responds faster
  - Implemented a very simple gui, currently only displays the log.
    Can be disabled via the parameter --nogui.


0.5 (2010-03-15)
	- Initial release on sourceforge.net
