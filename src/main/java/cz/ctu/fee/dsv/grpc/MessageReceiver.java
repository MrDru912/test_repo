package cz.ctu.fee.dsv.grpc;

import com.google.protobuf.Empty;
import cz.ctu.fee.dsv.*;
import cz.ctu.fee.dsv.grpc.base.NodeCommands;
import cz.ctu.fee.dsv.grpc.base.NodeCommandsImpl;
import cz.ctu.fee.dsv.grpc.exceptions.NodeAlreadyJointException;
import cz.ctu.fee.dsv.grpc.mappers.ProtobufMapper;
import cz.ctu.fee.dsv.grpc.utils.Utils;
import io.grpc.stub.StreamObserver;

public class MessageReceiver extends CommandsGrpc.CommandsImplBase {
    private Node myNode = null;

    public NodeCommands getNodeCommands() {
        return nodeCommands;
    }

    private NodeCommands nodeCommands;

    public MessageReceiver(Node node) {
        this.myNode = node;
        nodeCommands = new NodeCommandsImpl(this.myNode);
    }

    @Override
    public void join(AddressProto protoAddr,
                             io.grpc.stub.StreamObserver<cz.ctu.fee.dsv.DSNeighboursProto> responseObserver) {
        Utils.updateTimeOnReceive(protoAddr.getTime(), this.myNode);
        DSNeighboursProto reply = nodeCommands.join(protoAddr);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
        }

    @Override
    public void chngNNextOfPrev(AddressProto addrProto,
                          StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(addrProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.chngNNextOfPrev(addrProto);
        // Send an empty response
        responseObserver.onNext(timeProto);
        // Signal that the call is complete
        responseObserver.onCompleted();
    }

    @Override
    public void chngNNext(AddressProto addrProto,
                          StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(addrProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.chngNNext(addrProto);
        // Send an empty response
        responseObserver.onNext(timeProto);
        // Signal that the call is complete
        responseObserver.onCompleted();
    }

    @Override
    public void chngNext(AddressProto addrProto,
                          StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(addrProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.chngNext(addrProto);
        // Send an empty response
        responseObserver.onNext(timeProto);
        // Signal that the call is complete
        responseObserver.onCompleted();
    }


    @Override
    public void chngPrev(AddressProto addrProto,
            io.grpc.stub.StreamObserver<cz.ctu.fee.dsv.AddressProto> responseObserver) {
        Utils.updateTimeOnReceive(addrProto.getTime(), this.myNode);
        AddressProto reply = nodeCommands.chngPrev(addrProto);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
//        return myNode.getNeighbours().next;
    }


    @Override
    public void nodeMissing(AddressProto addrProto, StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(addrProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.nodeMissing(addrProto);
        // Send an empty response
        responseObserver.onNext(timeProto);
        // Signal that the call is complete
        responseObserver.onCompleted();
    }


//
//    @Override
//    public void SendMsg(String toNickName, String fromNickName, String message) {
//
//    }


    @Override
    public void hello(Empty request, StreamObserver<TimeProto> responseObserver) {
//        this.updateTimeOnReceive(addrProto.getTime()); #TODO add time message

//        // Perform any logic you need (e.g., logging, updating state)
        TimeProto timeProto = nodeCommands.hello();
        // Send an empty response
        responseObserver.onNext(timeProto);
        // Signal that the call is complete
        responseObserver.onCompleted();
    }

    @Override
    public void preliminaryRequest(RequestResourceMessageProto requestMessageProto, StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(requestMessageProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.preliminaryRequest(requestMessageProto);
        responseObserver.onNext(timeProto);
        responseObserver.onCompleted();
    }

    @Override
    public void requestResource(RequestResourceMessageProto requestMessageProto, StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(requestMessageProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.requestResource(requestMessageProto);
        responseObserver.onNext(timeProto);
        responseObserver.onCompleted();
    }

    @Override
    public void resourceWasReleased(ResourceProto resourceProto, StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(resourceProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.resourceWasReleased(resourceProto);
        responseObserver.onNext(timeProto);
        responseObserver.onCompleted();
    }

    @Override
    public void acquireResource(AcquireMessageProto acquireMessageProto, StreamObserver<TimeProto> responseObserver) {
        Utils.updateTimeOnReceive(acquireMessageProto.getTime(), this.myNode);
        TimeProto timeProto = nodeCommands.acquireResource(acquireMessageProto);
        responseObserver.onNext(timeProto);
        responseObserver.onCompleted();
    }

}
