package org.gridkit.jvmtool.bstub;

import java.nio.channels.SocketChannel;

import net.java.btrace.api.extensions.ExtensionsRepository;
import net.java.btrace.api.wireio.Channel;
import net.java.btrace.client.ClientChannel;

public class BTWarmUp {

    public static void prefetchClasses() {
        try {
            Class.forName(ExtensionsRepository.class.getName());
            Class.forName(SocketChannel.class.getName());
            Class.forName(ClientChannel.class.getName());
            Class.forName(Channel.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
}
