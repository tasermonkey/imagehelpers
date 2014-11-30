package com.tasermonkeys.imagehelpers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Overlayer implements AutoCloseable {
    public static Logger log = Logger.getLogger(Overlayer.class.getName());
    private File file;
    private ImageReader imageReader;
    private BufferedImage bufferedImage;
    private ImageInputStream ciis;
    private float overlayAlpha = 0.80f;


    protected Overlayer(File file) throws IOException {
        this.file = file;
        String extension = file.getName().substring(file.getName().lastIndexOf('.')+1).toLowerCase();
        if (!extension.equals("gif")) {
            throw new IOException("Only gifs are currently supported");
        }
        imageReader = ImageIO.getImageReadersByFormatName(extension).next();
        ciis = ImageIO.createImageInputStream(file);
        try {
            imageReader.setInput(ciis, false);
            bufferedImage = imageReader.read(0);
        } catch (IOException | RuntimeException e) {
            closeSafetly(file, imageReader, ciis);
            throw e;
        }
    }

    public static Overlayer fromFile(String file) throws IOException {
        return fromFile(new File(file));
    }

    public static Overlayer fromFile(File file) throws IOException {
        return new Overlayer(file);
    }

    public boolean isAnimated() {
        try {
            return imageReader.getNumImages(true) > 1;
        } catch (IOException e) {
            return false;
        }
    }

    public Overlayer setOverlayAlpha(float alpha) {
        overlayAlpha = alpha;
        return this;
    }

    public Overlayer overlayImage(BufferedImage overlayImg) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int type = bufferedImage.getType();
        int ovWidth = overlayImg.getWidth();
        int ovHeight = overlayImg.getHeight();
        int ovPosX = (int) ((width / 2.0) - (ovWidth / 2.0));
        int ovPosY = (int) ((height / 2.0) - (ovHeight / 2.0));
        if ( ovPosX < 0 ) ovPosX = 0;
        if ( ovPosY < 0 ) ovPosY = 0;

        BufferedImage imgWithOverlay = new BufferedImage(width, height, type);
        Graphics2D g = imgWithOverlay.createGraphics();


        try {
            g.drawImage(bufferedImage, 0, 0, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
            g.drawImage(overlayImg, ovPosX, ovPosY, null);
        } finally {
            g.dispose();
        }
        bufferedImage = imgWithOverlay;
        return this;
    }

    public Overlayer overlayImage(File file) throws IOException {
        ImageInputStream iis = null;
        ImageReader reader = null;
        try {
            iis = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            reader = readers.next();
            reader.setInput(iis);
            return overlayImage(reader.read(0));
        } finally {
            closeSafetly(file, reader, iis);
        }

    }

    public Overlayer overlayImage(String filename) throws IOException {
        return overlayImage(new File(filename));
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public Overlayer writeToFile(File outputFilename) throws IOException {
        String extension = outputFilename.getName().substring(outputFilename.getName().lastIndexOf('.')+1).toLowerCase();
        String format = formatTypeFromExtension(extension);
        ImageWriter iw;
        try
        {
            iw = ImageIO.getImageWritersByFormatName(format).next();
        }
        catch (NoSuchElementException e)
        {
            throw new IOException(e);
        }
        try {
            try (FileOutputStream fos = new FileOutputStream(outputFilename)) {
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
                    iw.setOutput(ios);
                    iw.write(new IIOImage(bufferedImage, null, null));
                }
            }
            return this;
        } finally {
            closeSafetly(outputFilename, iw);
        }
    }

    public Overlayer writeToFile(String outputFilename) throws IOException {
        return writeToFile(new File(outputFilename));
    }

    @Override
    public void close() {
        closeSafetly(file, imageReader, ciis);
    }

    public static void closeSafetly(File file, ImageWriter writer) {
        try {
            if (writer != null) writer.dispose();
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not close ImageWriter for file " + getFilePath(file) + ": " + exceptionToString(e));
        }
    }

    public static void closeSafetly(File file, ImageReader reader, ImageInputStream cis) {
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

    private static String getFilePath(File file) {
        if ( file == null )
            return "UnknownInput";
        else
            return file.getAbsolutePath();
    }

    private static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private static String formatTypeFromExtension(String ext) throws IOException {
        Iterator<ImageReader> rIter = ImageIO.getImageReadersBySuffix(ext);
        if (rIter.hasNext())
        {
            return rIter.next().getFormatName();
        }
        throw new IOException("Unknown extension " + ext);
    }

    public static void main(String[] args) throws IOException {
        String mainFilename = args[0];
        String overlayFilename = args[1];
        String outputFilename = args[2];
        try (Overlayer overlayer = Overlayer.fromFile(mainFilename)) {
            System.out.println("Is an animated gif: " + overlayer.isAnimated());
            overlayer.overlayImage(overlayFilename).writeToFile(outputFilename);
        }
    }
}
