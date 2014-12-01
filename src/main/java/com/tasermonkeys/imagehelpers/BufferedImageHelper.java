package com.tasermonkeys.imagehelpers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BufferedImageHelper {
    public static Logger log = Logger.getLogger(BufferedImageHelper.class.getName());

    public static void writeToFile(File outputFilename, BufferedImage bufferedImage) throws IOException {
        String extension = outputFilename.getName().substring(outputFilename.getName().lastIndexOf('.') + 1).toLowerCase();
        String format = formatTypeFromExtension(extension);
        ImageWriter iw;
        try {
            iw = ImageIO.getImageWritersByFormatName(format).next();
        } catch (NoSuchElementException e) {
            throw new IOException(e);
        }
        try {
            try (FileOutputStream fos = new FileOutputStream(outputFilename)) {
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
                    iw.setOutput(ios);
                    iw.write(new IIOImage(bufferedImage, null, null));
                }
            }
        } finally {
            closeSafely(outputFilename, iw);
        }
    }

    public static void closeSafely(File file, ImageWriter writer) {
        try {
            if (writer != null) writer.dispose();
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not close ImageWriter for file " + getFilePath(file) + ": " + exceptionToString(e));
        }
    }

    public static void closeSafely(File file, ImageReader reader, ImageInputStream cis) {
        try {
            if (reader != null) reader.dispose();
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not close imageReader for file " + getFilePath(file) + ": " + exceptionToString(e));
        }
        try {
            if (cis != null) cis.close();
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not close ImageInputStream for file " + getFilePath(file) + ": " + exceptionToString(e));
        }
    }

    public static String getFilePath(File file) {
        if (file == null)
            return "UnknownInput";
        else
            return file.getAbsolutePath();
    }

    public static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static String formatTypeFromExtension(String ext) throws IOException {
        Iterator<ImageReader> rIter = ImageIO.getImageReadersBySuffix(ext);
        if (rIter.hasNext()) {
            return rIter.next().getFormatName();
        }
        throw new IOException("Unknown extension " + ext);
    }
}
