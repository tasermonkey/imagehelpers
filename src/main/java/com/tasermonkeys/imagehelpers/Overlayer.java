package com.tasermonkeys.imagehelpers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class Overlayer implements AutoCloseable {
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
            BufferedImageHelper.closeSafely(file, imageReader, ciis);
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
            BufferedImageHelper.closeSafely(file, reader, iis);
        }

    }

    public Overlayer overlayImage(String filename) throws IOException {
        return overlayImage(new File(filename));
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public Overlayer writeToFile(File outputFilename) throws IOException {
        BufferedImageHelper.writeToFile(outputFilename, bufferedImage);
        return this;
    }

    public Overlayer writeThumbnailToFile(int newWidth, int newHeight, String outputFilename) {


        return this;
    }

    public Overlayer writeToFile(String outputFilename) throws IOException {
        return writeToFile(new File(outputFilename));
    }

    @Override
    public void close() {
        BufferedImageHelper.closeSafely(file, imageReader, ciis);
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
