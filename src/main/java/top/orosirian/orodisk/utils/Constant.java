package top.orosirian.orodisk.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constant {

    public static final String LOCK_PREFIX = "disk:lock:";
    public static final String UPLOAD_LOCK_PREFIX = LOCK_PREFIX + "upload:";
    public static final String FOLDER_LOCK_PREFIX = LOCK_PREFIX + "folder:";
    public static final String FILE_LOCK_PREFIX = LOCK_PREFIX + "file:";

    public static final long LOCK_LEASE_TIME = 30;

    public static final String CACHE_PREFIX = "disk:cache:";
    public static final String USER_QUOTA_CACHE_PREFIX = CACHE_PREFIX + "user:quota:";
    public static final String QUEUE_KEY = "disk:vector:queue:";
    public static final String INFO_KEY = "disk:vector:info:";
    public static final long CACHE_QUOTA_TTL = 3600;

    public static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpeg", "mpg", "3gp"
    ));

    public static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a"
    ));

    public static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    ));

    public static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md"
    ));

    public static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "js", "ts", "jsx", "tsx", "vue", "html", "css", "scss", "sass", "less",
            "json", "xml", "yaml", "yml", "java", "py", "go", "rs", "c", "cpp", "h",
            "cs", "php", "rb", "swift", "kt", "scala", "sh", "bat", "sql"
    ));

    public static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "log", "csv", "ini", "conf", "cfg", "properties"
    ));

}
