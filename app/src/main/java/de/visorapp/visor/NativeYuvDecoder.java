package de.visorapp.visor;

/**
 * Created by Christian on 02.04.17.
 */

public class NativeYuvDecoder {

    static {
        System.loadLibrary("yuv-decoder");
    }

    // Java_de_visorapp_visor_NativeYuvDecoder_YUVtoRBGA
    public static native void YUVtoRBGA(byte[] yuv, int width, int height, int[] out);

    // Java_de_visorapp_visor_NativeYuvDecoder_YUVtoARBG
    public static native void YUVtoARBG(byte[] yuv, int width, int height, int[] out);

    // Java_de_visorapp_visor_NativeYuvDecoder_YUVtoARBG
    public static native void YUVtoRGBGreyscale(byte[] yuv, int width, int height, int[] out);
}
