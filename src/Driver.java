import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Word;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import java.util.LinkedList;
import java.util.Queue;


public class Driver {

    public static final Point CENTER_OF_SCREEN = new Point(960, 540);

    public static void main(String[] args) {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        //System.setProperty("TESSDATA_PREFIX", "C:\\Users\\timdl\\IdeaProjects\\WowBot\\src\\tessdata");
        char[] windowText = new char[512];
        User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String windowTitle = Native.toString(windowText);
            if (windowTitle.contains("World of Warcraft")) {
                User32.INSTANCE.SetForegroundWindow(hwnd);
                WinDef.RECT windowRect = new WinDef.RECT();
                User32.INSTANCE.GetWindowRect(hwnd, windowRect);

               FixedSizeQueue mostRecentDistancesQueue = new FixedSizeQueue(10);
                Point[] path = new Point[1];
                // 44.50, 91.00
                // 45.00, 89.29
                //46.00, 86.36
                // 45.31, 83.78
                //path[0] = new Point(34.00, 66.74);
                //path[1] = new Point(35.63, 66.55);
                //path[2] = new Point(36.13, 63.52);
                //path[3] = new Point(34.36, 63.06);
                path[0] = new Point(32.29, 65.44);
                // 32.29, 65.44

                Robot robot = null;
                try {
                    robot = new Robot();
                } catch (AWTException e) {
                    throw new RuntimeException(e);
                }

                //for (int x = 0; x < 20; x++) {
                    for (int i = 0; i < path.length; i++) {
                        // get x coord where screenX = 10
                        // get y coord where screenY = 50
                        // these are hardcoded
                        double currentXCoord = robot.getPixelColor(10, 0).getRed();
                        double currentXCoordDecimalPart = robot.getPixelColor(10, 0).getGreen();
                        double currentYCoord = robot.getPixelColor(50, 0).getRed();
                        double currentYCoordDecimalPart = robot.getPixelColor(50, 0).getGreen();
                        currentXCoord += (currentXCoordDecimalPart / 100);
                        currentYCoord += (currentYCoordDecimalPart / 100);
                        int currentFacing = robot.getPixelColor(50, 0).getBlue();
                        int inCombat = robot.getPixelColor(80, 0).getRed();
                        System.out.println("inCombat: " + inCombat);

                        System.out.println("Current (X,Y): " + "(" + currentXCoord + "," + currentYCoord + ")");
                        System.out.println(currentFacing); // in range from 0-255 where 0 is true north
                        double degrees = (currentFacing / 255.0) * 360.0;
                        double radians = Math.toRadians(degrees);
                        System.out.println(degrees);
                        System.out.println(radians);
                        currentFacing = robot.getPixelColor(50, 0).getBlue();
                        degrees = (currentFacing / 255.0) * 360.0;
                        radians = Math.toRadians(degrees);

                        double distance = distance(currentXCoord, currentYCoord, path[i].x, path[i].y);
                        System.out.println("Distance: " + distance);
                        double slope = calculateWowDirection(currentXCoord, currentYCoord, path[i].x, path[i].y);
                        System.out.println("Slope: " + slope);

                        // Determines how much our character will be turning based on our direction as well as the where the next point is
                        double directionDiff = slope - radians;
                        directionDiff = shortenDirectionDiff(directionDiff);
                        System.out.println("Direction diff: " + directionDiff);
                        double turnTime = (Math.toDegrees(directionDiff) / 360) * 2000;
                        System.out.println("Turn time: " + turnTime);

                        // If turnTime is positive, make left turn
                        if (turnTime > 0) {
                            turnLeft(turnTime);
                            // If negative value, function makes a right turn
                        } else if (turnTime < 0) {
                            // Converts negative time into positive for setTimeout in turnRightWowFinish
                            turnTime = Math.abs(turnTime);
                            turnRight(turnTime);
                        }
                        int j = -1;
                        while (distance > 0.01) {
                            walkForward(distance, false);
                            currentXCoord = robot.getPixelColor(10, 0).getRed();
                            currentXCoordDecimalPart = robot.getPixelColor(10, 0).getGreen();
                            currentYCoord = robot.getPixelColor(50, 0).getRed();
                            currentYCoordDecimalPart = robot.getPixelColor(50, 0).getGreen();
                            currentXCoord += (currentXCoordDecimalPart / 100);
                            currentYCoord += (currentYCoordDecimalPart / 100);
                            currentFacing = robot.getPixelColor(50, 0).getBlue();
                            degrees = (currentFacing / 255.0) * 360.0;
                            radians = Math.toRadians(degrees);
                            System.out.println("Current (X,Y): " + "(" + currentXCoord + "," + currentYCoord + ")");
                            int bagsAreFull = robot.getPixelColor(180, 0).getRed();
                            System.out.println("Are bags full? " + bagsAreFull);
                            if (bagsAreFull != 0) {
                                try {
                                    Rect box = updateScreenshots(robot, degrees, j);
                                    // Calculate coordinates of NPCS relative to the center of the screen
                                    // Calculate coordinates of the center of the bounding box
                                    double xCenter = box.x + box.width / 2;
                                    double yCenter = box.y + box.height / 2;

                                    //System.out.println("NPC coordinates: (" + xCenter + ", " + yCenter + ")");
                                    double xCoordNpc = currentXCoord - (CENTER_OF_SCREEN.x - xCenter) * (1.0 / 1920.0);
                                    double yCoordNpc = currentYCoord - (CENTER_OF_SCREEN.y - yCenter) * (1.0 / 1080.0);
                                    double slopeToNpc = calculateWowDirection(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);

                                    // Determines how much our character will be turning based on our direction as well as the where the next point is
                                    double directionDiffToNpc = slopeToNpc - radians;
                                    directionDiffToNpc = shortenDirectionDiff(directionDiffToNpc);
                                    double turnTimeToNpc = (Math.toDegrees(directionDiffToNpc) / 360) * 2000;

                                    // If turnTime is positive, make left turn
                                    if (turnTimeToNpc > 0) {
                                        turnLeft(turnTimeToNpc);
                                        // If negative value, function makes a right turn
                                    } else if (turnTimeToNpc < 0) {
                                        // Converts negative time into positive for setTimeout in turnRightWowFinish
                                        turnTimeToNpc = Math.abs(turnTimeToNpc);
                                        turnRight(turnTimeToNpc);
                                    }

                                    focusNearbyTarget();
                                    int hasTarget = robot.getPixelColor(140, 0).getRed();
                                    while (hasTarget != 0) {
                                        walkForward(distance, false); // we are not close enough yet to the target for pressing TAB to pick it up, so move forward towards it
                                        double newXCoord = robot.getPixelColor(10, 0).getRed();
                                        double newXCoordDecimalPart = robot.getPixelColor(10, 0).getGreen();
                                        double newYCoord = robot.getPixelColor(50, 0).getRed();
                                        double newYCoordDecimalPart = robot.getPixelColor(50, 0).getGreen();
                                        currentXCoord = newXCoord + (newXCoordDecimalPart / 100);
                                        currentYCoord = newYCoord + (newYCoordDecimalPart / 100);
                                        currentFacing = robot.getPixelColor(50, 0).getBlue();
                                        degrees = (currentFacing / 255.0) * 360.0;
                                        radians = Math.toRadians(degrees);
                                        box = updateScreenshots(robot, degrees, i);
                                        xCenter = box.x + box.width / 2;
                                        yCenter = box.y + box.height / 2;
                                        System.out.println("NPC coordinates: (" + xCenter + ", " + yCenter + ")");
                                        xCoordNpc = currentXCoord - (CENTER_OF_SCREEN.x - xCenter) * (1.0 / 1920.0);
                                        yCoordNpc = currentYCoord - (CENTER_OF_SCREEN.y - yCenter) * (1.0 / 1080.0);

                                        System.out.println("Current (X,Y): " + "(" + currentXCoord + "," + currentYCoord + ")");
                                        System.out.println("NPC (X,Y): " + "(" + xCoordNpc + "," + yCoordNpc + ")");
                                        slopeToNpc = calculateWowDirection(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                                        System.out.println("Slope to NPC: " + slopeToNpc); // 3.4288841
                                        // radians = 3.56047 (204 degrees)
                                        // Determines how much our character will be turning based on our direction as well as the where the next point is
                                        directionDiffToNpc = slopeToNpc - radians; // 3.4288841 - 3.56047 = -0.1315859
                                        System.out.println("Direction diff to NPC in degrees: " + Math.toDegrees(directionDiffToNpc));
                                        directionDiffToNpc = shortenDirectionDiff(directionDiffToNpc);
                                        System.out.println("Shortened direction diff to NPC in degrees: " + Math.toDegrees(directionDiffToNpc));
                                        turnTimeToNpc = (Math.toDegrees(directionDiffToNpc) / 360) * 2000;
                                        System.out.println("Turn time to NPC " + turnTimeToNpc);
                                        // If turnTime is positive, make left turn
                                        if (turnTimeToNpc > 0) {
                                            turnLeft(turnTimeToNpc);
                                            // If negative value, function makes a right turn
                                        } else if (turnTimeToNpc < 0) {
                                            // Converts negative time into positive for setTimeout in turnRightWowFinish
                                            turnTimeToNpc = Math.abs(turnTimeToNpc);
                                            turnRight(turnTimeToNpc);
                                        }
                                        focusNearbyTarget();
                                        hasTarget = robot.getPixelColor(140, 0).getRed();
                                        System.out.println("hasTarget: " + hasTarget);
                                    }
                                    System.out.println("Slorkk");
                                    inCombat = robot.getPixelColor(80, 0).getRed();
                                    int speed = robot.getPixelColor(10, 0).getBlue();
                                    System.out.println("Pre-check: " + inCombat + " " + speed);
                                    while (inCombat != 0) {
                                        // Wait for the specified duration (in milliseconds)
                                        int durationMillis = 500; // 0.25 seconds
                                        try {
                                            Thread.sleep(durationMillis);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        interactWithTarget();
                                        inCombat = robot.getPixelColor(80, 0).getRed();
                                        speed = robot.getPixelColor(10, 0).getBlue();
                                        System.out.println(inCombat);
                                        System.out.println(speed);
                                    }
                                    if (speed != 0) {
                                        walkBackward();
                                        interactWithTarget();
                                    }
                                    while (inCombat != 255) {
                                        inCombat = robot.getPixelColor(80, 0).getRed();
                                    }
                                    // we killed the target, and now our focus was lost. Focus the most recent target to focus the dead npc so we can loot it
                                    focusPreviousTarget();
                                    interactWithTarget();
                                    focusSelf();
                                } catch (IndexOutOfBoundsException e) {
                                    // do nothing
                                }
                            }

                            distance = distance(currentXCoord, currentYCoord, path[i].x, path[i].y);
                            System.out.println("Distance: " + distance);
                            mostRecentDistancesQueue.add(distance);
                            System.out.println(mostRecentDistancesQueue.getQueue());
                            System.out.println("Are we stuck? " + mostRecentDistancesQueue.checkIfPlayerIsStuck());
                            boolean isPlayerStuck = mostRecentDistancesQueue.checkIfPlayerIsStuck();
                            if (isPlayerStuck) {
                                getPlayerUnstuck();
                            }
                            slope = calculateWowDirection(currentXCoord, currentYCoord, path[i].x, path[i].y);
                            System.out.println("Slope: " + slope);
                            // Determines how much our character will be turning based on our direction as well as the where the next point is
                            directionDiff = slope - radians;
                            directionDiff = shortenDirectionDiff(directionDiff);
                            System.out.println("Direction diff: " + directionDiff);
                            turnTime = (Math.toDegrees(directionDiff) / 360) * 2000;
                            System.out.println("Turn time: " + turnTime);
                            // If turnTime is positive, make left turn
                            if (turnTime > 0) {
                                turnLeft(turnTime);
                                // If negative value, function makes a right turn
                            } else if (turnTime < 0) {
                                // Converts negative time into positive for setTimeout in turnRightWowFinish
                                turnTime = Math.abs(turnTime);
                                turnRight(turnTime);
                            }
                        }
                        System.out.println("SUCCESS!!! Current (X,Y): " +
                                "(" + currentXCoord + "," + currentYCoord + ") and Target (X, Y): " + "(" + path[i].x +
                                "," + path[i].y + ")");
                    }
               // }

                /* below this is stuff we commented out */

                //Rect box = updateScreenshots(robot, degrees, -1);
                // Calculate coordinates of NPCS relative to the center of the screen
                // Calculate coordinates of the center of the bounding box
                //double xCenter = box.x + box.width / 2;
                //double yCenter = box.y + box.height / 2;

                // we have to convert from the distance between pixel locations on the screen to the distance in the in-game wow coordinates.
                // Note: this relies on fixed camera, with max zoom out factor, and a completely vertical birds eye view.
                // if you move the character from the middle of your screen to the edge of the screen in any of the 4 cardinal directions (N,S, E,W)
                // then you will move exactly 0.5 coordinates in that direction in wow.
                // this means that both the length and the width of the screen in wow coordinates is 1 respectively.
                // the screen resolution is 1920X1080. This means that moving the character from 0 pixels (very left) to 1920 pixels (very right) goes 1 wow coordinate.
                // so for each pixel you move on the x axis, it is scaled to 1/1920 wow coordinates.
                // For the Y axis the scale is 1/1080.
                // So the x-axis calculation for screen pixel coordinates to wow coordinates is this:
                // WoW coordinate=Character’s wow coordinate+(Pixel coordinate−Screen center coordinate) * (1/1920)
                // example:
                //
                // wowX = 47.96 - ((960.0 - 401.0) * (1/1920));
                // wowX = 47.96 - (559.0 * (1/1920));
                // wowX = 47.96 - (559.0 * 0.000521);
                // wowX = 47.96 - 0.2912
                // wowX = 47.67
                //System.out.println("NPC coordinates: (" + xCenter + ", " + yCenter + ")");
                //double xCoordNpc = currentXCoord - (CENTER_OF_SCREEN.x - xCenter) * (1.0 / 1920.0);
                //double yCoordNpc = currentYCoord - (CENTER_OF_SCREEN.y - yCenter) * (1.0 / 1080.0);
                //System.out.println("NPC coordinates: (" + xCoordNpc + ", " + yCoordNpc + ")");
                //double distanceToNpc = distance(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                //System.out.println("Distance to NPC: " + distanceToNpc);
                //System.out.println("Slope to NPC: ");
                //double slopeToNpc = calculateWowDirection(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                //System.out.println(slopeToNpc);


                // Determines how much our character will be turning based on our direction as well as the where the next point is
                //double directionDiffToNpc = slopeToNpc - radians;
                //System.out.println("Direction diff to NPC in radians: " + directionDiffToNpc);
                //directionDiffToNpc = shortenDirectionDiff(directionDiffToNpc);
                //System.out.println("Shortened direction diff to NPC in radians: " + directionDiffToNpc);

                // Calculates the amount of time needed to turn in order to get to our desired slope
                // The final static value refers to how much time (ms) is required to walk in a full circle
                //double turnTimeToNpc = (Math.toDegrees(directionDiffToNpc) / 360) * 2000;
                //System.out.println("Turn time to NPC: " + turnTimeToNpc);

                // If turnTime is positive, make left turn
                //if (turnTimeToNpc > 0) {
                //    turnLeft(turnTimeToNpc);
                   // If negative value, function makes a right turn
                //} else if (turnTimeToNpc < 0) {
                // Converts negative time into positive for setTimeout in turnRightWowFinish
                //   turnTimeToNpc = Math.abs(turnTimeToNpc);
                //    turnRight(turnTimeToNpc);
                //}
                /*focusNearbyTarget();
                int hasTarget = robot.getPixelColor(140, 0).getRed();
                int i = 0;
                while(hasTarget != 0) {
                    walkForward(); // we are not close enough yet to the target for pressing TAB to pick it up, so move forward towards it
                    double newXCoord = robot.getPixelColor(10, 0).getRed();
                    double newXCoordDecimalPart = robot.getPixelColor(10, 0).getGreen();
                    double newYCoord = robot.getPixelColor(50, 0).getRed();
                    double newYCoordDecimalPart = robot.getPixelColor(50, 0).getGreen();
                    currentXCoord = newXCoord + (newXCoordDecimalPart / 100);
                    currentYCoord = newYCoord + (newYCoordDecimalPart / 100);
                    currentFacing = robot.getPixelColor(50, 0).getBlue();
                    degrees = (currentFacing / 255.0) * 360.0;
                    radians = Math.toRadians(degrees);
                    box = updateScreenshots(robot, degrees, i);
                    xCenter = box.x + box.width / 2;
                    yCenter = box.y + box.height / 2;
                    System.out.println("NPC coordinates: (" + xCenter + ", " + yCenter + ")");
                    xCoordNpc = currentXCoord - (CENTER_OF_SCREEN.x - xCenter) * (1.0 / 1920.0);
                    yCoordNpc = currentYCoord - (CENTER_OF_SCREEN.y - yCenter) * (1.0 / 1080.0);

                    System.out.println("Current (X,Y): " + "(" + currentXCoord + "," + currentYCoord + ")");
                    System.out.println("NPC (X,Y): " + "(" + xCoordNpc + "," + yCoordNpc + ")");
                    distanceToNpc = distance(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                    System.out.println("new distance: " + distanceToNpc);
                    slopeToNpc = calculateWowDirection(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                    System.out.println("Slope to NPC: " + slopeToNpc); // 3.4288841
                    // radians = 3.56047 (204 degrees)
                    // Determines how much our character will be turning based on our direction as well as the where the next point is
                    directionDiffToNpc = slopeToNpc - radians; // 3.4288841 - 3.56047 = -0.1315859
                    System.out.println("Direction diff to NPC in degrees: " + Math.toDegrees(directionDiffToNpc));
                    directionDiffToNpc = shortenDirectionDiff(directionDiffToNpc);
                    System.out.println("Shortened direction diff to NPC in degrees: " + Math.toDegrees(directionDiffToNpc));
                    turnTimeToNpc = (Math.toDegrees(directionDiffToNpc) / 360) * 2000;
                    System.out.println("Turn time to NPC " + turnTimeToNpc);
                    // If turnTime is positive, make left turn
                    if (turnTimeToNpc > 0) {
                        turnLeft(turnTimeToNpc);
                        // If negative value, function makes a right turn
                    } else if (turnTimeToNpc < 0) {
                        // Converts negative time into positive for setTimeout in turnRightWowFinish
                        turnTimeToNpc = Math.abs(turnTimeToNpc);
                        turnRight(turnTimeToNpc);
                    }
                    focusNearbyTarget();
                    hasTarget = robot.getPixelColor(140, 0).getRed();
                    System.out.println("hasTarget: " + hasTarget);
                }
                System.out.println("Slorkk");
                inCombat = robot.getPixelColor(80, 0).getRed();
                int speed = robot.getPixelColor(10, 0).getBlue();
                System.out.println("Pre-check: " + inCombat + " " + speed);
                while (inCombat != 0) {
                    // Wait for the specified duration (in milliseconds)
                    int durationMillis = 500; // 0.25 seconds
                    try {
                        Thread.sleep(durationMillis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    interactWithTarget();
                    inCombat = robot.getPixelColor(80,0).getRed();
                    speed = robot.getPixelColor(10, 0).getBlue();
                    System.out.println(inCombat);
                    System.out.println(speed);
                }
                if (speed != 0) {
                    walkBackward();
                    interactWithTarget();
                }
                while (inCombat != 255) {
                    inCombat = robot.getPixelColor(80, 0).getRed();
                }
                // we killed the target, and now our focus was lost. Focus the most recent target to focus the dead npc so we can loot it
                focusPreviousTarget();
                interactWithTarget();*/


                /*Cursor.startCursorThread();
                int i = 0;
                while(distanceToNpc >= 0.06) {
                    walkForward();
                    double newXCoord = robot.getPixelColor(10, 0).getRed();
                    double newXCoordDecimalPart = robot.getPixelColor(10, 0).getGreen();
                    double newYCoord = robot.getPixelColor(50, 0).getRed();
                    double newYCoordDecimalPart = robot.getPixelColor(50, 0).getGreen();
                    currentXCoord = newXCoord + (newXCoordDecimalPart / 100);
                    currentYCoord = newYCoord + (newYCoordDecimalPart / 100);
                    currentFacing = robot.getPixelColor(50, 0).getBlue();
                    degrees = (currentFacing / 255.0) * 360.0;
                    radians = Math.toRadians(degrees);
                    box = updateScreenshots(robot, degrees, i);
                    xCenter = box.x + box.width / 2;
                    yCenter = box.y + box.height / 2;
                    System.out.println("NPC coordinates: (" + xCenter + ", " + yCenter + ")");
                    xCoordNpc = currentXCoord - (CENTER_OF_SCREEN.x - xCenter) * (1.0 / 1920.0);
                    yCoordNpc = currentYCoord - (CENTER_OF_SCREEN.y - yCenter) * (1.0 / 1080.0);

                    System.out.println("Current (X,Y): " + "(" + currentXCoord + "," + currentYCoord + ")");
                    System.out.println("NPC (X,Y): " + "(" + xCoordNpc + "," + yCoordNpc + ")");
                    distanceToNpc = distance(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                    System.out.println("new distance: " + distanceToNpc);
                    slopeToNpc = calculateWowDirection(currentXCoord, currentYCoord, xCoordNpc, yCoordNpc);
                    System.out.println("Slope to NPC: " + slopeToNpc); // 3.4288841
                    // radians = 3.56047 (204 degrees)
                    // Determines how much our character will be turning based on our direction as well as the where the next point is
                    directionDiffToNpc = slopeToNpc - radians; // 3.4288841 - 3.56047 = -0.1315859
                    System.out.println("Direction diff to NPC in degrees: " + Math.toDegrees(directionDiffToNpc));
                    directionDiffToNpc = shortenDirectionDiff(directionDiffToNpc);
                    System.out.println("Shortened direction diff to NPC in degrees: " + Math.toDegrees(directionDiffToNpc));
                    turnTimeToNpc = (Math.toDegrees(directionDiffToNpc) / 360) * 2000;
                    System.out.println("Turn time to NPC " + turnTimeToNpc);
                    // If turnTime is positive, make left turn
                    if (turnTimeToNpc > 0) {
                        turnLeft(turnTimeToNpc);
                        // If negative value, function makes a right turn
                    } else if (turnTimeToNpc < 0) {
                        // Converts negative time into positive for setTimeout in turnRightWowFinish
                        turnTimeToNpc = Math.abs(turnTimeToNpc);
                        turnRight(turnTimeToNpc);
                    }

                    // note: since the color value of the pixel is in integers, it is going to be difficult to make the bot walk to a very precise point like
                    // 78.55, 59.23, because the java program is only getting the rounded version of 78 and 59. So it will get to the general location as of now but not more precise than that.
                    // an idea I have is to make the green value return the decimal part of the coordinate as an integer, i.e if we are currently at 78.56 then the green value would return 56
                    i++;
                }
                Cursor.stopCursorThread();*/
                return false;
            }
            return true;
        }, null);
    }

