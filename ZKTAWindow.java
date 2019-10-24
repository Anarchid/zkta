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
            // rescale points back to heightmap scale
            double width = img.getWidth();
            double height = img.getHeight();
            double radius = Math.sqrt(width*width+height*height)/2;
            
            int edges = 0;
            bg.setColor(Color.BLUE);
            for (Edge e : g.edges) {
                if (e.valid) {
                    edges++;
                    int x1 = (int)Math.round(e.source.position.x*radius + width/2);
                    int y1 = (int)Math.round(e.source.position.y*radius + height/2);
                    int x2 = (int)Math.round(e.target.position.x*radius + width/2);
                    int y2 = (int)Math.round(e.target.position.y*radius + height/2);
                    bg.drawLine(x1,y1,x2,y2);
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
                int x1 = (int)Math.round(p1.x*radius + width/2);
                int y1 = (int)Math.round(p1.y*radius + height/2);
                int x2 = (int)Math.round(p2.x*radius + width/2);
                int y2 = (int)Math.round(p2.y*radius + height/2);
                bg.drawLine(x1,y1,x2,y2);
                System.out.println("Voronoi errored on edge {<"+p1+">,<"+p2+">}");
            }

            bg.setColor(Color.RED);
            for (Vertex[] va:zkta.borders){
                for(Vertex v:va) {
                    org.rogach.jopenvoronoi.Point p = v.position;
                    int x = (int) Math.round(p.x * radius + width / 2);
                    int y = (int) Math.round(p.y * radius + height / 2);
                    bg.fillOval(x - 3, y - 3, 6, 6);
                }
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
