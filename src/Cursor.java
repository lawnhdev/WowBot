import com.sun.jna.platform.win32.User32;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class Cursor {

    public static final Point CENTER_OF_SCREEN = new Point(960, 540);
    private static volatile boolean stopCursorThread = false;
    private static final String SWORD_CURSOR_IMAGE_PATH = "C:\\Users\\timdl\\IdeaProjects\\WowBot\\src\\CursorImages\\wow_sword_combat_highlighted.jpg";
    private static Thread cursorThread;


    // Runnable class to move the cursor in a circle
    public static class CursorRunnable implements Runnable {
        @Override
        public void run() {

            // Set the radius of the circle
            int radius = 100;

            // Set the speed of the cursor movement
            int speed = 5;
            Mat swordCursorImage = Imgcodecs.imread(SWORD_CURSOR_IMAGE_PATH, Imgcodecs.IMREAD_UNCHANGED);
            // Create a Robot instance to simulate mouse events
            Robot robot = null;
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }


            // Move the cursor in a circle
            while (!stopCursorThread && !Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < 3; i++) {
                    // Move the cursor in a circle
                    for (int angle = 0; angle < 360; angle += speed) {
                        // Calculate the new cursor position
                        int x = (int) (CENTER_OF_SCREEN.x + (int) (Math.cos(Math.toRadians(angle)) * radius));
                        int y = (int) (CENTER_OF_SCREEN.y + (int) (Math.sin(Math.toRadians(angle)) * radius));

                        // Set the cursor position
                        User32.INSTANCE.SetCursorPos(x, y);
                        int facingWrongWayFromNpc = robot.getPixelColor(10, 40).getRed();
                        System.out.println("Are we facing towards the NPC? " + facingWrongWayFromNpc);

                        // Capture a small region around the cursor
                        BufferedImage screen = robot.createScreenCapture(new Rectangle(MouseInfo.getPointerInfo().getLocation().x - 10, MouseInfo.getPointerInfo().getLocation().y - 10, 20, 20));
                        // Convert the BufferedImage toa Mat (OpenCV format)
                        Mat cursorRegion = new Mat(screen.getHeight(), screen.getWidth(), CvType.CV_8UC3);
                        BufferedImage cursorRegionBI = new BufferedImage(screen.getWidth(), screen.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                        cursorRegionBI.getGraphics().drawImage(screen, 0, 0, null);
                           byte[] pixels = ((DataBufferByte) cursorRegionBI.getRaster().getDataBuffer()).getData();
                        cursorRegion.put(0, 0, pixels);
                        // Compare the captured region with the sword cursor image
                        Mat result = new Mat();
                        Imgproc.matchTemplate(cursorRegion, swordCursorImage, result, Imgproc.TM_CCOEFF_NORMED);

                        // Check if the match exceeds a certain threshold (indicating the sword cursor)
                        if (Core.minMaxLoc(result).maxVal > 0.5) {
                            // Sword cursor detected, perform right-click
                            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                        }
                        // Pause briefly to slow down the movement
                        try {
                            Thread.sleep(5); // Adjust the sleep time as needed
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    radius -= 25;
                }
                radius = 100;
            }
        }
    }

    public static void startCursorThread() {
        cursorThread = new Thread(new CursorRunnable());
        cursorThread.setDaemon(true); // Daemon threads are automatically terminated when the program exits
        cursorThread.start();
    }

    public static void stopCursorThread() {
        stopCursorThread = true;
    }
}