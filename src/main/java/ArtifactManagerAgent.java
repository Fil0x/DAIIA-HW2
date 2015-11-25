import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.List;

public class ArtifactManagerAgent extends Agent {

    private int SPAWN_TIME = 3000;
    private static final String NAME = "(ArtifactManager)";

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
            /*
             The sequential behaviour simulates the procedure to spawn an auction and publish it.
             1 - Start the auction
             */
            SequentialBehaviour sb = new SequentialBehaviour(getAgent());
            sb.addSubBehaviour(new StartAuction());
            getAgent().addBehaviour(sb);
        }
    }

    private class StartAuction extends OneShotBehaviour {
        public StartAuction(){
            super();
        }

        @Override
        public void action() {
            try {
                // Get the platform AID and ask the DF about the presence of the platform.
                AID platfom;
                DFAgentDescription dfd = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("provide-tour");
                dfd.addServices(sd);
                DFAgentDescription[] result = DFService.search(getAgent(), dfd);
                if (result.length>0) {
                    for(DFAgentDescription r: result) {
                        System.out.println(NAME + ": STARTED");
                        System.out.println(r.getName().getName());
                    }
                }
        } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }
}
