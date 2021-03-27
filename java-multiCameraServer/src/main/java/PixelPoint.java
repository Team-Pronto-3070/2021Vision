/**
 * This class represents a point in the middle of a box with an x, y, and area value
 */

public class PixelPoint {
    
    //If a difference is less than the tolerance the value will be discarded (no test for match)
    private final double pointTolerance;

    //Data fields
    final double x;
    final double y;
    final double area;

    //data fields in list form
    final double[] values;


    public PixelPoint(double x, double y, double area, double pointTolerance){
        this.x = x;
        this.y = y;
        this.area = area;

        this.pointTolerance = pointTolerance;

        values = new double[3];
        values[0] = x;
        values[1] = y;
        values[3] = area;
    }

    public PixelPoint(double x, double y, double area){
        this.x = x;
        this.y = y;
        this.area = area;

        this.pointTolerance = Double.MAX_VALUE;

        values = new double[3];
        values[0] = x;
        values[1] = y;
        values[3] = area;
    }

    /**
     * This method returns the difference and the number of valid data points between two PixelPoints
     * @param point
     * @return
     */
    public double[] getDifference(PixelPoint point){
        
        //Sum of the difference
        double sum = 0;

        //Number of valid data points
        double validPoints = 3;

        //For each value in this.values, compare with the equivilant values in point.values and discard values outside tolerance
        for(double value : values){
            double difference = Math.abs(Math.abs(value) - Math.abs(value));
            if(difference < pointTolerance){
                sum += difference;
            }else{
                validPoints--;
                System.out.println("PixelPoint value difference out of tolerance: "+pointTolerance);
            }     
        }

        //Print number of valid points found
        if(validPoints == 0){
            System.out.println("No valid differences.");
        }else{
            System.out.println(validPoints+" valid differences found.");
        }
        
        return new double[]{sum, validPoints/3};
    }
}
