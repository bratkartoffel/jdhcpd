This project provides a lightweight, simple and easy to use 
DHCP server written in 100% java code.

It can be configured either via the config.ini file or
live via console.
Currently it is just an simple tool to provide IP-Adresses
in a given range and a subnet.

This project was started as i realized that there was no free and
easy to use dhcp server for windows that i can use at my lan-partys.
A sample config.ini is present which distribute ip adresses from
192.168.0.20 to 192.168.0.200 in a class C net (255.255.255.0)

The project is hosted an sourceforge and can be found via the
following url: http://sourceforge.net/projects/jdhcpd/

You can contact me via the service provided by sourceforge.net

The config.ini is a simple textfile, which is used to configure 
the server. It contains only one section (the "global" section) 
and provides the following options:

renewal_time:
  When the client should try to renew the lease. Defined in seconds.
  default: 77760
lease_time:
  How long the lease is valid. Defined in seconds.
  default: 86400

server_ip:
  Defines which ip-adress the server got.
  default: 192.168.0.1
first_ip:
  The first ip to provide to the clients.
  default: 192.168.0.20
last_ip:
  The last ip to provide to the clients.
  default: 192.168.0.200
netmask:
  The netmask of this network. CIDR notation is NOT allowed.
  default: 255.255.255.0
dns_servers:
  A list of DNS-Servers responsible for this network. Each IP is
  separated by a space.
  default: 192.168.0.1

