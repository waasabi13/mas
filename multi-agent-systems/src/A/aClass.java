package A;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import java.lang.*;  // Импортируем все классы из пакета java.lang

public class aClass extends Agent {
    // Метод setup для инициализации агента
    public void setup() {
        System.out.println("Привет! агент " + getAID().getName() + " готов.");  // Сообщение о запуске агента
        // Добавляем циклическое поведение для получения сообщений
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();  // Получение сообщения, если оно есть
                if (msg != null) {
                    System.out.println(" – " + myAgent.getLocalName() + " received: " + msg.getContent());  // Вывод содержимого сообщения
                }
                block();  // Блокировка поведения до появления нового сообщения
            }
        });

        // Переменная для хранения описания агентов
        AMSAgentDescription[] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults(Long.valueOf(-1));
            agents = AMSService.search(this, new AMSAgentDescription(), c);  // Поиск агентов через AMSService
        } catch (Exception e) {
            System.out.println("Problem searching AMS: " + e);  // Обработка ошибок поиска агентов
            e.printStackTrace();
        }
        for (int i = 0; i < agents.length; i++) {
            System.out.println(agents[i].getName());
        }
        // Отправляем сообщение агентам
        for (int i = 0; i < agents.length; i++) {
            AID agentID = agents[i].getName();  // Получение ID агента
            if (agentID.getName().equals(this.getName())) {continue;}
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);  // Создание сообщения типа INFORM
            System.out.println(msg.getContent());
            msg.addReceiver(agentID);  // Указываем получателя
            msg.setLanguage("English");  // Указываем язык сообщения
            msg.setContent("Ping");  // Устанавливаем содержимое сообщения
            send(msg);  // Отправляем сообщение
        }
    }
}

