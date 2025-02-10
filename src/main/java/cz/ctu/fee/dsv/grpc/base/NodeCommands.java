package cz.ctu.fee.dsv.grpc.base;

import cz.ctu.fee.dsv.*;
import cz.ctu.fee.dsv.grpc.exceptions.NodeAlreadyJointException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeCommands {
        DSNeighboursProto join(AddressProto addr);
        void chngNNext(AddressProto addr);
        void chngNext(AddressProto addr);
        AddressProto chngPrev(AddressProto addr);
        void chngNNextOfPrev(AddressProto addr);
        void nodeMissing(AddressProto addr);
        void nodeLeft(AddressProto addrProto);
        void hello();
        TimeProto preliminaryRequest(RequestResourceMessageProto requestMessageProto);
        TimeProto requestResource(RequestResourceMessageProto id);
        TimeProto acquireResource(AcquireMessageProto acquireResponseMessage);
        TimeProto resourceWasReleased(ResourceProto resourceProto);
}