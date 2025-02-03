package cz.ctu.fee.dsv.grpc.resources;

import cz.ctu.fee.dsv.RequestResourceMessageProto;
import cz.ctu.fee.dsv.ResourceProto;
import cz.ctu.fee.dsv.grpc.lomet.Graph;
import cz.ctu.fee.dsv.grpc.mappers.ProtobufMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static cz.ctu.fee.dsv.grpc.utils.Utils.getProcessId;

public class Resource {
    final Logger logger = LoggerFactory.getLogger(Resource.class);

    private String id;
    private String data;
    private Graph graph;
    private String lastPreliminaryRequesterId;
    private LinkedList<RequestResourceMessageProto> delayedRequestsList = new LinkedList();

    private String currentOwningProcessId;
    private boolean resourceFree = true;

    public Resource(String id, String data) {
        this.id = id;
        this.data = data;
        this.graph = new Graph();
        this.graph.addVertex(id);
    }


    public boolean processRequest(RequestResourceMessageProto requestResourceMessageProto) {
        String requesterProcessId = getProcessId(requestResourceMessageProto.getRequesterAddress());
        logger.info("Processing request from " + requesterProcessId);
        if (this.isResourceFree()) {
            /* revert direction of process->resource edge to resource->process */
            this.getGraph().removeEdge(requesterProcessId, this.getId());
            this.getGraph().addEdge(this.getId(), requesterProcessId);
            if (this.getGraph().hasCycle()){ /* refuse the request and keep it pending */
                logger.info("Resource was not granted to {} " +
                        ".Resource request was delayed. Cycle was detected in the dependency graph on resource {}.",requesterProcessId, this.getId());

                /* reverting direction of edge back */
                this.getGraph().removeEdge(this.getId(), requesterProcessId);
                this.getGraph().addEdge(requesterProcessId, this.getId());

                /* adding request to the delayed requests list to process it as the resource is released */
                delayRequest(requestResourceMessageProto);
                logger.info("Graph after delay:\n{}", this.getGraph().getStringGraph());
                return false;
            } else { /* granting the resource to the requester */
                logger.info("Resource {} was granted to {}", this.getId(), requesterProcessId);
                this.setResourceFree(false);
                this.setCurrentOwningProcessId(requesterProcessId);

                /* deleting request from delayed requests */
                this.getDelayedRequestsList()
                        .removeIf(request -> request.getRequesterAddress().equals(requestResourceMessageProto.getRequesterAddress()));
                logger.info("Graph after granting resource:\n{}", this.getGraph().getStringGraph());
                return true;
                }
        } else {
            logger.info("Resource was not granted to {} because it is taken by another process. " +
                    "Resource request on resource {} delayed.", requesterProcessId, this.getId());
            delayRequest(requestResourceMessageProto);
            return false;
        }
    }

    public void delayRequest(RequestResourceMessageProto requestResourceMessageProto){
        boolean delayRequestsQueueAlreadyContainsRequest = this.getDelayedRequestsList().stream()
                .anyMatch(r -> r.getRequesterAddress().equals(requestResourceMessageProto.getRequesterAddress())
                        && r.getResourceId().equals(requestResourceMessageProto.getResourceId()));
        if (!delayRequestsQueueAlreadyContainsRequest) {
            this.getDelayedRequestsList().add(requestResourceMessageProto);
        }
    }

    public RequestResourceMessageProto processReleasedResource(ResourceProto resourceProto){

        /* removing process vertex with all dependencies from resource graph*/
        this.getGraph().removeVertex(this.currentOwningProcessId);

        /* updating the resource */
        this.setData(resourceProto.getData());
        this.setResourceFree(true);
        this.setCurrentOwningProcessId(null);
        logger.info("Resource {} was released\n{}", this.getId(), this.graph.getStringGraph());

        // Processing delayed requests
        if (!this.getDelayedRequestsList().isEmpty()){
            RequestResourceMessageProto request = this.getDelayedRequestsList().poll();
            logger.info("Processing the earliest delayed request from {}", ProtobufMapper.fromProtoToAddress(request.getRequesterAddress()));
            return request;
        } else {
            logger.info("No delayed requests to process");
            this.setLastPreliminaryRequesterId(null);
            return null;
        }
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public Graph getGraph() {
        return graph;
    }

    public String getLastPreliminaryRequesterId() {
        return lastPreliminaryRequesterId;
    }

    public void setLastPreliminaryRequesterId(String lastPreliminaryRequesterId) {
        this.lastPreliminaryRequesterId = lastPreliminaryRequesterId;
    }

    public LinkedList<RequestResourceMessageProto> getDelayedRequestsList() {
        return delayedRequestsList;
    }

    public boolean isResourceFree() {
        return resourceFree;
    }

    public void setResourceFree(boolean resourceFree) {
        this.resourceFree = resourceFree;
    }

    @Override
    public String toString() {
        return "Resource[" +
                "id=" + id +
                ", data=" + data + ", lastPreliminaryRequest: " + lastPreliminaryRequesterId + ", " + ']';
    }

    public void processPreliminaryRequest(RequestResourceMessageProto preliminaryRequestMessageProto) {
        String requesterId = getProcessId(
                preliminaryRequestMessageProto.getRequesterAddress().getHostname(),
                preliminaryRequestMessageProto.getRequesterAddress().getPort());

        /* Adding dependency process->resource  */
        this.getGraph().addVertex(requesterId);
        this.getGraph().addEdge(requesterId, this.getId());

        /* If some other process is dependent on the resource then adding dependency to that process */
        String lastRequesterId = this.getLastPreliminaryRequesterId();
        if (lastRequesterId != null) {
            this.getGraph().addEdge(requesterId, lastRequesterId);
        }
        this.setLastPreliminaryRequesterId(requesterId);

        logger.info("Preliminary request was processed and dependency graph was updated.\n{}", this.graph.getStringGraph());

    }

    public String getCurrentOwningProcessId() {
        return currentOwningProcessId;
    }

    public void setCurrentOwningProcessId(String currentOwningProcessId) {
        this.currentOwningProcessId = currentOwningProcessId;
    }
}
