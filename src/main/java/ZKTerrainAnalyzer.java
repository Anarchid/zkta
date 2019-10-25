import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Line;
import com.goebl.simplify.Simplify;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.rogach.jopenvoronoi.*;

public class ZKTerrainAnalyzer {
    // given a bitmap (a walkability map in our context) it returns the external contour of obstacles
    BufferedImage bitMap;
    ArrayList<Contour> fullContours;
    ArrayList<Contour> simplifiedContours;
    VoronoiDiagram voronoid;
    HalfEdgeDiagram graph;
    Vertex[] errorEdge;
    Vertex[][] borders;
    int[][] labelMap;
    RTree<Integer,Line> labelTree;
    double v_width;
    double v_height;
    double radius;
    int width;
    int height;
    ArrayList<Edge> pruned;

    ZKTerrainAnalyzer(BufferedImage img){
        width = img.getWidth();
        height = img.getHeight();
        v_width = (double)width;
        v_height = (double)height;
        radius = Math.sqrt(width*width + height*height)/2;

        bitMap = img;
        labelMap = new int[height][width];
        labelTree = RTree.create();

        fullContours = traceContours();
        simplifiedContours = simplifyContours();
        voronoid = generateVoronoi((ArrayList<Contour>)simplifiedContours.clone());
        pruned = getPrunedEdges(voronoid);
    }

    public int[][] getLabelMap(){
        return labelMap;
    }

    public int getHeight(){
        return height;
    }

    public int getWidth(){
        return width;
    }

    // probably need to convert them somehow?
    private ArrayList<Edge> getPrunedEdges(VoronoiDiagram voronoid) {
        HalfEdgeDiagram hed = voronoid.get_graph_reference();
        ArrayList<Vertex> vertices = new ArrayList<>();
        for(Vertex v:hed.vertices){
            vertices.add(v);
        }
        ArrayList<Edge> pruned = new ArrayList<>();

        for(Vertex v: vertices){
            int[] c = coordsFromVoronoi(v);
            // remove vertex if inside obstacle or on the border
            c[0] = snap(snap(c[0],0,5),width,5);
            c[1] = snap(snap(c[1],0,5),width,5);

            if(c[0] < 0 || c[0] >= width || c[1]<0 || c[1] >= height) {
                pruned.addAll(v.out_edges);
                pruned.addAll(v.in_edges);
            }
            else if(labelMap[c[0]][c[1]] != 0){
                pruned.addAll(v.out_edges);
                pruned.addAll(v.in_edges);
            }
        }
        System.out.println(pruned.size()+" voronoi edges pruned");
        return pruned;
    }

    public HalfEdgeDiagram getGraph(){
        return graph;
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

    int snap(int value, int attractor, int tolerance){
        return Math.abs(value-attractor)<tolerance?attractor:value;
    }

    double[] coordsToVoronoi(int x, int y){
        double vx = ((double)x - v_width/2)/radius;
        double vy = ((double)y - v_height/2)/radius;
        return new double[]{vx, vy};
    }

    int[] coordsFromVoronoi(Vertex v){
        return coordsFromVoronoi(v.position);
    }

    int[] coordsFromVoronoi(org.rogach.jopenvoronoi.Point p){
        return coordsFromVoronoi(p.x,p.y);
    }

    int[] coordsFromVoronoi(double x, double y){
        int ix = (int)Math.round(x*radius + v_width/2);
        int iy = (int)Math.round(y*radius + v_height/2);
        return new int[]{ix,iy};
    }

    private VoronoiDiagram generateVoronoi(ArrayList<Contour>obstacles){
        long timeStarted = System.currentTimeMillis();
        VoronoiDiagram vd = new VoronoiDiagram();

        Point[] boundingBox = new Point[4];
        boundingBox[0] = new Point(1,1);
        boundingBox[1] = new Point(width-1,1);
        boundingBox[2] = new Point(width-1,height-1);
        boundingBox[3] = new Point(1,height-1);

        Vertex[] cornerVertices = new Vertex[4];

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
                // snap to border because of voodoo:
                // first, bwta2 does this for some reason
                // second, skipping this causes NPE in voronoi
                int px = snap(snap(pts[j].x, width,5),1,5);
                int py = snap(snap(pts[j].y, height,5),1,5);
                double[] vc = coordsToVoronoi(px,py);

                vertices[j] = vd.insert_point_site(new org.rogach.jopenvoronoi.Point(vc[0],vc[1]));
                allVertices.add(vertices[j]);
            }
            contourVertices[v] = vertices;
            v++;
        }

