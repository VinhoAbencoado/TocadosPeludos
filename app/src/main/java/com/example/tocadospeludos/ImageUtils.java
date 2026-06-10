package com.example.tocadospeludos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

/**
 * Carregamento leve de imagens a partir de URIs de conteúdo (fotos de animais/anexos).
 * Faz downsampling para evitar OutOfMemory ao exibir fotos grandes em cards.
 */
public class ImageUtils {

    private ImageUtils() {
    }

    /**
     * Decodifica a imagem da URI redimensionando para caber em ~maxSizePx.
     * Retorna null se a URI for inválida, vazia ou ilegível (ex.: permissão perdida).
     */
    public static Bitmap loadBitmap(Context context, String uriString, int maxSizePx) {
        if (uriString == null || uriString.trim().isEmpty()) {
            return null;
        }
        try {
            Uri uri = Uri.parse(uriString);

            // 1ª passada: lê apenas as dimensões.
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            // 2ª passada: decodifica já reduzida.
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calculateInSampleSize(bounds, maxSizePx);
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int maxSizePx) {
        if (maxSizePx <= 0) {
            return 1;
        }
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while ((height / inSampleSize) > maxSizePx || (width / inSampleSize) > maxSizePx) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }
}
