import com.goebl.simplify.Simplify;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.rogach.jopenvoronoi.*;

public class ZKTerrainAnalyzer {
    // given a bitmap (a walkability map in our context) it returns the external contour of obstacles
    BufferedImage bitMap;
    ArrayList<Contour> fullContours;
    ArrayList<Contour> simplifiedContours;
    VoronoiDiagram voronoid;
    Vertex[] errorEdge;
    Vertex[][] borders;
    int[][] labelMap;

    ZKTerrainAnalyzer(BufferedImage img){
        int width = img.getWidth();
        int height = img.getHeight();
        bitMap = img;
        labelMap = new int[height][width];
        fullContours = traceContours();
        simplifiedContours = simplifyContours();
        voronoid = generateVoronoi((ArrayList<Contour>)simplifiedContours.clone());
    }

    public VoronoiDiagram getVoronoid(){
        return voronoid;
    }

    public ArrayList<Contour> getContours(){
        return fullContours;
    }

    public ArrayList<Contour> getSimplifiedContours(){
        return simplifiedContours;
    }

    int clamp(int value, int min, int max) {
        return value;
        //return Math.min(Math.max(value, min), max);
    }

    private VoronoiDiagram generateVoronoi(ArrayList<Contour>obstacles){
        long timeStarted = System.currentTimeMillis();

        VoronoiDiagram vd = new VoronoiDiagram();

        int w = bitMap.getWidth() - 1;
        int h = bitMap.getHeight() - 1;

        double width = (double)bitMap.getWidth();;
        double height = (double)bitMap.getHeight();
        double radius = Math.sqrt(width*width + height*height)/2;

        Contour boundingBox = new Contour();
        boundingBox.addPoint(1,1);
        boundingBox.addPoint(w-1,1);
        boundingBox.addPoint(w-1,h-1);
        boundingBox.addPoint(1,h-1);

        ArrayList<Vertex> allVertices = new ArrayList<>();
        Vertex[][] contourVertices = new Vertex[obstacles.size()][];
        Iterator<Contour> i = obstacles.iterator();

        int v = 0;
        // add vertices for detected contours
        while (i.hasNext()) {
            Point[] pts = i.next().getPointsArray();
            Vertex[] vertices = new Vertex[pts.length-1];

            // jopenvoronoi requires all points to be within a unit circle centered at zero
            // cast and add all vertices; skip last vertex of each contour
            for(int j=0;j < pts.length-1;j++){
                double x = ((double)pts[j].x - width/2)/radius;
                double y = ((double)pts[j].y - height/2)/radius;
                vertices[j] = vd.insert_point_site(new org.rogach.jopenvoronoi.Point(x,y));
                allVertices.add(vertices[j]);
            }
            contourVertices[v] = vertices;
            v++;
        }

        // add vertices for corners
        for(Point p:boundingBox.getPoints()){
            double x = ((double)p.x - width/2)/radius;
            double y = ((double)p.y - height/2)/radius;
            allVertices.add(vd.insert_point_site(new org.rogach.jopenvoronoi.Point(x,y)));
        }

        // define borders
        borders = new Vertex[4][];
        double tolerance = 0.002;
        borders[0] = getHorizontalBorder(vd,allVertices,-height/2/radius,tolerance); // north
        borders[1] = getHorizontalBorder(vd,allVertices,+height/2/radius,tolerance); // south
        borders[2] = getVerticalBorder(vd,allVertices,+width/2/radius,tolerance); // east
        borders[3] = getVerticalBorder(vd,allVertices,-width/2/radius,tolerance); // west

        // assign segments
        for(int vi=0;vi < contourVertices.length;vi++) {
            for (int j = 0; j < contourVertices[vi].length; j++) {
                int k = j + 1 < contourVertices[vi].length ? j + 1 : 0;
                // check if these two vertices were on a border, and thus already linked
                boolean novel = true;
                isNovel:
                for(int l=0;l<borders.length;l++) {
                    for (int n = 1; n < borders[l].length; n++) {
                        if(borders[l][n-1] == contourVertices[vi][j] && borders[l][n] == contourVertices[vi][k]){
                            System.out.println("Skipping duplicate border edge!");
                            novel = false;
                            break isNovel;
                        }
                        if(borders[l][n] == contourVertices[vi][j] && borders[l][n-1] == contourVertices[vi][k]){
                            System.out.println("Skipping duplicate border edge!");
                            novel = false;
                            break isNovel;
                        }
                    }
                }

                if(novel) {
                    try {
                        vd.insert_line_site(contourVertices[vi][j], contourVertices[vi][k]);
                    } catch (Exception e) {
                        this.errorEdge = new Vertex[2];
                        errorEdge[0] = contourVertices[vi][j];
                        errorEdge[1] = contourVertices[vi][k];
                        return vd;
                    }
                }
            }
        }

        // assign borders
        for(int l=0;l<borders.length;l++) {
            for (int n = 1; n < borders[l].length; n++) {
                try {
                    vd.insert_line_site(borders[l][n - 1], borders[l][n]);
                } catch (Exception e) {
                    this.errorEdge = new Vertex[2];
                    errorEdge[0] = borders[l][n - 1];
                    errorEdge[1] = borders[l][n];
                    return vd;
                }
            }
        }




        System.out.println("Time spent generating voronoi: "+(System.currentTimeMillis() - timeStarted) + "ms");

        return vd;
    }

