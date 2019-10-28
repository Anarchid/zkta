public class LabeledContour extends Contour {
    public int label;

    LabeledContour(int label){
        super();
        this.label = label;
    }

    LabeledContour(Contour c, int label){
        this.points = c.getPoints();
        this.label = label;
    }
}
