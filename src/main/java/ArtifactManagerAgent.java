import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class ArtifactManagerAgent extends Agent {

    private final int SPAWN_TIME = 15000;
    private static final String NAME = "(ArtifactManager)";
    private FSMBehaviour fsm;
    //0
    private static final String START_AUCTION = "A";
    //1
    private static final String CFP = "B";
    //2
    private static final String COMPLETE_AUCTION = "C";

    //State
    private ArrayList<AID> buyers;
    private float lastProposedPrice;
    private float leastAcceptablePrice;
    private int nResponders;
    private ACLMessage msgToSend;

    //Boolean for both accept bid and no bids (in end of WAIT_FOR_BIDS false, go to CFP)
    private boolean proposalAccepted;
    private Artifact itemToSell;

    public ArtifactManagerAgent() {
        super();

        addBehaviour(new AuctionSpawner(this, SPAWN_TIME));
    }

    protected void setup() {
        System.out.println("Agent:" + getAID().getName() + " is ready!");
    }

    protected void takeDown() {
        System.out.println("Agent:" + getAID().getName() + " terminating...");
    }

    private class AuctionSpawner extends TickerBehaviour {
        public AuctionSpawner(Agent a, long period) {
            super(a, period);
        }

        @Override
        public void onStart() {
            System.out.println("Agent:" + getAgent().getName() + "[Ticker:" + getPeriod() + "] is ready!");
        }

        @Override
        protected void onTick() {
            fsm = new FSMBehaviour();
            fsm.registerFirstState(new StartAuction(getAgent()), START_AUCTION);
            fsm.registerState(new HandleAuction(getAgent(), msgToSend), CFP);
            fsm.registerLastState(new CompleteAuction(), COMPLETE_AUCTION);

            fsm.registerDefaultTransition(START_AUCTION, CFP);
            fsm.registerDefaultTransition(CFP, COMPLETE_AUCTION);

            getAgent().addBehaviour(fsm);
        }
    }

    private class StartAuction extends OneShotBehaviour {
        public StartAuction(Agent a){
            super(a);
        }

        @Override
        public void action() {
            try {
                System.out.println(NAME + ": soon starting a new dutch auction");
                buyers = new ArrayList<>();
                // Wait for the other agents to boot
                doWait(100);
                // Get the interested buyers-curators
                DFAgentDescription dfd = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("auction-bidder");
                dfd.addServices(sd);
                DFAgentDescription[] result = DFService.search(getAgent(), dfd);
                // Create a new message to broadcast to the interested bidders
                ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.setSender(getAID());
                msg.setContentObject(Utilities.getArtifact());
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
                if (result.length>0) {
                    nResponders = result.length;
                    for(DFAgentDescription r: result) {
                        msg.addReceiver(r.getName());
                        buyers.add(r.getName());
                    }
                    msgToSend = msg;
                }
            } catch (FIPAException e) {
                    e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class HandleAuction extends ContractNetInitiator {

        public HandleAuction(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        protected void handlePropose(ACLMessage propose, Vector v) {
            System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
        }

        protected void handleRefuse(ACLMessage refuse) {
            System.out.println("Agent "+refuse.getSender().getName()+" refused");
        }

        protected void handleFailure(ACLMessage failure) {
            if (failure.getSender().equals(myAgent.getAMS())) {
                // FAILURE notification from the JADE runtime: the receiver
                // does not exist
                System.out.println("Responder does not exist");
            }
            else {
                System.out.println("Agent "+failure.getSender().getName()+" failed");
            }
            // Immediate failure --> we will not receive a response from this agent
            nResponders--;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            if (responses.size() < nResponders) {
                // Some responder didn't reply within the specified timeout
                System.out.println("Timeout expired: missing "+(nResponders - responses.size())+" responses");
            }
            // Evaluate proposals.
            int bestProposal = -1;
            AID bestProposer = null;
            ACLMessage accept = null;
            Enumeration e = responses.elements();
            while (e.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) e.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    acceptances.addElement(reply);
                    int proposal = Integer.parseInt(msg.getContent());
                    if (proposal > bestProposal) {
                        bestProposal = proposal;
                        bestProposer = msg.getSender();
                        accept = reply;
                    }
                }
            }
            // Accept the proposal of the best proposer
            if (accept != null) {
                System.out.println("Accepting proposal "+bestProposal+" from responder "+bestProposer.getName());
                accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            }
        }

        protected void handleInform(ACLMessage inform) {
            System.out.println("Agent "+inform.getSender().getName()+" successfully performed the requested action");
        }
    }

    private class CompleteAuction extends OneShotBehaviour{

        @Override
        public void action() {

            //if lowest price hit, inform everyone auction closed

            //if someone bid, inform winner and everyone else auction closed

        }
    }
}
