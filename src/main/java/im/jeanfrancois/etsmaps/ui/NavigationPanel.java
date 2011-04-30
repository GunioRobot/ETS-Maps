package im.jeanfrancois.etsmaps.ui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;
import com.google.inject.Inject;
import im.jeanfrancois.etsmaps.model.Landmark;
import im.jeanfrancois.etsmaps.model.Leg;
import im.jeanfrancois.etsmaps.model.NavigableMap;
import im.jeanfrancois.etsmaps.model.Route;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;


/**
 * Navigation panel that contains all the UI controls to search for
 * landmarks and navigate between them.
 *
 * @author jfim
 */
public class NavigationPanel extends JPanel {
    private EventList<Leg> routeLegs = new BasicEventList<Leg>();
    private NavigableMap map;
    private MapDisplayComponent mapDisplayComponent;
    private JComboBox originComboBox;
    private JComboBox destinationComboBox;

    private BufferedImage markerA = null;
    private BufferedImage markerB = null;

    @Inject
    public NavigationPanel(NavigableMap navigableMap, final MapDisplayComponent mapDisplayComponent) {
        map = navigableMap;
        this.mapDisplayComponent = mapDisplayComponent;

        setLayout(new MigLayout("wrap 2", "[][grow, fill]", "[][][][][][grow,fill][]"));

        add(new JLabel(new ImageIcon(NavigationPanel.class.getClassLoader().getResource("etsmaps-logo.png"))), "span");

        EventList<Landmark> landmarks = new BasicEventList<Landmark>();
        landmarks.addAll(map.getLandmarks());
        EventList<Landmark> sortedLandmarks = new SortedList<Landmark>(landmarks, new Comparator<Landmark>() {
            @Override
            public int compare(Landmark o1, Landmark o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigate();
                }
            }
        };

        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigate();
            }
        };

        add(new JLabel("De"));

        originComboBox = new JComboBox(new EventComboBoxModel<Landmark>(sortedLandmarks));
        add(originComboBox);
        AutoCompleteDecorator.decorate(originComboBox);
        originComboBox.getEditor().getEditorComponent().addKeyListener(keyListener);
        originComboBox.addActionListener(actionListener);

        add(new JLabel("Vers"));

        destinationComboBox = new JComboBox(new EventComboBoxModel<Landmark>(sortedLandmarks));
        add(destinationComboBox);
        AutoCompleteDecorator.decorate(destinationComboBox);
        destinationComboBox.getEditor().getEditorComponent().addKeyListener(keyListener);
        destinationComboBox.addActionListener(actionListener);

        /*final JButton button = new JButton("Naviguer");
          add(button, "span 2, alignx right");
          button.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  navigate();
              }
          }); */

        add(new JLabel("Directions"), "span 2");

        final JTable table = new JTable(new EventTableModel<Leg>(routeLegs,
                new TableFormat<Leg>() {
                    public int getColumnCount() {
                        return 2;
                    }

                    public String getColumnName(int i) {
                        if (i == 0)
                            return "Direction";
                        return "Distance";
                    }

                    public Object getColumnValue(Leg leg, int i) {
                        if (i == 0) {
                            return leg.getDescription();
                        }

                        NumberFormat formatter = new DecimalFormat("0.0");
                        return formatter.format(leg.getLengthInMetres()) + " m";
                    }
                }));
        add(new JScrollPane(table), "width 100%, span 2");

        final JCheckBox showDebugStuffCheckBox = new JCheckBox("Afficher les informations de déboguage");
        add(showDebugStuffCheckBox, "span");
        mapDisplayComponent.setDebugStuffVisible(showDebugStuffCheckBox.isSelected());

        showDebugStuffCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mapDisplayComponent.setDebugStuffVisible(showDebugStuffCheckBox.isSelected());
            }
        });

        try {
            markerA = ImageIO.read(NavigationPanel.class.getClassLoader().getResource("marker-a.png"));
            markerB = ImageIO.read(NavigationPanel.class.getClassLoader().getResource("marker-b.png"));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void navigate() {
        final Landmark origin = (Landmark) originComboBox.getSelectedItem();
        final Landmark destination = (Landmark) destinationComboBox.getSelectedItem();

        if (origin != null && destination != null) {
            Route route = map.getRouteBetweenLandmarks(origin,
                    destination);

            ArrayList<Leg> legs = new ArrayList<Leg>();

            for (int i = 0; i < route.getLegCount(); ++i) {
                legs.add(route.getLeg(i));
            }

            ReadWriteLock lock = routeLegs.getReadWriteLock();
            lock.writeLock().lock();
            routeLegs.clear();
            routeLegs.addAll(legs);
            lock.writeLock().unlock();

            mapDisplayComponent.overlayRoute(route);
        }

        mapDisplayComponent.setOrigin(origin);
        mapDisplayComponent.setDestination(destination);
    }
}
