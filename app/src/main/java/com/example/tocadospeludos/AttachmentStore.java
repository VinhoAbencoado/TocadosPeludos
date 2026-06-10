package com.example.tocadospeludos;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copia anexos (foto/PDF) escolhidos pelo usuário para o armazenamento privado do app,
 * para não depender da permissão da URI externa (que pode expirar). Os arquivos ficam em
 * filesDir/attachments e são servidos via FileProvider para visualização.
 */
public final class AttachmentStore {

    private static final String DIR = "attachments";
    public static final String AUTHORITY = "com.example.tocadospeludos.fileprovider";

    private AttachmentStore() {
    }

    /**
     * Copia o conteúdo da URI externa para um arquivo interno e retorna sua URI
     * ("file://" via getAbsolutePath em forma de content do FileProvider não é necessária aqui —
     * guardamos o caminho do arquivo interno como "app://" para reconhecê-lo depois).
     * Retorna a string a salvar, ou null em caso de falha.
     */
    public static String copyToInternal(Context context, Uri source, String keyPrefix) {
        if (source == null) {
            return null;
        }
        try {
            File dir = new File(context.getFilesDir(), DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String extension = guessExtension(context, source);
            File dest = new File(dir, keyPrefix + "_" + System.nanoTime()
                    + (extension.isEmpty() ? "" : "." + extension));

            try (InputStream in = context.getContentResolver().openInputStream(source);
                 OutputStream out = new FileOutputStream(dest)) {
                if (in == null) {
                    return null;
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            // Marca como arquivo interno para diferenciá-lo de content:// externo.
            return "app-file://" + dest.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** Indica se o valor salvo aponta para um arquivo interno copiado pelo app. */
    public static boolean isInternal(String stored) {
        return stored != null && stored.startsWith("app-file://");
    }

    /**
     * Resolve a URI visualizável de um valor salvo:
     * - "app-file://<path>" → content:// via FileProvider
     * - "content://..."      → a própria URI externa
     */
    public static Uri resolveViewableUri(Context context, String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        if (isInternal(stored)) {
            File file = new File(stored.substring("app-file://".length()));
            if (!file.exists()) {
                return null;
            }
            return FileProvider.getUriForFile(context, AUTHORITY, file);
        }
        return Uri.parse(stored);
    }

    /** Abre o anexo em um app visualizador (galeria/leitor de PDF). Retorna false se não der. */
    public static boolean openAttachment(Context context, String stored) {
        Uri uri = resolveViewableUri(context, stored);
        if (uri == null) {
            return false;
        }
        String type = context.getContentResolver().getType(uri);
        if (type == null) {
            type = "*/*";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String guessExtension(Context context, Uri uri) {
        String type = context.getContentResolver().getType(uri);
        if (type != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
            if (ext != null) {
                return ext;
            }
        }
        // Tenta pelo nome do arquivo.
        String name = displayName(context, uri);
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                return name.substring(dot + 1);
            }
        }
        return "";
    }

    private static String displayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            // ignora
        }
        return null;
    }
}
