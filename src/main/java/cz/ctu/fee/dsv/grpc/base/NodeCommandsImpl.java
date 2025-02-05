package cz.ctu.fee.dsv.grpc.base;

import cz.ctu.fee.dsv.*;
import cz.ctu.fee.dsv.grpc.Node;
import cz.ctu.fee.dsv.grpc.exceptions.NodeAlreadyJointException;
import cz.ctu.fee.dsv.grpc.mappers.ProtobufMapper;
import cz.ctu.fee.dsv.grpc.resources.Resource;
import cz.ctu.fee.dsv.grpc.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;

public class NodeCommandsImpl implements NodeCommands{
    final Logger logger = LoggerFactory.getLogger(NodeCommandsImpl.class);
    private Node myNode = null;

    public NodeCommandsImpl(Node node) {
        this.myNode = node;
    }

    @Override
    public DSNeighboursProto join(AddressProto protoAddr) {
        Address addr = ProtobufMapper.fromProtoToAddress(protoAddr);
        logger.info("{} JOIN was called ...", Utils.formatTime(this.myNode.getTime()));
        if (addr.compareTo(myNode.getAddress()) == 0) {
            logger.info("{} I am the first", Utils.formatTime(this.myNode.getTime()));
            return ProtobufMapper.DSNeighboursToProto(myNode.getNeighbours(), this.myNode.getLamportTime());
        } else {
            Address joiningNodeAddr = ProtobufMapper.fromProtoToAddress(protoAddr);
            logger.info("{} {} is joining ...", Utils.formatTime(this.myNode.getTime()), joiningNodeAddr);
            DSNeighbours myNeighbours = myNode.getNeighbours();
            Address myInitialNext = new Address(myNeighbours.next);     // because of 2 nodes config
            Address myInitialPrev = new Address(myNeighbours.prev);     // because of 2 nodes config
            DSNeighbours tmpNeighbours = new DSNeighbours(myNeighbours.next,
                    myNeighbours.nnext,
                    myNode.getAddress(),
                    myNeighbours.leader);

//            if (joiningNodeAddr.equals(myNode.getNeighbours().next)) {
//                logger.info("{} {} is already joint ...", Utils.formatTime(this.myNode.getTime()), joiningNodeAddr);
//                tmpNeighbours = new DSNeighbours(null,
//                        null,
//                        null,
//                        null);
//                return ProtobufMapper.DSNeighboursToProto(tmpNeighbours, this.myNode.getLamportTime());
//            }

            // to my (initial) next send msg ChPrev to addr
            AddressProto addressProto = myNode.getCommHub().getNext().chngPrev(protoAddr);
            Utils.updateTimeOnReceive(addressProto.getTime(), this.myNode);
            // to my (initial) prev send msg ChNNext addr
            TimeProto timeProto = myNode.getCommHub().getGrpcProxy(myInitialPrev).chngNNext(protoAddr);
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            tmpNeighbours.nnext = myNeighbours.nnext;
            // handle myself
            myNeighbours.nnext = myInitialNext;
            myNeighbours.next = addr;
            return ProtobufMapper.DSNeighboursToProto(tmpNeighbours, this.myNode.getLamportTime());
        }
    }


