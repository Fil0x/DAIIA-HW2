import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;


public class CuratorAgent extends Agent {
    private static final String NAME = "(curator)";

    private String senderType;
    private AID sender;
    private ArtifactIndex artifactIndex = new ArtifactIndex();

    // A single state FSM(for now) that works like a cyclic behaviour.
    // The only behaviour is to wait for requests from either the profiler or platform.
    public CuratorAgent(){
        super();

        MessageTemplate mtartifact = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        MessageTemplate mtauction = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);

        addBehaviour(new ArtifactRequestREResponder(this, mtartifact));
        addBehaviour(new AuctionREResponder(this, mtauction));
    }

    protected void setup(){
        // Register both services to DF
        // Artifact search
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType("artifact-search");
        sd1.setName("provide-artifact-search");
        sd1.addOntologies("request-ids");

        // Artifact lookup based on item ID
        ServiceDescription sd2 = new ServiceDescription();
        sd2.setType("artifact-lookup");
        sd2.setName("provide-artifact-lookup");
        sd2.addOntologies("request-iteminfo");

        // Register interest for auctions
        ServiceDescription sd3 = new ServiceDescription();
        sd3.setType("auction-bidder");
        sd3.setName("auction-participator");
        sd3.addOntologies("bid-in-auctions");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd1);
        dfd.addServices(sd2);
        dfd.addServices(sd3);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class ArtifactRequestREResponder  extends SimpleAchieveREResponder {

        public ArtifactRequestREResponder (Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage prepareResponse(ACLMessage request){
            ACLMessage reply = request.createReply();
            //System.out.println("(Curator) preparing response to request from: " + request.getSender().getLocalName());
            ////System.out.println("(Curator) with content: " + request.getContent());
            senderType = request.getEnvelope().getComments();
            if ("platform".equals(senderType)){
                //System.out.println("(Curator) Message received from a platform called: " + request.getSender().getLocalName());
                reply.setPerformative(ACLMessage.AGREE);

            } else if ("profiler".equals(senderType)){
                //System.out.println("(Curator) Message received from a profiler called: "+ request.getSender().getLocalName());
                reply.setPerformative(ACLMessage.AGREE);

            } else {
                reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);

            }
            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){

            ACLMessage reply = request.createReply();

            if("platform".equals(senderType)){
                User u;
                try {
                    u = (User)request.getContentObject();
                    reply.setContentObject((Serializable)artifactIndex.searchArtifactIDs(u.getInterests()));

                } catch (UnreadableException e) {
                    System.err.println("(Curator) Could not deserialize user");
                } catch (IOException e) {
                    System.err.println("(Curator) Could not serialize artifact id list");
                }

            } else if ("profiler".equals(senderType)){
                List<Integer> artifactIDs;
                try {
                    artifactIDs = (List<Integer>)request.getContentObject();
                    reply.setContentObject((Serializable)artifactIndex.searchArtifacts(artifactIDs));
                } catch (UnreadableException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            reply.setPerformative(ACLMessage.INFORM);
            reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

            return reply;
        }
    }

    private class AuctionREResponder extends SimpleAchieveREResponder{

        private MessageTemplate mt;

        public AuctionREResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
            this.mt = mt;
        }

        protected ACLMessage prepareResponse(ACLMessage request){
            //first message is a start auction message
            AID auctioneer = request.getSender();

            //send confirm
            ACLMessage interested = new ACLMessage(ACLMessage.AGREE);
            interested.addReceiver(auctioneer);
            send(interested);

            //handle CFP
            ACLMessage cfpOrComplete;
            while(true) {
                cfpOrComplete = blockingReceive(mt);
                String comments = cfpOrComplete.getEnvelope().getComments();
                if("cfp".equals(comments)){
                    System.out.println(NAME + " got cfp");
                    ACLMessage bidOrReject = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    send(bidOrReject);
                } else if ("win".equals(comments)) {
                    break;
                } else if ("end".equals(comments)){
                    break;
                }
            }

            //handle end

            ACLMessage reply = request.createReply();
            return reply;
        }


        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            return null;
        }
    }
}
