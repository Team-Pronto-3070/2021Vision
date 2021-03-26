public class PixelProfile {
    
    Main.Path self;    
    public final PixelPoint[] points;

    public PixelProfile(PixelPoint[] points, Main.Path self){
        this.points = points;
        this.self = self;
    }

    public Main.Path match(PixelProfile[] otherProfiles){
        
        Main.Path closestPath = self;
        double lowestDifference = compareTo(otherProfiles[0]);

        for(int i = 1; i < otherProfiles.length; i++){
            
            double difference = compareTo(otherProfiles[i]);
            if(difference < lowestDifference){
                lowestDifference = difference;
                closestPath = otherProfiles[i].self;
            }
            
        }

        return closestPath;
    }

    private double compareTo(PixelProfile profile){
        double differenceSum = 0;

        for(int i = 0; i < points.length; i++){
            differenceSum += points[i].getDifference(profile.points[i]); 
        }

        return differenceSum;
    }
}