    public static double distance(double currentXCoord, double currentYCoord, double targetXCoord, double targetYCoord)
    {
        double xDistSq = Math.pow(targetXCoord - currentXCoord, 2);
        double yDistSq = Math.pow(targetYCoord - currentYCoord, 2);
        return Math.sqrt(xDistSq + yDistSq);
    }

    // calculates number of degrees in radians between current point and next point
    // Y needs to be flipped because it's negative-y - up.
    // WoW coordinate system has direction "0" pointing at a 90 degree angle
    // to regular coordinates, so we need to rotate it and then do the modulo
    // so that the final direction result is in 0-2PI range.
    public static double calculateWowDirection(double currentXCoord, double currentYCoord, double targetXCoord, double targetYCoord)
    {
        // Current (X,Y): (48.08,87.45)
        //NPC (X,Y): (48.204479166666665,87.8712962962963)
        double slope = Math.atan2(targetYCoord - currentYCoord, currentXCoord - targetXCoord);
        // 1.858089092381
        // slope is the absolute direction to the next point from the player
        slope += Math.PI; // map to 0-2PI range
        // 4.9996791
        // Rotate by 90 degrees (so that 0 is up, not right)
        slope -= Math.PI * 0.5;
        //1.570795
        // Ensures that slope is not less than 0
        if (slope < 0) {
            slope += Math.PI * 2;
        }
        // Ensures slope is not greater than 2p
        if (slope > Math.PI * 2) {
            slope -= Math.PI * 2;
        }
        // 3.4288855919000625
        return slope;
    }
      
