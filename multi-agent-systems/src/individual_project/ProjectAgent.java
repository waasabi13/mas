package individual_project;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class ProjectAgent extends Agent {

    private int numProgrammersNeeded; // Требуемое количество программистов
    private int requiredExperience; // Минимальный требуемый опыт
    private int projectCost; // Предлагаемая оплата за проект
    private List<AID> programmersList = new ArrayList<>(); // Список нанятых программистов
    private int flagAll = 0; // Флаг для сообщения о завершении набора программистов

    protected void setup() {
        // Инициализация параметров проекта
        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            numProgrammersNeeded = Integer.parseInt((String) args[0]);
            requiredExperience = Integer.parseInt((String) args[1]);
            projectCost = Integer.parseInt((String) args[2]);

            // Регистрация агента в Directory Facilitator (DF) для его нахождения программистами
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("project"); // Тип, по которому его будут находить ProgrammerAgent
            sd.setName("ProjectAgent");
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            // Добавление поведения для получения сообщений от программистов
            addBehaviour(new ReceiveInformBehaviour());
            addBehaviour(new ReceiveProposeBehaviour());
            System.out.println("Проект " + getShortName() + " готов. Необходимое количество людей: "
                    + numProgrammersNeeded + ". Минимальный опыт: " + requiredExperience + " лет. Стоимость проекта: " + projectCost + " рублей");
        } else {
            System.out.println("Неправильные аргументы при создании проекта " + getShortName() + ". Удаление.");
            doDelete();
        }
    }

    // Метод для получения имени агента без части после "@"
    private String getShortName() {
        String agentName = getAID().getName();
        return agentName.substring(0, agentName.indexOf("@"));
    }

    protected void takeDown() {
        // Удаление агента из DF при завершении
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Проект " + getShortName() + " завершен и удален из DF.");
    }

    // Поведение для обработки сообщений INFORM от программистов
    private class ReceiveInformBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                int programmerExperience = Integer.parseInt(msg.getContent());
                if (programmersList.size() < numProgrammersNeeded) {
                    if (programmerExperience >= requiredExperience) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(Integer.toString(projectCost)); // Отправка информации об оплате
                        send(reply);
                    } else {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REFUSE);
                        send(reply);
                    }
                } else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    send(reply);
                }
            } else {
                block();
            }
        }
    }

    // Поведение для обработки сообщений PROPOSE от программистов
    private class ReceiveProposeBehaviour extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                if (programmersList.size() < numProgrammersNeeded) {
                    programmersList.add(msg.getSender());
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    reply.setContent("Проект " + getShortName() + " принял программиста: " +
                            msg.getSender().getName().substring(0, msg.getSender().getName().indexOf("@")));
                    send(reply);

                    if (programmersList.size() == numProgrammersNeeded) {
                        System.out.println("Проект " + getShortName() + " закончил набор программистов");
                        flagAll = 1; // Обновление флага, что набор завершен
                    }
                } else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    send(reply);
                }
            } else {
                block();
            }
        }
    }
}