        // add vertices for corners
        for(int z = 0; z < boundingBox.length;z++){
            Point p = boundingBox[z];
            double[] vc = coordsToVoronoi(p.x,p.y);
            Vertex cv = vd.insert_point_site(new org.rogach.jopenvoronoi.Point(vc[0],vc[1]));
            cornerVertices[z] = cv;
        }

        // define borders
        borders = new Vertex[4][];
        double tolerance = 0.005;
        borders[0] = getHorizontalBorder(allVertices,cornerVertices[0],cornerVertices[1],tolerance); // north
        borders[1] = getHorizontalBorder(allVertices,cornerVertices[2],cornerVertices[3],tolerance); // south
        borders[2] = getVerticalBorder(allVertices,cornerVertices[1],cornerVertices[2],tolerance); // east
        borders[3] = getVerticalBorder(allVertices,cornerVertices[0],cornerVertices[3],tolerance); // west

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
                            novel = false;
                            break isNovel;
                        }
                        if(borders[l][n] == contourVertices[vi][j] && borders[l][n-1] == contourVertices[vi][k]){
                            novel = false;
                            break isNovel;
                        }
                    }
                }

                if(novel) {
                    Vertex v1 = contourVertices[vi][j];
                    Vertex v2 = contourVertices[vi][k];
                    try {
                        vd.insert_line_site(v1, v2);
                    } catch (Exception e) {
                        this.errorEdge = new Vertex[2];
                        errorEdge[0] = contourVertices[vi][j];
                        errorEdge[1] = contourVertices[vi][k];
                        return vd;
                    }
                    // this should map the label of the closest obstacle, but pruning and relabeling too small
                    // components so far has been skipped, so the indices are unreliable!
                    labelTree = labelTree.add(vi, Geometries.line(v1.position.x,v1.position.y,v2.position.x,v2.position.y));
                }
            }
        }

        // assign borders
        for(int l=0;l<borders.length;l++) {
            for (int n = 1; n < borders[l].length; n++) {
                Vertex v1 = borders[l][n - 1];
                Vertex v2 = borders[l][n];
                try {
                    vd.insert_line_site(v1, v2);
                } catch (Exception e) {
                    e.printStackTrace();
                    this.errorEdge = new Vertex[2];
                    errorEdge[0] = borders[l][n - 1];
                    errorEdge[1] = borders[l][n];
                    return vd;
                }
                // -1 will meant to us that the obstacle is edge of map
                labelTree = labelTree.add(-1, Geometries.line(v1.position.x,v1.position.y,v2.position.x,v2.position.y));
            }
        }

        System.out.println("Generated voronoi and r-tree in "+(System.currentTimeMillis() - timeStarted) + "ms");

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
    private Vertex[] getHorizontalBorder(ArrayList<Vertex> vertices,Vertex left, Vertex right, double tolerance){
        ArrayList<Vertex> inTheWay = new ArrayList<>();

        double height = (left.position.y+right.position.y)/2;

        inTheWay.add(left);
        inTheWay.add(right);

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
    private Vertex[] getVerticalBorder(ArrayList<Vertex> vertices, Vertex top, Vertex bottom, double tolerance){
        ArrayList<Vertex> inTheWay = new ArrayList<>();
        double width = (top.position.x+bottom.position.x)/2;
        inTheWay.add(top);
        inTheWay.add(bottom);
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