    class SortByX implements Comparator<Vertex>
    {
        public int compare(Vertex a, Vertex b)
        {
            return (int)Math.signum(a.position.x - b.position.x);
        }
    }

    class SortByY implements Comparator<Vertex>
    {
        public int compare(Vertex a, Vertex b)
        {
            return (int)Math.signum(a.position.y - b.position.y);
        }
    }

    // link vertices within a horizontal line with edges
    private Vertex[] getHorizontalBorder(VoronoiDiagram vor, ArrayList<Vertex> vertices, double height, double tolerance){
        ArrayList<Vertex> inTheWay = new ArrayList<>();
        for(Vertex v:vertices){
            if(Math.abs(v.position.y-height) < tolerance){
                inTheWay.add(v);
            }
        }
        System.out.println("Found "+inTheWay.size()+" vertices on horizontal border at "+height);
        inTheWay.sort(new SortByX());
        return inTheWay.toArray(new Vertex[inTheWay.size()]);
    }

    // link vertices within a vertical line with edges
    private Vertex[] getVerticalBorder(VoronoiDiagram vor, ArrayList<Vertex> vertices, double width, double tolerance){
        ArrayList<Vertex> inTheWay = new ArrayList<>();
        for(Vertex v:vertices){
            if(Math.abs(v.position.x-width) < tolerance){
                inTheWay.add(v);
            }
        }
        System.out.println("Found "+inTheWay.size()+" vertices on vertical border at "+width);
        inTheWay.sort(new SortByY());
        return inTheWay.toArray(new Vertex[inTheWay.size()]);
    }

    private ArrayList<Contour> simplifyContours(){
        long timeStarted = System.currentTimeMillis();
        ArrayList<Contour> simple = new ArrayList<Contour>();
        Iterator<Contour> i = fullContours.iterator();
        while (i.hasNext()) {
            Contour simplifiedContour = simplifyContour(i.next());
            if(simplifiedContour.length() > 3){
                simple.add(simplifiedContour);
            }
        }
        System.out.println("Total time spent simplifying: "+(System.currentTimeMillis() - timeStarted) + "ms");
        return simple;
    }

    private Contour simplifyContour(Contour c) {
        Simplify<Point> simplify = new Simplify<Point>(new Point[0]);
        Point[] allPoints = c.getPointsArray();
        Point[] lessPoints = simplify.simplify(allPoints, 5, true);

        return new Contour(lessPoints);

    }