    @Override
    public TimeProto chngNNext(AddressProto addrProto) {
        logger.info("{} ChngNNext was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getNeighbours().nnext = ProtobufMapper.fromProtoToAddress(addrProto);
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public TimeProto chngNext(AddressProto addrProto) {
        logger.info("{} chngNext was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getNeighbours().next = ProtobufMapper.fromProtoToAddress(addrProto);
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public AddressProto chngPrev(AddressProto addrProto) {
        logger.info("{} ChngPrev was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getNeighbours().prev = ProtobufMapper.fromProtoToAddress(addrProto);
        return ProtobufMapper.AddressToProto(myNode.getNeighbours().next, this.myNode.getLamportTime());
    }

    @Override
    public TimeProto chngNNextOfPrev(AddressProto addrProto) {
        logger.info("{} chngNNextOfPrev was called ...", Utils.formatTime(this.myNode.getTime()));
        TimeProto timeProto = myNode.getCommHub().getPrev().chngNNext(addrProto);
        Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public TimeProto nodeMissing(AddressProto addrProto) {
        Address addr = ProtobufMapper.fromProtoToAddress(addrProto);
        logger.info("{} NodeMissing was called with {}", Utils.formatTime(this.myNode.getTime()), addr);
        if (addr.compareTo(myNode.getNeighbours().next) == 0) {
            // its for me
            DSNeighbours myNeighbours = myNode.getNeighbours();
            // to my nnext send msg ChPrev with myaddr -> my nnext = next
            myNeighbours.next = myNeighbours.nnext;
            myNeighbours.nnext = ProtobufMapper.fromProtoToAddress(
                    myNode.getCommHub().getNNext().chngPrev(ProtobufMapper.AddressToProto(myNode.getAddress(), this.myNode.getLamportTime()))
            );
            // to my prev send msg ChNNext to my.next
            TimeProto timeProto = myNode.getCommHub().getPrev().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            logger.info("{} NodeMissing DONE", Utils.formatTime(this.myNode.getTime()));
        } else {
            // send to next node
            TimeProto timeProto = myNode.getCommHub().getNext().nodeMissing(addrProto);
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
        }
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    public TimeProto nodeLeft(AddressProto addrProto) {
        Address addr = ProtobufMapper.fromProtoToAddress(addrProto);
        logger.info("{} NodeLeft was called with {}", Utils.formatTime(this.myNode.getTime()),addr);
        DSNeighbours myNeighbours = myNode.getNeighbours();
        /* 2 nodes cycle */
        if (myNode.getNeighbours().prev.compareTo(myNode.getNeighbours().next) == 0) {
            TimeProto timeProto = myNode.getCommHub().getPrev().chngNext(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            AddressProto addressProto = myNode.getCommHub().getPrev().chngPrev(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(addressProto.getTime(), this.myNode);
        }
        /* 3 nodes cycle */
        else if (myNode.getNeighbours().prev.compareTo(myNode.getNeighbours().nnext) == 0) {
            TimeProto timeProto = myNode.getCommHub().getNext().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);

            AddressProto addressProto = myNode.getCommHub().getNext().chngPrev(ProtobufMapper.AddressToProto(myNeighbours.prev, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(addressProto.getTime(), this.myNode);

            timeProto = myNode.getCommHub().getPrev().chngNext(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);

            timeProto = myNode.getCommHub().getPrev().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.prev, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
        }
        /* more than 3 nodes cycle */
        else {
            TimeProto timeProto = myNode.getCommHub().getPrev().chngNext(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);

            timeProto = myNode.getCommHub().getPrev().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.nnext, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);

            AddressProto addressProto = myNode.getCommHub().getNext().chngPrev(ProtobufMapper.AddressToProto(myNeighbours.prev, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(addressProto.getTime(), this.myNode);

            timeProto = myNode.getCommHub().getPrev().chngNNextOfPrev(ProtobufMapper.AddressToProto(myNeighbours.next, this.myNode.getLamportTime()));
            Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
        }
        logger.info("{} NodeMissing DONE" ,Utils.formatTime(this.myNode.getTime()));
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public TimeProto hello() {
        logger.info("{} Hello method called!", Utils.formatTime(this.myNode.getTime()));
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();

        // Send an empty response
//        responseObserver.onNext(Empty.getDefaultInstance());
//        // Signal that the call is complete
//        responseObserver.onCompleted();
    }

    @Override
    public TimeProto preliminaryRequest(RequestResourceMessageProto preliminaryRequestMessageProto) {
        if (preliminaryRequestMessageProto.getResourceId().equals(this.myNode.getResource().getId())) {
            logger.info("{} Preliminary request got on {} from {} for {}.", Utils.formatTime(this.myNode.getTime()),
                    myNode.getAddress(), ProtobufMapper.fromProtoToAddress(preliminaryRequestMessageProto.getRequesterAddress()),
                    preliminaryRequestMessageProto.getResourceId());
            this.myNode.processPreliminaryRequest(preliminaryRequestMessageProto);
        } else {
            logger.info("{} Preliminary request got on {} from {} for {}. Redirecting to the next node", Utils.formatTime(this.myNode.getTime()),
                    myNode.getAddress(), ProtobufMapper.fromProtoToAddress(preliminaryRequestMessageProto.getRequesterAddress()),
                    preliminaryRequestMessageProto.getResourceId());
            try{
                TimeProto timeProto = this.myNode.getCommHub().getNext().preliminaryRequest(preliminaryRequestMessageProto);
                Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            } catch (Exception e){
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next, this.myNode.getLamportTime()));
            }
        }
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public TimeProto requestResource(RequestResourceMessageProto requestResourceMessageProto) {
        if (requestResourceMessageProto.getResourceId().equals(this.myNode.getResource().getId())){
            logger.info("{} Request resource. Current node: ip: {}; port: {}, resourceId: {}. requested resource id: {}", Utils.formatTime(this.myNode.getTime()),
                    myNode.getMyIP(), myNode.getMyPort(), myNode.getResource().getId(), requestResourceMessageProto.getResourceId());
            myNode.requestResource(requestResourceMessageProto);
        } else {
            logger.info("{} Request resource. Current node: ip: {}; port: {}, resourceId: {}. requested resource id: {}. Redirecting to the next node.",
                    Utils.formatTime(this.myNode.getTime()),
                    myNode.getMyIP(), myNode.getMyPort(), myNode.getResource().getId(), requestResourceMessageProto.getResourceId());
            try{
                TimeProto timeProto = this.myNode.getCommHub().getNext().requestResource(requestResourceMessageProto);
                Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            } catch (Exception e){
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next, this.myNode.getLamportTime()));
            }
        }
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public TimeProto acquireResource(AcquireMessageProto acquireMessageProto) {
        if (this.myNode.getAddress().hostname.equals(acquireMessageProto.getRequesterAddress().getHostname())
        && this.myNode.getAddress().port == acquireMessageProto.getRequesterAddress().getPort()) {
            this.myNode.acquireResource(acquireMessageProto);
            logger.info("{} Acquire resource. Current node: {}; resourceId {}. Granted to: {}.", Utils.formatTime(this.myNode.getTime()),
                    myNode.getAddress(), myNode.getResource().getId(), ProtobufMapper.fromProtoToAddress(acquireMessageProto.getRequesterAddress()));
        } else {
            logger.info("{} Acquire resource. Current node: {}; resourceId {}. Granted to: {}. Redirecting to the next node", Utils.formatTime(this.myNode.getTime()),
                    myNode.getAddress(), myNode.getResource().getId(), ProtobufMapper.fromProtoToAddress(acquireMessageProto.getRequesterAddress()));
            try {
                TimeProto timeProto = this.myNode.getCommHub().getNext().acquireResource(acquireMessageProto);
                Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            } catch (Exception e){
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next, this.myNode.getLamportTime()));
            }

        }
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }

    @Override
    public TimeProto resourceWasReleased(ResourceProto resourceProto) {
        if (this.myNode.getResource().getId().equals(resourceProto.getId())){
            logger.info("{} Resource was released request on {}.", Utils.formatTime(this.myNode.getTime()), myNode.getAddress());
            this.myNode.processReleaseResource(resourceProto);
        } else {
            logger.info("{} Resource was released request on {}. Redirecting to the next node", Utils.formatTime(this.myNode.getTime()), myNode.getAddress());
            try {
                TimeProto timeProto = this.myNode.getCommHub().getNext().resourceWasReleased(resourceProto);
                Utils.updateTimeOnReceive(timeProto.getTime(), this.myNode);
            } catch (Exception e){
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next, this.myNode.getLamportTime()));
            }
        }
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }
}
