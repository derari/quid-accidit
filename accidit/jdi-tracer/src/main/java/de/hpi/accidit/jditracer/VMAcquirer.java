package de.hpi.accidit.jditracer;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;

public class VMAcquirer {

    /**
     * Call this with the localhost port to connect to.
     */
    @SuppressWarnings("SleepWhileInLoop")
    public VirtualMachine connect(int port, int retry, int time) {
        String strPort = Integer.toString(port);
        AttachingConnector connector = getConnector();
        try {
            for (int i = 0; i < retry; i++) {
                try {
                    return connect(connector, strPort);
                } catch (ConnectException e) { }
                Thread.sleep(time);
            }            
            return connect(connector, strPort);
        } catch (IllegalConnectorArgumentsException | InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private AttachingConnector getConnector() {
        VirtualMachineManager vmManager = Bootstrap
                .virtualMachineManager();
        for (Connector connector : vmManager
                .attachingConnectors()) {
            System.out.println(connector.name());
            if ("com.sun.jdi.SocketAttach".equals(connector
                    .name())) {
                return (AttachingConnector) connector;
            }
        }
        throw new IllegalStateException();
    }

    private VirtualMachine connect(
            AttachingConnector connector, String port)
            throws IllegalConnectorArgumentsException,
            IOException {
        Map<String, Connector.Argument> args = connector
                .defaultArguments();
        Connector.Argument pidArgument = args.get("port");
        if (pidArgument == null) {
            throw new IllegalStateException();
        }
        pidArgument.setValue(port);
        
        pidArgument = args.get("hostname");
        if (pidArgument == null) {
            throw new IllegalStateException();
        }
        pidArgument.setValue("localhost");

        return connector.attach(args);
    }
}
