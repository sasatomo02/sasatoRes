package sasato.lib;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SasatoResTest {

    @BeforeEach
    void setup() {
        SasatoRes.setDebugMode(false);
    }

    @Test
    @DisplayName("成功レスポンスが正しく生成され、データが取得できること")
    void testSuccessResponse() {
        String testData = "Hello, Sasato!";
        SasatoRes<String> res = SasatoRes.success(testData);

        assertEquals("SUCCESS", res.getStatusCode());
        assertEquals(testData, res.getData());
        assertNotNull(res.getMetadata().getRequestId());
        assertTrue(res.getMetadata().getProcessingTimeMs() >= 0);
    }

    @Test
    @DisplayName("ページネーション付きの成功レスポンスが正しく生成されること")
    void testSuccessWithPagination() {
        SasatoRes<String> res = SasatoRes.success("Data", 100, 10, 0);

        assertTrue(res.getMetadata().getPagination().isPresent());
        SasatoRes.PaginationInfo pg = res.getMetadata().getPagination().get();
        assertEquals(100, pg.getTotalCount());
        assertEquals(10, pg.getLimit());
        assertEquals(0, pg.getOffset());
    }

    @Test
    @DisplayName("セキュリティ：デバッグモードがfalseの時、スタックトレースが隠蔽されること")
    void testSecurityMasking() {
        Exception ex = new RuntimeException("Secret Error");
        SasatoRes<Object> res = SasatoRes.error("ERR001", "Error occurred", ex, "user_id=123, password=secret_pass");

        // デバッグモードがfalseなら"Hidden"や拒否メッセージが返るはず
        assertEquals("Hidden", res.getError().getExceptionType());
        assertEquals("Hidden", res.getError().getRequestDetails());
        assertTrue(res.getError().getStackTrace().contains("Access Denied"));
    }

    @Test
    @DisplayName("セキュリティ：デバッグモードがtrueの時、詳細情報が表示されること")
    void testDebugModeShowsDetails() {
        SasatoRes.setDebugMode(true);
        Exception ex = new RuntimeException("Secret Error");
        SasatoRes<Object> res = SasatoRes.error("ERR001", "Error occurred", ex, "user_id=123");

        // デバッグモードがtrueなら本物の情報が返る
        assertEquals("java.lang.RuntimeException", res.getError().getExceptionType());
        assertTrue(res.getError().getStackTrace().contains("testDebugModeShowsDetails"));
    }

    @Test
    @DisplayName("サニタイズ：requestDetails内の機密情報が黒塗りされていること")
    void testSanitizing() {
        // デバッグモードをONにしても、サニタイズ（黒塗り）は効いている必要がある
        SasatoRes.setDebugMode(true);
        String rawDetails = "token=abc-123, password=my_password, user=sasato";
        SasatoRes<Object> res = SasatoRes.error("ERR", "Msg", new RuntimeException(), rawDetails);

        String sanitized = res.getError().getRequestDetails();

        assertTrue(sanitized.contains("token=********"));
        assertTrue(sanitized.contains("password=********"));
        assertTrue(sanitized.contains("user=sasato")); // 機密でないものはそのまま
    }
}