    // This replaces turns over 180 degrees into smaller turns in the opposite direction
    // In other words, expresses the same turn more efficiently
    public static double shortenDirectionDiff(double directionDiff) {
        if (directionDiff > Math.PI) directionDiff = ((Math.PI * 2) - directionDiff) * -1;
        if (directionDiff < -Math.PI) directionDiff = (Math.PI * 2) - (directionDiff * -1);
        return directionDiff;
    }

    public static List<Rect> mergeBoundingBoxes(List<Rect> boundingBoxes, double distanceThreshold) {
        List<Rect> mergedBoundingBoxes = new ArrayList<>();
        for (int i = 0; i < boundingBoxes.size(); i++) {
            Rect currentBox = boundingBoxes.get(i);
            Rect mergedBox = new Rect(currentBox.tl(), currentBox.br());

            for (int j = i + 1; j < boundingBoxes.size(); j++) {
                Rect nextBox = boundingBoxes.get(j);
                // Calculate the distance between the centers of the bounding boxes
                Point center1 = new Point(mergedBox.x + mergedBox.width / 2, mergedBox.y + mergedBox.height / 2);
                Point center2 = new Point(nextBox.x + nextBox.width / 2, nextBox.y + nextBox.height / 2);
                double distance = distance(center2.x, center2.y, center1.x, center1.y);
                //System.out.println("Distance between two bounding boxes: " + distance);
                // If the distance is less than the threshold, merge the bounding boxes
                if (distance < distanceThreshold) {
                    mergedBox = mergeRects(mergedBox, nextBox);
                }
            }
            mergedBoundingBoxes.add(mergedBox);
        }
        return mergedBoundingBoxes;
    }

