package individual_project;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class ProgrammerAgent extends Agent {
    private int experience;
    private List<ACLMessage> proposals = new ArrayList<>();
    private int hiring = 0;
    private SearchProjectsBehaviour spb = new SearchProjectsBehaviour(this, 15000);
    private int findProjects = 0; //найденные проекты
    private int step = 0;
    private ACLMessage maxProposal = null;
    private int maxCost = Integer.MIN_VALUE;

    // Метод для получения имени агента без части после @
    private String getShortName() {
        String agentName = getAID().getName();
        return agentName.substring(0, agentName.indexOf("@"));
    }

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 1) {
            experience = Integer.parseInt((String) args[0]);
            System.out.println("Программист " + getShortName() + " с опытом работы = " + experience + " готов");

            // Запуск поведения для периодического поиска проектов
            addBehaviour(spb); // Проверка проектов каждые 15 секунд

            // Поведение для обработки предложений от проектов
            addBehaviour(new ReceiveProposalsBehaviour());
        } else {
            System.out.println("Неправильные аргументы при создании программиста " + getShortName() + ". Удаление.");
            doDelete();
        }
    }

    // Поведение для поиска доступных проектов каждые 15 секунд
    private class SearchProjectsBehaviour extends TickerBehaviour {
        public SearchProjectsBehaviour(Agent a, long period) {
            super(a, period);
        }

        protected void onTick() {
            System.out.println("Программист " + getShortName() + " ищет доступные проекты...");

            // Поиск всех агентов проекта через Directory Facilitator (DF)
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("project");
            template.addServices(sd);

            try {
                // Получение списка всех агентов проекта
                DFAgentDescription[] result = DFService.search(myAgent, template);
                findProjects = result.length;
                if (result.length == 0) {
                    System.out.println("Программист " + getShortName() + " нашел 0 проектов.");
                } else {
                    for (DFAgentDescription dfAgent : result) {
                        AID projectAgent = dfAgent.getName();
                        // Отправка сообщения о наличии программиста проекту
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setContent(Integer.toString(experience));
                        msg.addReceiver(projectAgent);
                        send(msg);
                    }
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    // Поведение для получения предложений от проектов
    private class ReceiveProposalsBehaviour extends Behaviour {
        private int repliesCnt = 0;
        public void action() {
            switch (step) {

                case 0:
                    // получим ответы с ценами, либо отказами, если книги нет
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    ACLMessage proposal = receive(mt);
                    if (proposal != null) {
                        proposals.add(proposal);
                        repliesCnt++;
                        if (repliesCnt == findProjects) {
                            step = 2;
                        }
                    } else {
                        block();
                    }


                    break;
                case 2:
                    // отправим запрос на покупку агенту, предложившему лучшую цену


                    // Выбор проекта с максимальной стоимостью после получения предложения
                    for (ACLMessage p : proposals) {
                        int cost = Integer.parseInt(p.getContent());
                        if (cost > maxCost) {
                            maxCost = cost;
                            maxProposal = p;
                        }
                    }
                    step = 3;
                    break;
                case 3:
                    if (maxProposal != null) {
                        ACLMessage reply = maxProposal.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        send(reply);
                        System.out.println("Программист " + getShortName() + " выбрал проект "
                                + maxProposal.getSender().getName().substring(0, maxProposal.getSender().getName().indexOf("@"))
                                + " с оплатой " + maxCost + " рублей");
                        hiring = 1; // сотрудника наняли
                        myAgent.removeBehaviour(spb);
                        step = 4;

                    }
                    proposals.clear(); // Очистка списка предложений для следующего цикла
                    break;
            }

        }

        @Override
        public boolean done() {
            return step == 4;
        }
    }
}
