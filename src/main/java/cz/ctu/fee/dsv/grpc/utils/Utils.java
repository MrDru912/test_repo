package cz.ctu.fee.dsv.grpc.utils;

import cz.ctu.fee.dsv.AddressProto;
import cz.ctu.fee.dsv.grpc.Node;

public class Utils {
    public static String getProcessId(String hostname, int port){
        return hostname + ":" + port;
    }

    public static String getProcessId(AddressProto addressProto){
        return addressProto.getHostname() + ":" + addressProto.getPort();
    }

    // lamport update
    public static void updateTimeOnReceive(int time, Node node) {
        node.setTime(Math.max(time, node.getTime()) + 1);
    }

    public static String formatTime(int time){
        return("|time: " + time + "|");
    }

}
