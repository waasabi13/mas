package bookstore;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookBuyerAgent extends Agent {
    private AID[] sellerAgents;
    private String targetBookTitle;
    
    protected void setup() {
        System.out.println("Hello! Buyer-agent "+getAID().getName()+" is ready.");
        
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            targetBookTitle = (String)args[0];
            System.out.println("Buyer-agent "+getAID().getName()+" trying to buy "+targetBookTitle);
            
            ServiceDescription sd = new ServiceDescription();
            sd.setType("book-selling");
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);
            addBehaviour(new TickerBehaviour(this, 5 * 1000) {
                protected void onTick() {
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, dfd);
                        sellerAgents = new AID[result.length];
                        for(int i = 0; i < result.length; i++)
                            sellerAgents[i] = result[i].getName();
                        addBehaviour(new RequestPerformer());
                    } catch (FIPAException ex) {
                        Logger.getLogger(BookBuyerAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        } else {
            System.out.println("Buyer-agent "+getAID().getName()+" haven't specific title.");
            doDelete();
        }
    }
    
    protected void takeDown() {
        System.out.println("Buyer-agent "+getAID().getName()+" terminated.");
    }
    
    public class RequestPerformer extends Behaviour { 
        private AID bestSeller;
        private int bestPrice;
        private int repliesCnt = 0;
        private MessageTemplate mt; 
        private int step = 0; 
 
        public void action() {
            switch (step) {
                case 0: 
                    // отправим запрос на книгу всем продавцам 
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP); 
                    for (int i = 0; i < sellerAgents.length; ++i) { 
                        cfp.addReceiver(sellerAgents[i]); 
                    } 
                    cfp.setContent(targetBookTitle); 
                    cfp.setConversationId("book-trade"); 
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); 
                    myAgent.send(cfp); 
                    // подготовим шаблон для получения цен на книги 
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith())); 
                    step = 1; 
                    break; 
                case 1: 
                    // получим ответы с ценами, либо отказами, если книги нет 
                    ACLMessage reply = myAgent.receive(mt); 
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            int price = Integer.parseInt(reply.getContent()); 
                            if (bestSeller == null || price < bestPrice) { 
                                // это лучшее предложение 
                                bestPrice = price; 
                                bestSeller = reply.getSender(); 
                            } 
                        } 
                        repliesCnt++; 
                        if (repliesCnt >= sellerAgents.length) { 
                            // приняты ответы от всех продавцов 
                            step = 2; 
                        }
                    } else { 
                        block(); 
                    } 
                    break; 
                case 2: 
                    // отправим запрос на покупку агенту, предложившему лучшую цену 
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL); 
                    order.addReceiver(bestSeller); 
                    order.setContent(targetBookTitle); 
                    order.setConversationId("book-trade"); 
                    order.setReplyWith("order" + System.currentTimeMillis()); 
                    myAgent.send(order); 
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith())); 
                    step = 3; 
                    break; 
                case 3: 
                    reply = myAgent.receive(mt); 
                    if (reply != null) {
                        // принят ответ на предложение покупки 
                        if (reply.getPerformative() == ACLMessage.INFORM) { 
                            // покупка совершена 
                            System.out.println(targetBookTitle + " successfully purchased book from " + bestSeller.getName());
                            System.out.println("Best price = " + bestPrice);
                            myAgent.doDelete(); 
                        } 
                        step = 4; 
                    } else {
                        block(); 
                    }
                    break;
            }
        }
        
        public boolean done() { 
            return ((step == 2 && bestSeller == null) || step == 4); 
        } 
    }
}