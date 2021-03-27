/**
 * This class is a collection of PixelPoint objects that adds comparation functionality;
 */

public class PixelProfile {
    
    //Enum name for this profile
    Main.Path self;    

    //List of points this class contains
    public final PixelPoint[] points;

    public PixelProfile(PixelPoint[] points, Main.Path self){
        this.points = points;
        this.self = self;
    }

    /**
     * This method takes in a list of PixelProfiles and returns the name of the closest match
     * @param otherProfiles
     * @return
     */
    public Main.Path match(PixelProfile[] otherProfiles){
        
        //Contains the name of the closest path
        Main.Path closestPath = self;

        //Contains the certainty of the match
        double closestPathCertainty = 0;

        //Contains the lowest difference between self and another profile
        double lowestDifference = compareTo(otherProfiles[0])[0];

        //Contains the raw difference and certainty of a profile comparation
        double[] difference;

        System.out.println("Matching possible paths to current profile:");
        System.out.println("Summary (difference, certainty):");

        //For every profile in otherProfiles, compare to self and set closest match
        for(int i = 1; i < otherProfiles.length; i++){
            difference = compareTo(otherProfiles[i]);
            if(difference[0] < lowestDifference){
                lowestDifference = difference[0];
                closestPath = otherProfiles[i].self;
                closestPathCertainty = difference[1];
            }

            System.out.println(otherProfiles[i].self+": "+difference[0]+", "+difference[1]);  
        }

        //Print best match
        System.out.println("\nBest Match: "+closestPath+" difference: "+lowestDifference+" certainty: "+closestPathCertainty * 100+"%");

        return closestPath;
    }

    /**
     * This method does the heavy lifting inside of the match() method.
     * it takes in a single profile and returns the difference between them and the certainty of its result.
     * @param profile
     * @return
     */
    private double[] compareTo(PixelProfile profile){
        
        //Contains the two values to return
        double differenceSum = 0;
        double certaintyScore = 0;

        //Holds the result of the PixelPoint.getDifference() method
        double[] differenceProfile; 

        int matchingLength;
        if(points.length - profile.points.length >= 0){
            matchingLength = profile.points.length;
        }else{
            matchingLength = points.length;
        }

        //Compare each point in self to each point in the profile
        for(int i = 0; i < matchingLength; i++){
            
            //Get the raw difference
            differenceProfile = points[i].getDifference(profile.points[i]); 
            
            
            //Divide by number of datapoints (helps ignore invalid data)
            differenceSum += differenceProfile[0]/(differenceProfile[1] + 1 /*Avoid a divide by 0*/);

            //sum the certainty score (to be divided later)
            certaintyScore += differenceProfile[1];
        }

        //divides the certainty score by the number of points tested (*3 for the amount of data in each point !!NEEDS EDIT)
        certaintyScore = certaintyScore/(points.length * 3);

        return new double[]{differenceSum, certaintyScore};
    }
}
