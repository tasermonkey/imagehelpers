package com.tasermonkeys.imagehelpers;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ThumbnailMaker {
    private final BufferedImage srcImage;

    public ThumbnailMaker(BufferedImage srcImage) {
        this.srcImage = srcImage;
    }

    public static ThumbnailMaker fromImage(BufferedImage srcImage) {
        return new ThumbnailMaker(srcImage);
    }

    public ThumbnailMaker resizeToFile(File outputFile, int targetWidth, int targetHeight) throws IOException {
        BufferedImageHelper.writeToFile(outputFile, resize(targetWidth, targetHeight));
        return this;
    }

    public ThumbnailMaker resizeToFile(String outputFilename, int targetWidth, int targetHeight) throws IOException {
        return resizeToFile(new File(outputFilename), targetWidth, targetHeight);
    }

    public BufferedImage resize(int targetWidth, int targetHeight) {
        int currentWidth = srcImage.getWidth();
        int currentHeight = srcImage.getHeight();

        BufferedImage destImage = new BufferedImage(targetWidth, targetHeight, srcImage.getType());

        // TODO: crop the srcImage to be the same ratio as targetWidth/targetHeight, so that we don't stretch

        if (currentWidth == targetWidth && currentHeight == targetHeight) {
            Graphics g = destImage.getGraphics();
            try {
                g.drawImage(srcImage, 0, 0, null);
            } finally {
                g.dispose();
            }
            return destImage;
        }
        // If progressive resizing is not required, just do a single BILINEAR resizing
        if ((targetWidth * 2 >= currentWidth) && (targetHeight * 2 >= currentHeight)) {
            Graphics2D g = destImage.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(srcImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                g.dispose();
            }
            return destImage;
        }

        // This is Progressive Bilinear resizing
        BufferedImage tempImage = new BufferedImage(currentWidth, currentHeight, srcImage.getType());

        Graphics2D g = tempImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setComposite(AlphaComposite.Src);

            // Want this to be a function, however, would have to create an object to pass-by-ref
            {
                // Since we are going to half the size every iteration, and we want to end up on the target width/height
                // we will make the first iteration go down to a multiple of 2 of our targetWidth/height.
                // Its probably best to do the smallest change first than last, since the image is larger then.
                int startWidth;
                int startHeight;
                for (startWidth = targetWidth, startHeight = targetHeight;
                     startWidth < currentWidth && startHeight < currentHeight; startWidth *= 2, startHeight *= 2) {
                }

                currentWidth = startWidth / 2;
                currentHeight = startHeight / 2;
            }
            // Perform first resize step.
            g.drawImage(srcImage, 0, 0, currentWidth, currentHeight, null);

            // Perform an in-place progressive bilinear resize.
            while ((currentWidth >= targetWidth * 2) && (currentHeight >= targetHeight * 2)) {
                currentWidth /= 2;
                currentHeight /= 2;

                // last size for width
                if (currentWidth < targetWidth) {
                    currentWidth = targetWidth;
                }
                // last size for height
                if (currentHeight < targetHeight) {
                    currentHeight = targetHeight;
                }

                g.drawImage(tempImage, 0, 0, currentWidth, currentHeight, 0, 0, currentWidth * 2, currentHeight * 2, null);
            }

        } finally {
            // I hate java.  RAII would be so much better
            g.dispose();
        }

        Graphics2D destg = destImage.createGraphics();
        try {
            destg.drawImage(tempImage, 0, 0, targetWidth, targetHeight, 0, 0, currentWidth, currentHeight, null);
        } finally {
            destg.dispose();
        }
        return destImage;
    }


}
