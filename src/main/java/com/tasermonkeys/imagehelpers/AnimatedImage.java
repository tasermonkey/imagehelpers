package com.tasermonkeys.imagehelpers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Iterator;
import java.util.logging.Logger;

import static com.tasermonkeys.imagehelpers.BufferedImageHelper.getFilePath;

public class AnimatedImage {
    private static Logger logger = Logger.getLogger(AnimatedImage.class.getName());
    public static boolean isAnimated(String filename) {
        return isAnimated(new File(filename));
    }

    public static boolean isAnimated(File file) {
        if ( file == null || !file.canRead())
        {
            logger.warning("Either can not read or file does not exist: " + getFilePath(file));
            return false;
        }
        ImageInputStream iis = null;
        ImageReader reader = null;
        try {
            iis = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            reader = readers.next();
            reader.setInput(iis);
            return reader.getNumImages(true) > 1;
        } catch (Exception e) {
            logger.severe("ERROR counting number of frames in image \"" + getFilePath(file) + "\": " + BufferedImageHelper.exceptionToString(e));
            return false;
        } finally {
            BufferedImageHelper.closeSafely(file, reader, iis);
        }
    }
}