    public static List<Rect> filterNestedBoundingBoxes(List<Rect> mergedBoundingBoxes) {
        List<Rect> filteredBoundingBoxes = new ArrayList<>();

        // Sort the bounding boxes by area in descending order
        mergedBoundingBoxes.sort((box1, box2) -> Double.compare(box2.area(), box1.area()));
        // Iterate through the sorted list of bounding boxes
        for (Rect currentBox : mergedBoundingBoxes) {
            boolean isNested = false;

            // Check if the current bounding box is entirely contained within other bounding box
            for (Rect otherBox : mergedBoundingBoxes) {
                if (!currentBox.equals(otherBox) && isNested(currentBox, otherBox)) {
                    isNested = true;
                    break;
                }
            }
            // If the current bounding box is not entirely contained within any other bounding box, add it into the filtered
            // list because it is the outermost merged box found below the distance threshold
            if (!isNested) {
                filteredBoundingBoxes.add(currentBox);
            }
        }
        return filteredBoundingBoxes;
    }

    public static boolean isNested(Rect smaller, Rect larger) {
        return larger.contains(smaller.tl()) && larger.contains(smaller.br());
    }

    public static Rect mergeRects(Rect rect1, Rect rect2) {
        int x = Math.min(rect1.x, rect2.x);
        int y = Math.min(rect1.y, rect2.y);
        int width = Math.max(rect1.x + rect1.width, rect2.x + rect2.width) - x;
        int height = Math.max(rect1.y + rect1.height, rect2.y + rect2.height) - y;
        return new Rect(x, y, width, height);
    }

