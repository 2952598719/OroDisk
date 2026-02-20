package top.orosirian.orodisk.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Funcs {

    public static String calculateMd5(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fis);
        }
    }

    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    public static String generateStoragePath(String md5, String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + File.separator + md5 + "." + extension;
    }

    public static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            }
        }
        boolean _ = file.delete();
    }

    public static boolean isVideo(String filename) {
        return Constant.VIDEO_EXTENSIONS.contains(getExtension(filename));
    }

    public static boolean isAudio(String filename) {
        return Constant.AUDIO_EXTENSIONS.contains(getExtension(filename));
    }

    public static boolean isImage(String filename) {
        return Constant.IMAGE_EXTENSIONS.contains(getExtension(filename));
    }

    public static boolean isDocument(String filename) {
        return Constant.DOCUMENT_EXTENSIONS.contains(getExtension(filename));
    }

    public static boolean isPdf(String filename) {
        return "pdf".equals(getExtension(filename));
    }

    public static boolean isCode(String filename) {
        return Constant.CODE_EXTENSIONS.contains(getExtension(filename));
    }

    public static boolean isText(String filename) {
        return Constant.TEXT_EXTENSIONS.contains(getExtension(filename));
    }

    public static boolean isPreviewable(String filename) {
        return isImage(filename) || isPdf(filename) || isCode(filename) || isText(filename);
    }

}
