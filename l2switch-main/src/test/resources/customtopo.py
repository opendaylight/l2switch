# !/usr/bin/python

# usage: sudo mn --controller=remote,ip=<controller_ip> --switch=ovsk,protocols=OpenFlow13 --custom <path to customtopo.py> --topo ring ...

from mininet.topo import Topo


def add_hosts_to_switch(self, switch, hosts, start_host_suffix):
    host_suffix = start_host_suffix
    for _ in range(hosts):
        host = self.addHost("h%s" % host_suffix)
        self.addLink(switch, host)
        host_suffix += 1


class RingTopo(Topo):
    def __init__(self, switches=3, hosts_per_switch=1, **opts):
        Topo.__init__(self, **opts)
        host_suffix = 1
        switch = self.addSwitch('s%s' % 1)
        first_switch = switch
        for i in range(1, switches):
            # add hosts to switch
            add_hosts_to_switch(self, switch, hosts_per_switch, host_suffix)
            host_suffix += hosts_per_switch

            new_switch = self.addSwitch('s%s' % (i + 1))
            self.addLink(new_switch, switch)
            switch = new_switch

        add_hosts_to_switch(self, switch, hosts_per_switch, host_suffix)
        self.addLink(switch, first_switch)


class MeshTopo(Topo):
    def __init__(self, switches=3, hosts_per_switch=1, **opts):
        Topo.__init__(self, **opts)
        created_switches = []
        host_suffix = 1
        for i in range(switches):
            new_switch = self.addSwitch('s%s' % (i + 1))

            # add hosts to new switch
            add_hosts_to_switch(self, new_switch, hosts_per_switch, host_suffix)
            host_suffix += hosts_per_switch

            for switch in created_switches:
                self.addLink(new_switch, switch)

            created_switches.append(new_switch)


topos = {'ring': RingTopo,
         'mesh': MeshTopo}
