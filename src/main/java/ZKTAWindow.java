import org.rogach.jopenvoronoi.Edge;
import org.rogach.jopenvoronoi.HalfEdgeDiagram;
import org.rogach.jopenvoronoi.Vertex;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ZKTAWindow {
        public static void main(String args[]) {

            BufferedImage img = null;
            try {
                img = ImageIO.read(new File("trojan.png"));
            } catch (IOException e) {
                System.out.println("Terrain analyser has resigned and is now spectating");
                return;
            }

            ZKTerrainAnalyzer zkta = new ZKTerrainAnalyzer(img);
            ArrayList<Contour> contours = zkta.getContours();
            ArrayList<Contour> simple = zkta.getSimplifiedContours();

            Graphics bg = img.getGraphics();

            bg.setColor(Color.BLACK);
            bg.fillRect ( 0, 0, img.getWidth(), img.getHeight() );

            bg.setColor(Color.DARK_GRAY);
            int[][] lm = zkta.getLabelMap();

            for(int x=0; x < zkta.getWidth(); x++ ){
                for(int y=0; y < zkta.getHeight(); y++ ){
                    if(lm[x][y] != 0){
                        bg.drawLine(x,y,x,y);
                    }
                }
            }

            bg.setColor(Color.GREEN);
            int fullSizePoints = 0;

            for(int c = 0; c < contours.size();c++){
                Contour contour = contours.get(c);
                for(int p = 0; p < contour.length();p++){
                    fullSizePoints++;
                    int n = p + 1 < contour.length()? p+1 : 0;
                    Point cp = contour.getPoint(p);
                    Point np = contour.getPoint(n);
                    bg.drawLine(cp.x,cp.y,np.x,np.y);
                }
            }

            int simplifiedPoints = 0;

            HalfEdgeDiagram g = zkta.getVoronoid().get_graph_reference();
            ArrayList<Edge> pruned = zkta.pruned;

            int edges = 0;

            for (Edge e : g.edges) {
                if (e.valid) {
                    int[] v1 = zkta.coordsFromVoronoi(e.source);
                    int[] v2 = zkta.coordsFromVoronoi(e.target);

                    if(zkta.pruned.contains(e)){
                        bg.setColor(Color.LIGHT_GRAY);
                    }else{
                        bg.setColor(Color.CYAN);
                        bg.fillOval(v1[0] - 3, v1[1] - 3, 6, 6);
                        bg.fillOval(v2[0] - 3, v2[1] - 3, 6, 6);
                    }
                    edges++;
                    bg.drawLine(v1[0],v1[1],v2[0],v2[1]);
                }
            }

            bg.setColor(Color.YELLOW);
            for(int i = 0; i < simple.size();i++){
                Contour contour = simple.get(i);
                Point pp = contour.getPoint(0);
                bg.drawString(""+i,pp.x+10,pp.y+10);
                for(int p = 0; p < contour.length();p++){
                    simplifiedPoints++;
                    int n = p + 1 < contour.length()? p+1 : 0;
                    Point cp = contour.getPoint(p);
                    Point np = contour.getPoint(n);
                    bg.drawLine(cp.x,cp.y,np.x,np.y);
                }
            }

            if(zkta.errorEdge != null){
                bg.setColor(Color.RED);
                org.rogach.jopenvoronoi.Point p1 = zkta.errorEdge[0].position;
                org.rogach.jopenvoronoi.Point p2 = zkta.errorEdge[1].position;
                int[] v1 = zkta.coordsFromVoronoi(p1);
                int[] v2 = zkta.coordsFromVoronoi(p2);
                bg.drawLine(v1[0],v1[1],v2[0],v2[1]);
                System.out.println("Voronoi errored on edge {<"+p1+">,<"+p2+">}");
            }

            System.out.println("Total contour points: "+fullSizePoints);
            System.out.println("Simplified points: "+simplifiedPoints);
            System.out.println("Voronoi edges: "+edges);

            JFrame frame = new JFrame();
            ImageIcon icon = new ImageIcon(img);
            JLabel label = new JLabel(icon);
            frame.add(label);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
    }
}
