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

    private ArrayList<AID> buyers;
    private int SPAWN_TIME = 3000;
    private static final String NAME = "(ArtifactManager)";
    private FSMBehaviour fsm;

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
            // Start a new auction
          //  getAgent().addBehaviour(new StartAuction());
            fsm = new FSMBehaviour();
            fsm.registerFirstState(new StartAuction(getAgent()), "A");
            fsm.registerLastState(new CallForProposal(getAgent()), "B");

            fsm.registerDefaultTransition("A", "B");

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
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            msg.setSender(getAgent().getAID());
            msg.setContent("call-for-proposal");
            msg.setProtocol();
        }
    }
}