    public static Rect updateScreenshots(Robot robot, double facing, int iteration) {
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenImage = robot.createScreenCapture(screenRect);
        // Save the screenshot to a file
        File file = new File("screenshot.png");
        String copyFileName = "screenshot_copy_" + iteration + ".png";
        File copyFile = new File(copyFileName);
        try {
            ImageIO.write(screenImage, "png", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Screenshot saved to: " + file.getAbsolutePath());
        Mat image = Imgcodecs.imread("screenshot.png");
        // Define the center of rotation (assuming the center of the image)
        Point center = new Point(image.cols() / 2, image.rows() / 2);
        // Define the angle of rotation (in degrees, counterclockwise)
        // Compute the rotation matrix
        System.out.println("Rotating by " + facing + " degrees.");
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, facing, 1.0);
        // Rotate the image
        Mat rotatedImage = new Mat();
        Imgproc.warpAffine(image, rotatedImage, rotationMatrix, image.size());
        // Save the rotated image
        Imgcodecs.imwrite("rotated_screenshot.png", rotatedImage);
        String rotatedImageCopyFileName = "rotated_screenshot_" + iteration + ".png";
        // Save the rotated image
        Imgcodecs.imwrite(rotatedImageCopyFileName, rotatedImage);
        // Convert the image to HSV color space
        Mat hsvImage = new Mat();
        Imgproc.cvtColor(rotatedImage, hsvImage, Imgproc.COLOR_BGR2HSV);
        // Define range of yellow color in HSV
        Scalar lowerYellow = new Scalar(30, 150, 150);
        Scalar upperYellow = new Scalar(35, 255, 255);
        //Scalar lowerYellow = new Scalar(30, 200, 200);
        //Scalar upperYellow = new Scalar(35, 255, 255);
        // Threshold the HSV image to get only yellow colors
        Mat mask = new Mat();
        Core.inRange(hsvImage, lowerYellow, upperYellow, mask);

        // Find contours in the yellow mask
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Filter out tiny contours
        List<MatOfPoint> filteredContours = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area >= 0.15) {
                filteredContours.add(contour);
            }
        }

