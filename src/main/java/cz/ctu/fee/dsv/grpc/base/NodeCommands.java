package cz.ctu.fee.dsv.grpc.base;

import cz.ctu.fee.dsv.*;
import cz.ctu.fee.dsv.grpc.exceptions.NodeAlreadyJointException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeCommands {
        DSNeighboursProto join(AddressProto addr);
        TimeProto chngNNext(AddressProto addr);
        TimeProto chngNext(AddressProto addr);
        AddressProto chngPrev(AddressProto addr);
        TimeProto chngNNextOfPrev(AddressProto addr);
        TimeProto nodeMissing(AddressProto addr);
        TimeProto nodeLeft(AddressProto addrProto);
        TimeProto hello();
        TimeProto preliminaryRequest(RequestResourceMessageProto requestMessageProto);
        TimeProto requestResource(RequestResourceMessageProto id);
        TimeProto acquireResource(AcquireMessageProto acquireResponseMessage);
        TimeProto resourceWasReleased(ResourceProto resourceProto);
}