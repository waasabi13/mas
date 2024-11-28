package B;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;


public class bClass extends Agent {
    // Метод setup вызывается при инициализации агента
    @Override
    protected void setup() {
        // Сообщение о старте агента
        System.out.println("Привет! агент " + getAID().getName() + " готов.");

        // Добавление циклического поведения для обработки сообщений
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Получаем сообщение, если оно доступно
                ACLMessage msg = receive();
                if (msg != null) {
                    // Вывод локального имени агента и содержимого полученного сообщения
                    System.out.println(" – " + myAgent.getLocalName() + " received: " + msg.getContent());

                    // Создаем ответное сообщение
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM); // Тип сообщения — INFORM
                    reply.setContent("Pong"); // Устанавливаем содержимое сообщения

                    send(reply); // Отправляем ответное сообщение
                }
                block(); // Блокировка поведения до появления нового сообщения
            }
        });
    }
}