        // Calculate bounding boxes around the contours
        List<Rect> boundingBoxes = new ArrayList<>();
        for (MatOfPoint contour : filteredContours) {
            Rect boundingBox = Imgproc.boundingRect(contour);
            boundingBoxes.add(boundingBox);
        }

        // Bitwise-AND mask and original image
        Mat result = new Mat();
        Core.bitwise_and(rotatedImage, rotatedImage, result, mask);

        // Define a distance threshold for merging boundingBoxes (adjust as needed)
        double distanceThreshold = 250.0;

        // Merge closely located bounding boxes
        List<Rect> mergedBoundingBoxes = mergeBoundingBoxes(boundingBoxes, distanceThreshold);

        // Filter out nested bounding boxes
        List<Rect> filteredBoundingBoxes = filterNestedBoundingBoxes(mergedBoundingBoxes);

        for (Rect box : filteredBoundingBoxes) {
            Imgproc.rectangle(result, new Point(box.x, box.y), new Point(box.x + box.width, box.y + box.height), new Scalar(0, 255, 0), 2);
        }

        // Save the result image
        Imgcodecs.imwrite("yellow_areas_detected.png", result);
        String yellowImageCopyFileName = "yellow_areas_detected_" + iteration + ".png";
        Imgcodecs.imwrite(yellowImageCopyFileName, result);

