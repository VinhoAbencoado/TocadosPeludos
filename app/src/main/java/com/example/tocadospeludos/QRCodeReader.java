package com.example.tocadospeludos;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Decodifica QR Codes a partir de uma imagem escolhida pelo usuário, usando ZXing core.
 *
 * <p>Optamos por ler de imagem (galeria/arquivo) em vez de câmera ao vivo porque o
 * scanner por câmera exigiria uma nova dependência (zxing-android-embedded) — proibido
 * pelas restrições do projeto. Assim a ONG fotografa/recebe o QR do adotante e valida aqui.
 */
public final class QRCodeReader {

    private QRCodeReader() {
    }

    /** Lê o texto do primeiro QR encontrado na imagem da URI, ou null se não houver/der erro. */
    public static String decodeFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        Bitmap bitmap = ImageUtils.loadBitmap(context, uri.toString(), 1600);
        if (bitmap == null) {
            // Fallback: tenta decodificar diretamente (caso o ImageUtils falhe por formato).
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                bitmap = android.graphics.BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                return null;
            }
        }
        return decode(bitmap);
    }

    private static String decode(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binary = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        try {
            Result result = new MultiFormatReader().decode(binary, hints);
            return result != null ? result.getText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrai o código do ingresso ("TP-XXXXXXXX") do conteúdo de um QR gerado pelo app.
     * O conteúdo tem uma linha "Codigo: TP-XXXX". Se não encontrar, devolve o texto inteiro
     * (permite colar/validar um código avulso).
     */
    public static String extractTicketCode(String qrText) {
        if (qrText == null) {
            return null;
        }
        for (String line : qrText.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("codigo:")) {
                return trimmed.substring("codigo:".length()).trim();
            }
        }
        return qrText.trim();
    }
}
