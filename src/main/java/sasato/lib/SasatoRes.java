package sasato.lib;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * APIレスポンスライブラリ
 */
public class SasatoRes<T> {

    private final String statusCode;
    private final T data;
    private final ErrorDetails error;
    private final MetaInfo metadata;
    private final long startNanos;

    // デフォルトは安全な状態（マスク有効）
    private static boolean isDebugMode = false;

    public static void setDebugMode(boolean debug) {
        isDebugMode = debug;
    }

    // --- 内部構造 ---

    public static class MetaInfo {
        private final String requestId;
        private final String apiVersion;
        private final String timestamp;
        private long processingTimeMs;
        private PaginationInfo pagination;

        private MetaInfo(String version) {
            this.requestId = UUID.randomUUID().toString();
            this.apiVersion = version;
            this.timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        public String getRequestId() { return requestId; }
        public String getApiVersion() { return apiVersion; }
        public String getTimestamp() { return timestamp; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public Optional<PaginationInfo> getPagination() { return Optional.ofNullable(pagination); }
    }

    public static class PaginationInfo {
        private final long totalCount;
        private final int limit;
        private final int offset;

        public PaginationInfo(long totalCount, int limit, int offset) {
            this.totalCount = totalCount;
            this.limit = limit;
            this.offset = offset;
        }

        public long getTotalCount() { return totalCount; }
        public int getLimit() { return limit; }
        public int getOffset() { return offset; }
    }

    public static class ErrorDetails {
        private final String code;
        private final String message;
        private final String exceptionType;
        private final String stackTrace;
        private final String requestDetails;

        // サニタイズ用正規表現：password, token, secret, auth などを検知
        public static final Pattern SENSITIVE_PATTERN =
                Pattern.compile("(?i)(password|token|secret|apiKey|auth|credential|card_no)=[^&\\s,]*");

        public ErrorDetails(String code, String message, Optional<Throwable> throwable, String requestDetails) {
            this.code = code;
            this.message = message;
            this.exceptionType = throwable.map(t -> t.getClass().getName()).orElse(null);
            this.stackTrace = throwable.map(this::getStackTraceAsString).orElse(null);
            this.requestDetails = sanitize(requestDetails);
        }

        /**
         * 機密情報を正規表現でマスクする
         */
        private String sanitize(String details) {
            if (details == null) return null;
            return SENSITIVE_PATTERN.matcher(details).replaceAll("$1=********");
        }

        private String getStackTraceAsString(Throwable throwable) {
            return Arrays.stream(throwable.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
        }

        // --- セキュリティGetter ---
        public String getCode() { return code; }
        public String getMessage() { return message; }

        public String getExceptionType() {
            return isDebugMode ? exceptionType : "Hidden";
        }

        public String getStackTrace() {
            return isDebugMode ? stackTrace : "Access Denied: Set debug mode to true to see details.";
        }

        public String getRequestDetails() {
            return isDebugMode ? requestDetails : "Hidden";
        }
    }


    private SasatoRes(String statusCode, T data, ErrorDetails error) {
        this.startNanos = System.nanoTime();
        this.statusCode = statusCode;
        this.data = data;
        this.error = error;
        this.metadata = new MetaInfo("1.0.0");
    }

    /**
     * 成功レスポンスを生成します。
     * * @param data レスポンスに含めるデータ
     * @param <T> データの型
     * @return 成功ステータスとメタデータを含むSasatoResオブジェクト
     */
    public static <T> SasatoRes<T> success(T data) {
        return new SasatoRes<>("SUCCESS", data, null).build();
    }

    /**
     * 成功レスポンスを生成します。(ページング付き)
     * * @param data    レスポンスに含めるデータ
     *          total   ページ数
     *          limit   上限数
     *          offset  オフセット数
     * @param <T> データの型
     * @return 成功ステータスとメタデータを含むSasatoResオブジェクト
     */
    public static <T> SasatoRes<T> success(T data, long total, int limit, int offset) {
        SasatoRes<T> res = new SasatoRes<>("SUCCESS", data, null);
        res.metadata.pagination = new PaginationInfo(total, limit, offset);
        return res.build();
    }

    /**
     * エラーレスポンスを生成します。
     * デバッグモードが false の場合、スタックトレースや詳細情報は自動的に隠蔽されます。
     * * @param errorCode エラーコード
     * @param message ユーザー向けメッセージ
     * @return エラー情報を含むSasatoResオブジェクト
     */
    public static <T> SasatoRes<T> failure(String errorCode, String message) {
        return new SasatoRes<T>("FAILURE", null, new ErrorDetails(errorCode, message, Optional.empty(), null)).build();
    }

    /**
     * システム関連のエラーレスポンスを生成します。
     * デバッグモードが false の場合、スタックトレースや詳細情報は自動的に隠蔽されます。
     * * @param errorCode エラーコード
     * @param message ユーザー向けメッセージ
     * @param throwable 発生した例外（隠蔽対象）
     * @param requestDetails リクエストの詳細（サニタイズ対象）
     * @return エラー情報を含むSasatoResオブジェクト
     */
    public static <T> SasatoRes<T> error(String errorCode, String message, Throwable throwable, String requestDetails) {
        return new SasatoRes<T>("ERROR", null, new ErrorDetails(errorCode, message, Optional.ofNullable(throwable), requestDetails)).build();
    }

    private SasatoRes<T> build() {
        this.metadata.processingTimeMs = (System.nanoTime() - this.startNanos) / 1_000_000;
        return this;
    }

    public String getStatusCode() { return statusCode; }
    public T getData() { return data; }
    public ErrorDetails getError() { return error; }
    public MetaInfo getMetadata() { return metadata; }
}