
package com.stratio.paas.sparkAT.specs;

import com.stratio.qa.specs.CommonG;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public class Common extends CommonG {
    private ServerSocketChannel serverSocket;
    private Socket socket;

    public ServerSocketChannel getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocketChannel serverSocket) {
        this.serverSocket = serverSocket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
