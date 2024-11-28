package bookstore;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BookSellerAgent extends Agent {
    private Hashtable catalogue;
    private BookSellerGUI myGUI;
    
    protected void setup() {
        System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");
        catalogue = new Hashtable();
        myGUI = new BookSellerGUI(this);
        myGUI.show();
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-selling");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(BookSellerAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
    }
    
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            Logger.getLogger(BookSellerAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        myGUI.dispose();
        System.out.println("Seller-agent "+getAID().getName()+" terminated.");
    }
    
    public void updateCatalogue(final String title, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(title, new Integer(price));
            }
        });
    }
    
    public class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer)catalogue.get(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                send(reply);
            } else
                block();
        }
    }
    
    public class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                catalogue.remove(msg.getContent());
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(msg.getContent());
                send(reply);
            } else
                block();
        }
    }
}