        // Calculate the center of the screen (your character's position)
        Point centerOfScreen = new Point(result.cols() / 2, result.rows() / 2);
        System.out.println("centerOfScreenPoint: " + centerOfScreen);
        filteredBoundingBoxes.sort(Comparator.comparingDouble(box -> {
            double centerX = box.x + box.width / 2;
            double centerY = box.y + box.height / 2;
            return distance(centerOfScreen.x, centerOfScreen.y, centerX, centerY);
        }));
        return filteredBoundingBoxes.get(0);
    }

    public static void turnLeft(double turnTime) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "a" to turn left
        input.input.ki.wVk = new WinDef.WORD('A');
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Wait for the specified duration (in milliseconds)
        int durationMillis = (int) turnTime; // 0.25 seconds
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Simulate releasing the A key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void turnRight(double turnTime) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "d" to turn right
        input.input.ki.wVk = new WinDef.WORD('D'); // 0x41
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Wait for the specified duration (in milliseconds)
        int durationMillis = (int) turnTime; // 0.25 seconds
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void focusNearbyTarget() {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(0x09); // TAB
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        int durationMillis = 500; // 0.25 seconds
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void focusPreviousTarget() {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(0x58); // X
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        int durationMillis = 500; // 0.25 seconds
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void focusSelf() {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(	0x5A); // Z
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        int durationMillis = 500; // 0.25 seconds
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void interactWithTarget() {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(0x49); // 0x41
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        int durationMillis = 500; // 0.25 seconds
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void walkForward(double distance, boolean random) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(0x57); // W
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Wait for the specified duration (in milliseconds)
        int durationMillis = 0;
        if (distance <= 0.15) {
            durationMillis = 100;
        } else {
            if (!random) {
                durationMillis = 1000;
            } else {
                int randomInRange = 500 + (int) (Math.random() * ((2500 - 500) + 1));
                durationMillis = randomInRange;
            }
        }
        try {
            Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void walkBackward() {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(0x53); // S
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void jump() {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        // Press "w" to walk forward
        input.input.ki.wVk = new WinDef.WORD(	0x20); // Spacebar to jump
        input.input.ki.dwFlags = new WinDef.DWORD(0);  // keydown

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

        // Simulate releasing the D key
        input.input.ki.dwFlags = new WinDef.DWORD(0x0002); // Key release
        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void getPlayerUnstuck() {
        int i = 0;
        while (i < 3) {
            int randomInRange = 300 + (int) (Math.random() * ((700 - 300) + 1));
            // Randomly choose to turn left or right
            if (Math.random() < 0.5) {
                turnLeft(randomInRange);
            } else {
                turnRight(randomInRange);
            }
            walkForward(1, true);
            jump();
            i++;
        }
    }

}
