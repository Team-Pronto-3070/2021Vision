// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.vision.VisionPipeline;
import edu.wpi.first.vision.VisionThread;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/*
   JSON format:
   {
       "team": <team number>,
       "ntmode": <"client" or "server", "client" if unspecified>
       "cameras": [
           {
               "name": <camera name>
               "path": <path, e.g. "/dev/video0">
               "pixel format": <"MJPEG", "YUYV", etc>   // optional
               "width": <video mode width>              // optional
               "height": <video mode height>            // optional
               "fps": <video mode fps>                  // optional
               "brightness": <percentage brightness>    // optional
               "white balance": <"auto", "hold", value> // optional
               "exposure": <"auto", "hold", value>      // optional
               "properties": [                          // optional
                   {
                       "name": <property name>
                       "value": <property value>
                   }
               ],
               "stream": {                              // optional
                   "properties": [
                       {
                           "name": <stream property name>
                           "value": <stream property value>
                       }
                   ]
               }
           }
       ]
       "switched cameras": [
           {
               "name": <virtual camera name>
               "key": <network table key used for selection>
               // if NT value is a string, it's treated as a name
               // if NT value is a double, it's treated as an integer index
           }
       ]
   }
 */

public final class Main {
  private static String configFile = "/boot/frc.json";

  @SuppressWarnings("MemberName")
  public static class CameraConfig {
    public String name;
    public String path;
    public JsonObject config;
    public JsonElement streamConfig;
  }

  @SuppressWarnings("MemberName")
  public static class SwitchedCameraConfig {
    public String name;
    public String key;
  };

  public static int team;
  public static boolean server;
  public static List<CameraConfig> cameraConfigs = new ArrayList<>();
  public static List<SwitchedCameraConfig> switchedCameraConfigs = new ArrayList<>();
  public static List<VideoSource> cameras = new ArrayList<>();

  private Main() {
  }


  /**
   * This is the pathing section!! I know it's excessibe; I'll admit, I just wanted to see how enums worked.
   */
  //Formatted x, y, area, pointTolerance (optional) (add as many points as you want though they 
  //need to be in the same order as the camera point list)

