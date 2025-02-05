package cz.ctu.fee.dsv.grpc.exceptions;

public class NodeAlreadyJointException extends Exception{

    public NodeAlreadyJointException(String errorMessage) {
        super(errorMessage);
    }
}
