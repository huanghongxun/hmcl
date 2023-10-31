package org.jackhuang.hmcl.ui;

import net.burningtnt.webp.SimpleWEBPLoader;
import net.burningtnt.webp.utils.RGBABuffer;
import org.jackhuang.hmcl.util.ResourceNotFoundError;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public final class AwtUtils {
    private AwtUtils() {
    }

    public static void setAppleIcon(Image image) {
        try {
            Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
            Method getTaskBarMethod = taskbarClass.getDeclaredMethod("getTaskbar");
            Object taskBar = getTaskBarMethod.invoke(null);
            Method setIconImageMethod = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
            setIconImageMethod.invoke(taskBar, image);
        } catch (Throwable ignore) {
        }
    }

    public static Image loadBuiltinWebpImage(String url) {
        RGBABuffer rgbaBuffer;
        try (InputStream inputStream = ResourceNotFoundError.getResourceAsStream(url)) {
            rgbaBuffer = SimpleWEBPLoader.decodeStreamByImageLoaders(inputStream);
        } catch (IOException e) {
            throw new ResourceNotFoundError("Cannot load builtin resource '" + url + "'.", e);
        }

        int width = rgbaBuffer.getWidth(), height = rgbaBuffer.getHeight();
        BufferedImage awtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WritableRaster writableRaster = awtImage.getRaster();
        byte[] rgbaData = rgbaBuffer.getRGBAData(), argbCache = new byte[4];
        for (int y = 0; y < height; y++) {
            int lineIndex = y * width * 4;
            for (int x = 0; x < width; x++) {
                // Transfer RGBA storage to ARGB storage
                int index = lineIndex + x * 4;
                System.arraycopy(
                        rgbaData,
                        index,
                        argbCache,
                        1,
                        3
                ); // Copy RGB data first with arraycopy to enhance performance
                argbCache[0] = rgbaData[index + 3]; // Manually copy A data

                writableRaster.setDataElements(x, y, argbCache);
            }
        }

        return awtImage;
    }
}