  static final PixelPoint[] ARED_POINTS = {
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0)
  };
  static final PixelPoint[] ABLUE_POINTS = {
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0)
  };
  static final PixelPoint[] BRED_POINTS = {
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0)
  };
  static final PixelPoint[] BBLUE_POINTS = {
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0),
    new PixelPoint(0.0, 0.0, 0.0)
  };

  //Create profile objects for the points lists (can compare objects to get match value)
  public static final PixelProfile ARED_PROFILE = new PixelProfile(ARED_POINTS, "aRed");
  public static final PixelProfile ABLUE_PROFILE = new PixelProfile(ABLUE_POINTS, "aBlue");
  public static final PixelProfile BRED_PROFILE = new PixelProfile(BRED_POINTS, "bRed");
  public static final PixelProfile BBLUE_PROFILE = new PixelProfile(BBLUE_POINTS, "bBlue");
  
  //List of preset profiles to compare too
  static PixelProfile[] profiles = {ARED_PROFILE, ABLUE_PROFILE,BRED_PROFILE,BBLUE_PROFILE};

  /**
   * This method takes a network table and a list of points to compare to. 
   * It will match the closest matching profile and return the chosen path in the network table. 
   * @param table
   * @param points
   */
  public static void choosePath(ArrayList<PixelPoint> points){
    PixelProfile visibleProfile = new PixelProfile((PixelPoint[]) points.toArray(), "none");

    NetworkTableEntry entry = NetworkTableInstance.getDefault().getTable("vision").getEntry("galacticSearchPath");

    String chosenPath = visibleProfile.match(profiles);
    entry.setString(chosenPath);
    
    if(chosenPath.equals("none")){
      System.out.println("No path chosen in java-multiCameraServer/Main.java: Main.choosePath()");
    }
  }

  /**
   * Report parse error.
   */
  public static void parseError(String str) {
    System.err.println("config error in '" + configFile + "': " + str);
  }

  /**
   * Read single camera configuration.
   */
  public static boolean readCameraConfig(JsonObject config) {
    CameraConfig cam = new CameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement pathElement = config.get("path");
    if (pathElement == null) {
      parseError("camera '" + cam.name + "': could not read path");
      return false;
    }
    cam.path = pathElement.getAsString();

    // stream properties
    cam.streamConfig = config.get("stream");

    cam.config = config;

    cameraConfigs.add(cam);
    return true;
  }

  /**
   * Read single switched camera configuration.
   */
  public static boolean readSwitchedCameraConfig(JsonObject config) {
    SwitchedCameraConfig cam = new SwitchedCameraConfig();

    // name
    JsonElement nameElement = config.get("name");
    if (nameElement == null) {
      parseError("could not read switched camera name");
      return false;
    }
    cam.name = nameElement.getAsString();

    // path
    JsonElement keyElement = config.get("key");
    if (keyElement == null) {
      parseError("switched camera '" + cam.name + "': could not read key");
      return false;
    }
    cam.key = keyElement.getAsString();

    switchedCameraConfigs.add(cam);
    return true;
  }

  /**
   * Read configuration file.
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public static boolean readConfig() {
    // parse file
    JsonElement top;
    try {
      top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
    } catch (IOException ex) {
      System.err.println("could not open '" + configFile + "': " + ex);
      return false;
    }

    // top level must be an object
    if (!top.isJsonObject()) {
      parseError("must be JSON object");
      return false;
    }
    JsonObject obj = top.getAsJsonObject();

    // team number
    JsonElement teamElement = obj.get("team");
    if (teamElement == null) {
      parseError("could not read team number");
      return false;
    }
    team = teamElement.getAsInt();

    // ntmode (optional)
    if (obj.has("ntmode")) {
      String str = obj.get("ntmode").getAsString();
      if ("client".equalsIgnoreCase(str)) {
        server = false;
      } else if ("server".equalsIgnoreCase(str)) {
        server = true;
      } else {
        parseError("could not understand ntmode value '" + str + "'");
      }
    }

    // cameras
    JsonElement camerasElement = obj.get("cameras");
    if (camerasElement == null) {
      parseError("could not read cameras");
      return false;
    }
    JsonArray cameras = camerasElement.getAsJsonArray();
    for (JsonElement camera : cameras) {
      if (!readCameraConfig(camera.getAsJsonObject())) {
        return false;
      }
    }

    if (obj.has("switched cameras")) {
      JsonArray switchedCameras = obj.get("switched cameras").getAsJsonArray();
      for (JsonElement camera : switchedCameras) {
        if (!readSwitchedCameraConfig(camera.getAsJsonObject())) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Start running the camera.
   */
  public static VideoSource startCamera(CameraConfig config) {
    System.out.println("Starting camera '" + config.name + "' on " + config.path);
    CameraServer inst = CameraServer.getInstance();
    UsbCamera camera = new UsbCamera(config.name, config.path);
    MjpegServer server = inst.startAutomaticCapture(camera);

    Gson gson = new GsonBuilder().create();

    camera.setConfigJson(gson.toJson(config.config));
    camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

    if (config.streamConfig != null) {
      server.setConfigJson(gson.toJson(config.streamConfig));
    }

    return camera;
  }

  /**
   * Start running the switched camera.
   */
  public static MjpegServer startSwitchedCamera(SwitchedCameraConfig config) {
    System.out.println("Starting switched camera '" + config.name + "' on " + config.key);
    MjpegServer server = CameraServer.getInstance().addSwitchedCamera(config.name);

    NetworkTableInstance.getDefault()
        .getEntry(config.key)
        .addListener(event -> {
              if (event.value.isDouble()) {
                int i = (int) event.value.getDouble();
                if (i >= 0 && i < cameras.size()) {
                  server.setSource(cameras.get(i));
                }
              } else if (event.value.isString()) {
                String str = event.value.getString();
                for (int i = 0; i < cameraConfigs.size(); i++) {
                  if (str.equals(cameraConfigs.get(i).name)) {
                    server.setSource(cameras.get(i));
                    break;
                  }
                }
              }
            },
            EntryListenerFlags.kImmediate | EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);

    return server;
  }

  /**
   * Example pipeline.
   */
  // public static class MyPipeline implements VisionPipeline {
  //   public int val;

  //   @Override
  //   public void process(Mat mat) {
  //     val += 1;
  //   }
  // }

  /**
   * Main.
   */
  public static void main(String... args) {
    if (args.length > 0) {
      configFile = args[0];
    }

    // read configuration
    if (!readConfig()) {
      return;
    }

    // start NetworkTables
    NetworkTableInstance ntinst = NetworkTableInstance.getDefault();
    if (server) {
      System.out.println("Setting up NetworkTables server");
      ntinst.startServer();
    } else {
      System.out.println("Setting up NetworkTables client for team " + team);
      ntinst.startClientTeam(team);
      ntinst.startDSClient();
    }

    // start cameras
    for (CameraConfig config : cameraConfigs) {
      cameras.add(startCamera(config));
    }

    // start switched cameras
    for (SwitchedCameraConfig config : switchedCameraConfigs) {
      startSwitchedCamera(config);
    }

    // start image processing on camera 0 if present
    if (cameras.size() >= 1) {
      new Thread(() -> {
        //NetworkTable table = NetworkTableInstance.getDefault().getTable("GripVisionData");
        //NetworkTableEntry valid = table.getEntry("Valid");
        
        //Creates and intitalizes cameras 
        CvSink videoIn = CameraServer.getInstance().getVideo();
        CameraServer.getInstance().addServer("Outline");
        // CameraServer.getInstance()
        CameraServer.getInstance().addServer("Default");
        // CameraServer.getInstance().
        CvSource outputStream = CameraServer.getInstance().putVideo("Default", 640, 480);
        CvSource outputStreamOutline = CameraServer.getInstance().putVideo("Outline", 640, 480);

        

        Mat source = new Mat();
        Mat output = new Mat();

        int thickness = 8;

        Scalar color = new Scalar(0, 0, 255);
        //Creates Grip pipeline
        GripPipeline p = new GripPipeline();

        while (!Thread.interrupted()) {
          if (videoIn.grabFrame(source) == 0) {
            continue;
          }

          p.process(source);

          //Imgproc.rectangle(source, p.startingPoint, p.oppositePoint, color, thickness);
          // SsImgproc.cvtColor(source, output, Imgproc.boundingRect(array));
          // Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2YCrCb);
          outputStream.putFrame(source);
          outputStreamOutline.putFrame(source);
        }

        choosePath(p.cargoPoints);
      });

    

      VisionThread visionThread = new VisionThread(cameras.get(0), new GripPipeline(), pipeline -> {
      
      // VisionThread visionThread = new VisionThread(cameras.get(0),
      //         new MyPipeline(), pipeline -> {
      //   // do something with pipeline results
      });
      /* something like this for GRIP:
      VisionThread visionThread = new VisionThread(cameras.get(0),
              new GripPipeline(), pipeline -> {
        ...
      });
       */
      visionThread.start();


    }


    

    // loop forever
    for (;;) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
        return;
      }
    }


  }
}