    private ArrayList<Contour> traceContours()
    {
        long timeStarted = System.currentTimeMillis();
        int cy, cx, tracingDirection, connectedComponentsCount = 0, labelId = 0;
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        ArrayList<Contour> holes = new ArrayList<Contour>();
        ArrayList<Contour> contours = new ArrayList<Contour>();

        for (cx = 0; cx < width; ++cx) {
            for (cy = 0, labelId = 0; cy < height; ++cy) {
                Color c = new Color(bitMap.getRGB(cx, cy));

                if (c.getRed() > 0) { // is obstacle
                    if (labelId != 0) { // use pre-pixel label
                        labelMap[cx][cy] = labelId;
                    } else {
                        labelId = labelMap[cx][cy];

                        if (labelId == 0) {
                            labelId = ++connectedComponentsCount;
                            tracingDirection = 0;
                            // external contour
                            Contour obstacle  = traceContour(cx, cy, labelId, tracingDirection);
                            contours.add(obstacle);
                            labelMap[cx][cy] = labelId;
                        }
                    }
                } else if (labelId != 0) { // walkable & pre-pixel has been labeled
                    if (labelMap[cx][cy] == 0) {
                        tracingDirection = 1;
                        // internal contour
                        Contour hole = traceContour(cx, cy - 1, labelId, tracingDirection);
                        /*
                        BoostPolygon polygon;
                        boost::geometry::assign_points(polygon, hole);
                        // if polygon isn't too small, add it to the result
                        if (boost::geometry::area(polygon) > MIN_ARE_POLYGON) {
                            // TODO a polygon can have walkable polygons as "holes", save them
                            LOG(" - [WARNING] Found big walkable HOLE");
                        } else {
                            // "remove" the hole filling it with the polygon label
                            holesToLabel.emplace_back(hole, labelId);
                        }*/

                    }
                    labelId = 0;
                }
            }
        }
        System.out.println("Total time spent tracing: "+(System.currentTimeMillis() - timeStarted) + "ms");
        return contours;
    }

    Contour traceContour(int cx, int cy, int labelId, int tracingDirection)
    {
        boolean tracingStopFlag = false, keepSearching = true;
        int fx, fy, sx = cx, sy = cy;
        Contour contourPoints = new Contour();
        TracePoint p = tracer(cx, cy, tracingDirection);

        contourPoints.addPoint(p);


        if (p.x != sx || p.y != sy) {
            fx = p.x; // final/initial points
            fy = p.y;

            while (keepSearching) {
                labelMap[p.x][p.y] = labelId;
                p = tracer(p.x, p.y, (p.traceDirection + 6) % 8);

                contourPoints.addPoint(p);

                if (p.x == sx && p.y == sy) {
                    tracingStopFlag = true;
                } else if (tracingStopFlag) {
                    if (p.x == fx && p.y == fy) {
                        keepSearching = false;
                    } else {
                        tracingStopFlag = false;
                    }
                }
            }
        }

        return contourPoints;
    }

    static int[][] searchDirection = { { 1, 0 }, { 1, 1 }, { 0 , 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 } };

    TracePoint tracer(TracePoint tp){
        return tracer(tp.x,tp.y,(tp.traceDirection + 6) % 8);
    }

    TracePoint tracer(int cx, int cy, int tracingdirection)
    {
        int i, y, x;
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();

        for (i = 0; i < 7; ++i) {

            y = cy + searchDirection[tracingdirection][0];
            x = cx + searchDirection[tracingdirection][1];

            // filter invalid position (out of range)
            if (x < 0 || x >= width || y < 0 || y >= height) {
                tracingdirection = (tracingdirection + 1) % 8;
                continue;
            }
            Color c = new Color(bitMap.getRGB(x,y));

            if (c.getRed() > 0) {
                return new TracePoint(x,y,tracingdirection);
            } else {
                // not an obstacle, label as such
                labelMap[x][y] = -1;
                tracingdirection = (tracingdirection + 1) % 8;
            }
        }
        return new TracePoint(cx,cy,tracingdirection);
    }
}
