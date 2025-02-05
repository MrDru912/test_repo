package cz.ctu.fee.dsv.grpc;

import cz.ctu.fee.dsv.CommandsGrpc;
import cz.ctu.fee.dsv.grpc.base.Address;
import cz.ctu.fee.dsv.grpc.base.DSNeighbours;
import cz.ctu.fee.dsv.grpc.base.NodeCommands;
import cz.ctu.fee.dsv.grpc.exceptions.KilledNodeActsAsClientException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GrpcCommunicationHub {
    private DSNeighbours actNeighbours = null;
    private Address myAddress = null;
    private MessageReceiver myMessageReceiver = null;
    private Node node = null;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    private boolean isAlive = true;

    public GrpcCommunicationHub (Node node) {
        this.node = node;
        this.myAddress = node.getAddress();
        this.actNeighbours = node.getNeighbours();
        this.myMessageReceiver = node.getMessageReceiver();
    }


    public CommandsGrpc.CommandsBlockingStub getNext() {
        return getGrpcProxy(actNeighbours.next);
    }


    public CommandsGrpc.CommandsBlockingStub getNNext() {
        return getGrpcProxy(actNeighbours.nnext);
    }


    public CommandsGrpc.CommandsBlockingStub getPrev() {
        return getGrpcProxy(actNeighbours.prev);
    }

//    public NodeCommands getLeader() throws RemoteException {
//        return getRMIProxy(actNeighbours.leader);
//    }

    public CommandsGrpc.CommandsBlockingStub getGrpcProxy(Address address) {
//        if (address.compareTo(myAddress) == 0 ) return myMessageReceiver;
//        else {
        String key = address.hostname + ":" + address.port;
        try {
                ManagedChannel channel = channels.computeIfAbsent(key, k ->
                        ManagedChannelBuilder.forAddress(address.hostname, address.port)
                                .usePlaintext()
                                .build()
                );
                CommandsGrpc.CommandsBlockingStub blockingStub = CommandsGrpc.newBlockingStub(channel);
                return blockingStub;
            } catch (Exception e) {
                // transitive RM exception
                e.printStackTrace();
                throw e;
            }
//        }
    }


    public void setActNeighbours(DSNeighbours actNeighbours) {
        this.actNeighbours = actNeighbours;
    }

    public void shutdown() {
        for (ManagedChannel channel : channels.values()) {
            try {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        channels.clear();
    }

}