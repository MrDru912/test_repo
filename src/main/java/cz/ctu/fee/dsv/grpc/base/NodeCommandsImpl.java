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
            return ProtobufMapper.DSNeighboursToProto(myNode.getNeighbours());
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
            myNode.getCommHub().getNext().chngPrev(protoAddr);
            // to my (initial) prev send msg ChNNext addr
            myNode.getCommHub().getGrpcProxy(myInitialPrev).chngNNext(protoAddr);
            tmpNeighbours.nnext = myNeighbours.nnext;
            // handle myself
            myNeighbours.nnext = myInitialNext;
            myNeighbours.next = addr;
            return ProtobufMapper.DSNeighboursToProto(tmpNeighbours);
        }
    }


    @Override
    public void chngNNext(AddressProto addrProto) {
        logger.info("{} ChngNNext was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getNeighbours().nnext = ProtobufMapper.fromProtoToAddress(addrProto);
    }

    @Override
    public void chngNext(AddressProto addrProto) {
        logger.info("{} chngNext was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getNeighbours().next = ProtobufMapper.fromProtoToAddress(addrProto);
    }

    @Override
    public AddressProto chngPrev(AddressProto addrProto) {
        logger.info("{} ChngPrev was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getNeighbours().prev = ProtobufMapper.fromProtoToAddress(addrProto);
        return ProtobufMapper.AddressToProto(myNode.getNeighbours().next);
    }

    @Override
    public void chngNNextOfPrev(AddressProto addrProto) {
        logger.info("{} chngNNextOfPrev was called ...", Utils.formatTime(this.myNode.getTime()));
        myNode.getCommHub().getPrev().chngNNext(addrProto);
    }

    @Override
    public void nodeMissing(AddressProto addrProto) {
        Address addr = ProtobufMapper.fromProtoToAddress(addrProto);
        logger.info("{} NodeMissing was called with {}", Utils.formatTime(this.myNode.getTime()), addr);
        if (addr.compareTo(myNode.getNeighbours().next) == 0) {
            // its for me
            DSNeighbours myNeighbours = myNode.getNeighbours();
            // to my nnext send msg ChPrev with myaddr -> my nnext = next
            myNeighbours.next = myNeighbours.nnext;
            myNeighbours.nnext = ProtobufMapper.fromProtoToAddress(
                    myNode.getCommHub().getNNext().chngPrev(ProtobufMapper.AddressToProto(myNode.getAddress()))
            );
            // to my prev send msg ChNNext to my.next
            myNode.getCommHub().getPrev().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.next));
            logger.info("{} NodeMissing DONE", Utils.formatTime(this.myNode.getTime()));
        } else {
            // send to next node
            myNode.getCommHub().getNext().nodeMissing(addrProto);
        }
    }

    public void nodeLeft(AddressProto addrProto) {
        Address addr = ProtobufMapper.fromProtoToAddress(addrProto);
        logger.info("{} NodeLeft was called with {}", Utils.formatTime(this.myNode.getTime()),addr);
        DSNeighbours myNeighbours = myNode.getNeighbours();
        /* 2 nodes cycle */
        if (myNode.getNeighbours().prev.compareTo(myNode.getNeighbours().next) == 0) {
            myNode.getCommHub().getPrev().chngNext(ProtobufMapper.AddressToProto(myNeighbours.next));
            myNode.getCommHub().getPrev().chngPrev(ProtobufMapper.AddressToProto(myNeighbours.next));
        }
        /* 3 nodes cycle */
        else if (myNode.getNeighbours().prev.compareTo(myNode.getNeighbours().nnext) == 0) {
            myNode.getCommHub().getNext().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.next));

             myNode.getCommHub().getNext().chngPrev(ProtobufMapper.AddressToProto(myNeighbours.prev));
             myNode.getCommHub().getPrev().chngNext(ProtobufMapper.AddressToProto(myNeighbours.next));
             myNode.getCommHub().getPrev().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.prev));
        }
        /* more than 3 nodes cycle */
        else {
            myNode.getCommHub().getPrev().chngNext(ProtobufMapper.AddressToProto(myNeighbours.next));

            myNode.getCommHub().getPrev().chngNNext(ProtobufMapper.AddressToProto(myNeighbours.nnext));

            myNode.getCommHub().getNext().chngPrev(ProtobufMapper.AddressToProto(myNeighbours.prev));

            myNode.getCommHub().getPrev().chngNNextOfPrev(ProtobufMapper.AddressToProto(myNeighbours.next));
        }
        logger.info("{} NodeMissing DONE" ,Utils.formatTime(this.myNode.getTime()));
    }

    @Override
    public void hello() {
        logger.info("{} Hello method called!", Utils.formatTime(this.myNode.getTime()));

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
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next));
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
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next));
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
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next));
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
                this.nodeMissing(ProtobufMapper.AddressToProto(myNode.getNeighbours().next));
            }
        }
        return TimeProto.newBuilder().setTime(this.myNode.getLamportTime()).build();
    }
}
