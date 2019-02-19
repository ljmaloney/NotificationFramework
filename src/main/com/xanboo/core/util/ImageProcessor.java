/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/ImageProcessor.java,v $
 * $Id: ImageProcessor.java,v 1.7 2010/10/15 16:01:01 levent Exp $
 * 
 * Copyright 2007 Xanboo, Inc.
 *
 */
package com.xanboo.core.util;

import java.awt.Canvas;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.*;
import javax.swing.ImageIcon;

import java.awt.Graphics2D;

import java.awt.image.BufferedImage;
//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;
//import com.sun.image.codec.jpeg.JPEGEncodeParam;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;


/**
 * This class replaces the 'convert' executable calls (defined with 'image.processor' property)
 * made in image/logo upload servlets. Dependent on JVM 1.4 and higher to support Headless 
 * Operation. JVM must run with "-Djava.awt.headless=true" option
 *
 */
public class ImageProcessor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ImageProcessor.class.getName());

    private String iFile;
    private String oFile;
    private int tW;
    private int tH;
    private int tQ;
    private boolean saveAspect;

    // Constructor (non-threaded)
    public ImageProcessor() { }
    
    
    // Constructor (threaded)
    public ImageProcessor(String inFile, String outFile, int tWidth, int tHeight, int quality, boolean aspectRatio) {
        this.iFile=inFile;
        this.oFile=outFile;
        this.tW=tWidth;
        this.tH=tHeight;
        this.tQ=quality;
        this.saveAspect=aspectRatio;
        
        Thread th=new Thread(this);
        th.setDaemon(true);
        th.start();
    }

    
    public void run() {
        createThumbnail(iFile, oFile, tW, tH, tQ, saveAspect);
    }
    
    public static int createThumbnail(String inFile, String outFile, int tWidth, int tHeight, int quality, boolean aspectRatio) {
        File test_file = new File(inFile);
        if(!test_file.exists() || test_file.isDirectory()) return -1;

        if(logger.isDebugEnabled()) {                
            logger.debug("Creating thumbnail for:" + inFile);
        }
        
        // load the original image first
        Image input_image = new ImageIcon(inFile).getImage();
        
        return createThumbnail(input_image, outFile, tWidth, tHeight, quality, aspectRatio);
    }

    public static int createThumbnail(byte[] inBytes, String outFile, int tWidth, int tHeight, int quality, boolean aspectRatio) {
        // load the original image first
        Image input_image = new ImageIcon(inBytes).getImage();
        
        return createThumbnail(input_image, outFile, tWidth, tHeight, quality, aspectRatio);
    }
    
    
    private static int createThumbnail(Image inputImage, String outFile, int tWidth, int tHeight, int quality, boolean aspectRatio) {
        int to_width, to_height;
        int image_width, image_height;

        image_width = inputImage.getWidth(null);
        image_height = inputImage.getHeight(null);

        if(image_width==-1 || image_height==-1) return -1;
        
        Image temp=null;

        // if already small, don't scale
        if(image_width>tWidth || image_height>tHeight || !aspectRatio) {
            // calculate thumbnail width and height based on aspectRatio flag and image size
            float fact2;
            if(aspectRatio) {
                if (image_width>image_height) {
                    fact2=((float) tWidth)/image_width;
                }else {
                    fact2=((float) tHeight)/image_height;
                }

                to_width = (int)((float)image_width * fact2);
                to_height= (int)((float)image_height* fact2);
            }else {
                to_width = tWidth;
                to_height = tHeight;
            }

            temp=inputImage.getScaledInstance(to_width, to_height, Image.SCALE_SMOOTH);

            MediaTracker tracker = new MediaTracker(new Canvas());
            tracker.addImage(temp, 0);
            try { tracker.waitForAll(); }catch(InterruptedException e){};
            tracker = null;

        //will keep the original size    
        }else {
            temp=inputImage;
            to_width=image_width;
            to_height=image_height;
        }

        // Create an image buffer in which to paint on.
        BufferedImage outImage = new BufferedImage(to_width, to_height, BufferedImage.TYPE_INT_RGB);

        // Paint image.
        Graphics2D g2d =  outImage.createGraphics();
        g2d.drawImage(temp, 0, 0, null);
        g2d.dispose();

        //JVM 1.4 ImageIcon class caching bug requires the following flush line - but causes some perf degrade
        //So, we make 1.5 and above a requirement to use this class and commenting out the flush line
        //If problem occurs in the future, we can enable it back. -LT
        //input_image.flush();
        
        // save the image to a thumbnail file:
//        try {
//            // JPEG-encode the image and write to file.
//            OutputStream os =  new FileOutputStream(outFile);
//            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
//
//            //set encoding quality given as 0-100%
//            JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(outImage);
//	    param.setQuality((float)(quality/100f), false);
//	    encoder.setJPEGEncodeParam(param);
//
//            encoder.encode(outImage);
//            os.close();
//
//            File f = new File(outFile);
//            return (int) f.length();
//
//        }catch (IOException e) {
//            if(logger.isDebugEnabled()) {
//                logger.error("Exception creating thumbnail: ", e);
//            }else {
//                logger.warn("Exception creating thumbnail: " + e.getMessage());
//            }
//
//        }
        return 0;
  }

/*
  public static void usage() {
	System.out.println("Usage: thumbcr <srcjpegfile> <destthumbfile> <width> <height> <quality>");
  }


  public static void main(String args[]) {

    if(args.length!=5) {
	usage();
	System.exit(1);
    }

    ImageProcessor iproc=new ImageProcessor();
    iproc.createThumbnail(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), true);
    
    //threaded example
    //ImageProcessor iproc=new ImageProcessor(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), true);
    
    try { Thread.sleep(2000); }catch(Exception e) {}
    System.exit(0);
  }
*/
}
