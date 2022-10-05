import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.path.Path;
import com.acmerobotics.roadrunner.path.PathBuilder;
import com.acmerobotics.roadrunner.path.PathSegment;
import com.acmerobotics.roadrunner.path.QuinticSpline;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DrawPanel extends JPanel {

    private static final Pattern p = Pattern.compile("[+-]?(\\d*\\.)?\\d+");

    private final NodeManager nodeM;
    private Path path;

    private final JPopupMenu menu = new JPopupMenu("Menu");

    private final JMenuItem delete = new JMenuItem("Delete");
    private final JMenuItem makeDisplace = new JMenuItem("Make Displacement Marker");
    private final JMenuItem makeSpline = new JMenuItem("Make Spline");

    private final JButton exportButton = new JButton("Export");
    private final JButton importButton = new JButton("Import");
    public final JButton flipButton = new JButton("Flip");
    private final JButton clearButton = new JButton("Clear");
    private final JButton undoButton = new JButton("Undo");
    private final JButton redoButton = new JButton("Redo");
    AffineTransform tx = new AffineTransform();
    int[] xPoly = {0, -2, 0, 2};
    int[] yPoly = {0, -4, -3, -4};
    Polygon poly = new Polygon(xPoly, yPoly, xPoly.length);
    private final double SCALE;

    DrawPanel(NodeManager nodeM, NodeManager undo, NodeManager redo, Main main) {
        this.nodeM = nodeM;
        this.SCALE = main.SCALE;
        this.path = path;
        setPreferredSize(new Dimension((int) Math.floor(144 * SCALE + 4), (int) Math.floor(144 * SCALE + (30))));
        JPanel buttons = new JPanel(new GridLayout(1, 4, 1, 1));

        menu.add(delete);
        menu.add(makeDisplace);
        menu.add(makeSpline);
        add(menu);

        buttons.add(exportButton);
        buttons.add(importButton);
        buttons.add(flipButton);
        buttons.add(clearButton);
        buttons.add(undoButton);
        buttons.add(redoButton);
        add(Box.createRigidArea(new Dimension((int) Math.floor(144 * SCALE), (int) Math.floor(144 * SCALE))));
        add(buttons);

        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(nodeM.size() > 0){
                    Node node = nodeM.get(0);
                    System.out.printf("drive.trajectorySequenceBuilder(new Pose2d(%.2f, %.2f, Math.toRadians(%.2f)))%n", node.x, -node.y, (node.heading+90));
                    for (int i = 1; i < nodeM.size(); i++) {
                        node = nodeM.get(i);
                        switch (node.getType()){
                            case SPLINE:
                                System.out.printf(".splineTo(new Pose2d(%.2f, %.2f, Math.toRadians(%.2f)))%n", node.x, -node.y, (node.heading+90));
                                break;
                            case MARKER:
                                System.out.printf(".splineTo(new Pose2d(%.2f, %.2f, Math.toRadians(%.2f)))%n", node.x, -node.y, (node.heading+90));
                                System.out.println(".addDisplacementMarker(() -> {})");
                                break;
                            default:
                                System.out.println("what");
                                break;
                        }
                    }
                    System.out.println(".build()");
                }
            }
        });
        flipButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Node un = new Node(-2,-2);
                un.state = 3;
                undo.add(un);
                for (int i = 0; i < nodeM.size(); i++) {
                    Node node = nodeM.get(i);
                    node.y *= -1;
                    node.heading = 180-node.heading;
                    nodeM.set(i, node);
                }
                repaint();
            }
        });
        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                main.undo();
                repaint();
            }
        });
        redoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                main.redo();
                repaint();
            }
        });
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //todo: add undo for this
                undo.clear();
                redo.clear();
                nodeM.clear();
                repaint();
            }
        });
        importButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    File file = new File(Main.class.getResource("/import.txt").toURI());
                    Scanner reader = new Scanner(file);
                    while (reader.hasNextLine()) {
                        String line = reader.nextLine();
                        if(line.contains(".splineTo(new Vector2d(")){
                            Matcher m = p.matcher(line);
                            Node node = new Node();
                            String[] data = new String[4];
                            for (int i = 0; m.find(); i++) {
                                data[i]=m.group(0);
                            }
                            node.x = Double.parseDouble(data[1]);
                            node.y = -Double.parseDouble(data[2]);
                            node.heading = Double.parseDouble(data[3])-90;

                            nodeM.add(node);
                        } else if(line.contains(".addDisplacementMarker(")){
                            (nodeM.get(nodeM.size()-1)).setType(Node.Type.MARKER);
                        }
                    }
                } catch (URISyntaxException | FileNotFoundException uriSyntaxException) {
                    uriSyntaxException.printStackTrace();
                }
                repaint();
            }
        });
        menu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                repaint();
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
                repaint();
            }
        });

        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Node n = nodeM.get(nodeM.editIndex);
                n.index = nodeM.editIndex;
                n.state = 1;
                undo.add(n);
                nodeM.remove(nodeM.editIndex);
                repaint();
            }
        });
        makeDisplace.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nodeM.get(nodeM.editIndex).setType(Node.Type.MARKER);
            }
        });
        makeSpline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nodeM.get(nodeM.editIndex).setType(Node.Type.SPLINE);
            }
        });
    }

    Color cyan = new Color(104, 167, 157);
    Color darkPurple = new Color(105,81,121);
    Color lightPurple = new Color(147, 88, 172);

    private void renderSplines(Graphics g, Path path, Color color) {
        g.setColor(color);
        for (int i = 0; i < path.length(); i++) {
            Pose2d pose1 = path.get(i-1);
            Pose2d pose2 = path.get(i);
            g.drawLine((int) Math.floor((pose1.getX()+72)*SCALE),(int) Math.floor((pose1.getY()+72)*SCALE),(int) Math.floor((pose2.getX()+72)*SCALE),(int) Math.floor((pose2.getY()+72)*SCALE));
        }
    }

    private void renderPoints(Graphics g, Path path, Color c1, int ovalScale){
        path.getSegments().forEach(pathSegment -> {
            Pose2d mid = pathSegment.get(pathSegment.length()/2);
            g.setColor(c1);
            g.fillOval((int) Math.floor((SCALE * (mid.getX() + 72)) - (ovalScale*SCALE)), (int) Math.floor((SCALE * (mid.getY() + 72)) - (ovalScale*SCALE)), (int) Math.floor(2*ovalScale*SCALE), (int) Math.floor(2*ovalScale*SCALE));
        });
    }

    @Override
    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        g.drawImage(new ImageIcon(Main.class.getResource("/field-2022-kai-dark.png")).getImage(), 0, 0, (int) Math.floor(144 * SCALE), (int) Math.floor(144 * SCALE), null);

        if(nodeM.size() > 0) {
            java.util.List<PathSegment> segments = new ArrayList<>();

            Node node = nodeM.get(0);
            for (int i = 1; i < nodeM.size(); i++) {
                final Node prevNode = node;
                node = nodeM.get(i);
                final double derivMag = Math.hypot(node.x - prevNode.x, node.y - prevNode.y);
                final double prevHeading = Math.toRadians(-prevNode.heading - 90);
                final double heading = Math.toRadians(-node.heading - 90);
                segments.add(new PathSegment(new QuinticSpline(
                        new QuinticSpline.Knot(prevNode.x, prevNode.y, derivMag * Math.cos(prevHeading), derivMag * Math.sin(prevHeading)),
                        new QuinticSpline.Knot(node.x, node.y, derivMag * Math.cos(heading), derivMag * Math.sin(heading)),
                        0.25, 1, 4
                )));
            }
            path = new Path(segments);
            renderSplines(g, path, darkPurple);
            renderPoints(g, path, cyan, 1);
            renderArrows(g, nodeM, 1);
        }
    }

    private void renderArrows(Graphics g, NodeManager nodeM, int ovalScale) {
        for (int i = 0; i < nodeM.size(); i++) {
            Node node = nodeM.get(i);
            tx.setToIdentity();
            tx.translate((SCALE * (node.x + 72)), (SCALE * (node.y + 72)));
            tx.rotate(Math.toRadians(-node.heading+180));
            tx.scale (SCALE, SCALE);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setTransform(tx);

            g2.setColor(darkPurple);
            g2.fillOval(-ovalScale,-ovalScale, 2*ovalScale, 2*ovalScale);
            switch (node.getType()){
                case SPLINE:
                    g2.setColor(lightPurple);
                    break;
                case MARKER:
                    g2.setColor(cyan);
                    break;
            }
            g2.fill(poly);
            g2.dispose();
        }
    }

    public JPopupMenu getMenu(){
        return menu;
    }

    public Path getPath(){
        return path;
    }

}