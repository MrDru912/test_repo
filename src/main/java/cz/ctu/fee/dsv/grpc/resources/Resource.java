package cz.ctu.fee.dsv.grpc.resources;

import cz.ctu.fee.dsv.RequestResourceMessageProto;
import cz.ctu.fee.dsv.ResourceProto;
import cz.ctu.fee.dsv.grpc.lomet.Graph;
import cz.ctu.fee.dsv.grpc.lomet.GraphNode;
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
    private GraphNode lastPreliminaryRequester;
    private LinkedList<RequestResourceMessageProto> delayedRequestsList = new LinkedList();

    private GraphNode currentOwningProcessId;
    private boolean resourceFree = true;
    private GraphNode resourceNode;

    public Resource(String id, String data) {
        this.id = id;
        this.data = data;
        this.graph = new Graph();
        this.resourceNode = new GraphNode(id, -1);
        this.graph.addVertex(resourceNode);
    }


    public boolean processRequest(RequestResourceMessageProto requestResourceMessageProto) {
        String requesterProcessId = getProcessId(requestResourceMessageProto.getRequesterAddress());
        int timestamp = requestResourceMessageProto.getTime();
        GraphNode requesterNode = new GraphNode(requesterProcessId, timestamp);
        logger.info("Processing request from " + requesterProcessId);
        if (this.isResourceFree()) {
            /* revert direction of process->resource edge to resource->process */
            this.getGraph().removeEdge(requesterNode, this.resourceNode);
            this.getGraph().addEdge(this.resourceNode, requesterNode);
            if (this.getGraph().hasCycle()){ /* refuse the request and keep it pending */
                logger.info("Resource was not granted to {} " +
                        ".Resource request was delayed. Cycle was detected in the dependency graph on resource {}.",requesterProcessId, this.getId());

                /* reverting direction of edge back */
                this.getGraph().removeEdge(this.resourceNode, requesterNode);
                this.getGraph().addEdge(requesterNode, this.resourceNode);

                /* adding request to the delayed requests list to process it as the resource is released */
                delayRequest(requestResourceMessageProto);
                logger.info("Graph after delay:\n{}", this.getGraph().getStringGraph());
                return false;
            } else { /* granting the resource to the requester */
                logger.info("Resource {} was granted to {}", this.getId(), requesterProcessId);
                this.setResourceFree(false);
                this.setCurrentOwningProcessId(new GraphNode(requesterProcessId, -1));

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
            this.setLastPreliminaryRequester(null);
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

    public GraphNode getLastPreliminaryRequester() {
        return lastPreliminaryRequester;
    }

    public void setLastPreliminaryRequester(GraphNode lastPreliminaryRequester) {
        this.lastPreliminaryRequester = lastPreliminaryRequester;
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
                ", data=" + data + ", lastPreliminaryRequest: " + lastPreliminaryRequester + ", " + ']';
    }

    public void processPreliminaryRequest(RequestResourceMessageProto preliminaryRequestMessageProto) {
        String requesterId = getProcessId(
                preliminaryRequestMessageProto.getRequesterAddress().getHostname(),
                preliminaryRequestMessageProto.getRequesterAddress().getPort());
        int preliminaryRequestTimestamp = preliminaryRequestMessageProto.getTime();
        GraphNode requesterProcessNode = new GraphNode(requesterId, preliminaryRequestTimestamp);
        logger.info("Processing preliminary request, requesterNode: {}.", requesterProcessNode);
        /* Adding dependency process->resource  */
        this.getGraph().addVertex(requesterProcessNode);
        this.getGraph().addEdge(requesterProcessNode, this.resourceNode);

        /* If some other process is dependent on the resource then adding dependency to the last process */
        GraphNode lastRequester = this.getLastPreliminaryRequester();
        if (lastRequester != null) {
            graph.processPreliminaryRequest(requesterProcessNode ,lastRequester, this.resourceNode);
        }
        this.setLastPreliminaryRequester(requesterProcessNode);

        logger.info("Preliminary request from {} was processed and dependency graph was updated.\n{}", requesterProcessNode, this.graph.getStringGraph());
    }

    public GraphNode getCurrentOwningProcessId() {
        return currentOwningProcessId;
    }

    public void setCurrentOwningProcessId(GraphNode currentOwningProcessId) {
        this.currentOwningProcessId = currentOwningProcessId;
    }
}
