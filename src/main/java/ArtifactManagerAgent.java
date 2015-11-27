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

import java.util.ArrayList;
import java.util.List;

public class ArtifactManagerAgent extends Agent {

    private final int SPAWN_TIME = 15000;
    private static final String NAME = "(ArtifactManager)";
    private FSMBehaviour fsm;
    //0
    private static final String START_AUCTION = "A";
    //1
    private static final String CFP = "B";
    //2
    private static final String WAIT_FOR_BIDS = "C";
    //3
    private static final String COMPLETE_AUCTION = "D";

    //State
    private ArrayList<AID> buyers;
    private float lastProposedPrice;
    private float leastAcceptablePrice;

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
            fsm.registerState(new CallForProposal(getAgent()), CFP);
            fsm.registerState(new WaitForBidOrRejects(), WAIT_FOR_BIDS);
            fsm.registerLastState(new CompleteAuction(), COMPLETE_AUCTION);

            fsm.registerDefaultTransition(START_AUCTION, CFP);
            fsm.registerTransition(CFP,WAIT_FOR_BIDS,2);
            fsm.registerTransition(WAIT_FOR_BIDS, COMPLETE_AUCTION, 3);
            fsm.registerTransition(CFP, COMPLETE_AUCTION, 3);
            fsm.registerTransition(WAIT_FOR_BIDS, CFP, 1);

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
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setSender(getAID());
                msg.setContent("inform-start-of-auction");
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
                if (result.length>0) {
                    for(DFAgentDescription r: result) {
                        msg.addReceiver(r.getName());
                        buyers.add(r.getName());
                    }
                }
                send(msg);
        } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    private class CallForProposal extends OneShotBehaviour {

        public CallForProposal(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            for(int i = 0 ; i < buyers.size() ; i++){
                blockingReceive()
            }
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            msg.setSender(getAgent().getAID());
            msg.setContent("call-for-proposal");
        }

        @Override
        public int onEnd(){
            //TODO
            //if lowest acceptable price wasn't accepted,
            //return 3 COMPLETE_AUCTION

            //else (auction still ongoing)
            //return 2 WAIT_FOR_BIDS
            return -1;
        }
    }

    private class WaitForBidOrRejects extends OneShotBehaviour{

        @Override
        public void action() {
            for (int i = 0; i< buyers.size(); i++){
                ACLMessage msg = blockingReceive();
                //Set state complete auction or new bid or finished.
            }

        }

        @Override
        public int onEnd(){
            //TODO
            //If bid accepted, return 3 AUCTION_COMPLETE

            //Else return 1 CFP
            return -1;
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
