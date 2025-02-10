package cz.ctu.fee.dsv.grpc;

import cz.ctu.fee.dsv.grpc.exceptions.KilledNodeActsAsClientException;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class APIHandler {
    final Logger logger = LoggerFactory.getLogger(APIHandler.class);

    private int port = 7000;
    private Node myNode = null;
    private Javalin app = null;


    public APIHandler(Node myNode, int port) {
        this.myNode = myNode;
        this.port = port;
    }

    public APIHandler(Node myNode) {
        this(myNode, 7000);
    }


    public void start() {
        this.app = Javalin.create()
                .get("/setDelay/{delay}", ctx -> {
                    logger.info("Setting delay: {}", ctx.pathParam("delay"));
                    myNode.setDelay(Integer.parseInt(ctx.pathParam("delay")));
                    ctx.result("Delay was set up. ip: " + myNode.getMyIP() + " port: " + myNode.getMyPort()+"\n");
                })
                .get("/join/{other_node_ip}/{other_node_port}", ctx -> {
//                    myNode.sleepForDelay();
                    logger.info("Joining node: {}: {}", ctx.pathParam("other_node_ip"), ctx.pathParam("other_node_port"));
                    myNode.join(ctx.pathParam("other_node_ip"), Integer.parseInt(ctx.pathParam("other_node_port")));
                    ctx.result("Tried to join to: " + ctx.pathParam("other_node_ip") + " " + ctx.pathParam("other_node_port") + "\n");
                })
                .get("/leave", ctx -> {
//                    myNode.sleepForDelay();
                    logger.info("Node is leaving. ip: {} port: {}", myNode.getMyIP(), myNode.getMyPort());
                    myNode.leave();
                    ctx.result("Node left. ip: " + myNode.getMyIP() + " port: " + myNode.getMyPort()+"\n");
                })
                .get("/kill", ctx -> {
//                    myNode.sleepForDelay();
                    logger.info("Node is being killed. ip: {} port: {}", myNode.getMyIP(), myNode.getMyPort());
                    myNode.kill();
                    ctx.result("Node was killed. ip: " + myNode.getMyIP() + " port: " + myNode.getMyPort()+"\n");
                })
                .get("/revive", ctx -> {
//                    myNode.sleepForDelay();
                    logger.info("Node is being revived. ip: {} port: {}", myNode.getMyIP(), myNode.getMyPort());
                    try {
                        myNode.revive();
                        ctx.result("Node was revived. ip: " + myNode.getMyIP() + "port: " + myNode.getMyPort()+"\n");
                    } catch (Exception e){
                        ctx.result("Failed to revive node ip: " + myNode.getMyIP() + " port: " + myNode.getMyPort() + "\n");
                    }
                })
                .get("/preliminary_request/{resource_id}", ctx -> {
//                    myNode.sleepForDelay();
                    logger.info("Sending preliminary request from {} to get {}.", myNode.getAddress(), ctx.pathParam("resource_id"));
                    myNode.sendPreliminaryRequests(ctx.pathParam("resource_id"));
                })
                .post("/preliminary_requests", ctx -> {
//                    myNode.sleepForDelay();
                    List<String> resourceIds = ctx.bodyAsClass(List.class);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Sending preliminary request from ").append(myNode.getAddress())
                            .append(" to get ");
                    for (String resourceId : resourceIds) {
                        stringBuilder.append("[").append(resourceId).append(" ], ");
                    }
                    logger.info(stringBuilder.toString());
                    myNode.sendPreliminaryRequests(resourceIds);

                })
                .get("/request_resource/{resource_id}", ctx -> {
//                    myNode.sleepForDelay();
                    logger.info("Sending request to get {}. {}", ctx.pathParam("resource_id"), myNode.getAddress());
                    myNode.requestResource(ctx.pathParam("resource_id"));
                })
                .get("/release_resource/{resource_id}", ctx -> {
                    logger.info("Sending release source {}. {}", ctx.pathParam("resource_id"), myNode.getAddress());
                    try{
                        myNode.sleepForDelay();
                        myNode.releaseResource(ctx.pathParam("resource_id"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .get("/send_hello_next", ctx -> {
                    logger.info("Sending hello to next node");
                    try {
                        myNode.sleepForDelay();
                        myNode.sendHelloToNext();
                        ctx.result("Hello sent\n");
                    } catch (KilledNodeActsAsClientException e){
                        ctx.result("Node is not alive. Try to revive it.\n").status(500);
                    }
                })
                .get("/get_status", ctx -> {
                    myNode.sleepForDelay();
                    logger.info("Getting status of this node");
                    myNode.printStatus();
                    ctx.result(myNode.getStatus() + "\n");
//                    ctx.json(myNode);
                })
//                .get("/start_grpc_server", ctx -> {
//                    logger.info("Starting grpc server part.");
//                    myNode.startGrpc();
//                    ctx.result("grpc server started\n");
//                })
//                .get("/stop_grpc_server", ctx -> {
//                    logger.info("Stopping grpc server part.");
//                    myNode.stopGrpc();
//                    myNode.resetoNodeInTopology();
//                    ctx.result("grpc server + topology info reset\n");
//                })
                // try to add command for starting election
                .start(this.port);
    }
}

