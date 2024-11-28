package bookstore;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BookSellerGUI extends JFrame {
    BookSellerAgent agent;
    
    public BookSellerGUI(BookSellerAgent new_agent) {
        super(new_agent.getName());
        agent = new_agent;
        setBounds(100,100,300,160);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowListener() {
            public void windowActivated(WindowEvent event) {}
            public void windowClosed(WindowEvent event) {}
            public void windowDeactivated(WindowEvent event) {}
            public void windowDeiconified(WindowEvent event) {}
            public void windowIconified(WindowEvent event) {}
            public void windowOpened(WindowEvent event) {}
            public void windowClosing(WindowEvent event) {
                agent.doDelete();
            }
        });
        
        JLabel titleLabel = new JLabel("Book title:");
        JTextField titleField = new JTextField(30);
        JTextField priceField = new JTextField(30);
        JLabel priceLabel = new JLabel("Price:");
        JButton addButton = new JButton("Add");
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel canvas = new JPanel();
        titlePanel.add(titleLabel);
        titlePanel.add(titleField);
        pricePanel.add(priceLabel);
        pricePanel.add(priceField);
        canvas.setLayout(new BoxLayout(canvas, BoxLayout.Y_AXIS));
        canvas.add(titlePanel);
        canvas.add(pricePanel);
        canvas.add(addButton);
        add(canvas);
        //pack();
        
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                agent.updateCatalogue(titleField.getText(), Integer.parseInt(priceField.getText()));
                titleField.setText("");
                priceField.setText("");
            }
        });
    }
}
