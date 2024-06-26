package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlsRepository;
import hexlet.code.utils.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    private static final String HTML_PATH = "src/test/resources/test.html";
    private static Javalin app;
    private static MockWebServer mockServer;
    private static String urlName;

    public static String getContentOfHtmlFile() throws IOException {
        Path path = Paths.get(HTML_PATH);
        List<String> lines = Files.readAllLines(path);
        return String.join("\n", lines);
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        mockServer = new MockWebServer();
        urlName = mockServer.url(HTML_PATH).toString();
        MockResponse mockResponse = new MockResponse().setBody(getContentOfHtmlFile());
        mockServer.enqueue(mockResponse);
    }

    @AfterAll
    public static void afterAll() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    public void beforeEach() throws SQLException, IOException {

        app = App.getApp();
    }

    @Test
    public void testRootPage() {

        JavalinTest.test(app, (server, client) -> {
            Response response = client.get(NamedRoutes.rootPath());
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("Анализатор страниц");
        });
    }

    @Test
    public void testUrlsPath() {

        JavalinTest.test(app, (server, client) -> {
            Response response = client.get(NamedRoutes.urlsPath());
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    public void testAddPage() {

        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://www.google.com";
            Response response = client.post(NamedRoutes.urlsPath(), requestBody);
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("https://www.google.com");
            assertThat(UrlsRepository.getEntities()).hasSize(1);
        });
    }

    @Test
    public void testDoubleAddPage() throws SQLException {

        Url url = new Url("https://www.google.com");
        UrlsRepository.save(url);
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://www.google.com";
            Response response = client.post(NamedRoutes.urlsPath(), requestBody);
            assertThat(response.code()).isEqualTo(200);
//            assertThat(response.body().string()).contains("https://www.google.com");
            assertThat(UrlsRepository.getEntities()).hasSize(1);
        });
    }


    @Test
    public void testSavePage() throws SQLException {
        Url url = new Url("https://www.google.com");
        UrlsRepository.save(url);
        JavalinTest.test(app, (server, client) -> {
            Response response = client.get(NamedRoutes.urlPath(url.getId()));
            assertThat(response.code()).isEqualTo(200);
        });
    }

    @Test
    public void testBadRequest() {
        JavalinTest.test(app, (server, client) -> {
            Response response = client.get(NamedRoutes.urlPath("bad request"));
            assertThat(response.code()).isEqualTo(400);
        });
    }

    @Test
    public void testUrlNotExists() {
        JavalinTest.test(app, (server, client) -> {
            Response response = client.get("\\xcvsfaasdas");
            assertThat(response.code()).isEqualTo(404);
        });
    }

    @Test
    public void testEntities() {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://ru.hexlet.io";
            Response response = client.post(NamedRoutes.urlsPath(), requestBody);
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("https://ru.hexlet.io");
            assertThat(UrlsRepository.getEntities()).hasSize(1);
            Response response2 = client.get(NamedRoutes.urlPath("1"));
            assertThat(response2.code()).isEqualTo(200);
            assertThat(response2.body().string()).contains("https://ru.hexlet.io");
        });
    }

    @Test
    public void testCheckUrl() throws SQLException {
        Url url = new Url(urlName);
        UrlsRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            Response response = client.post(NamedRoutes.urlCheckPath(url.getId()));
            assertThat(response.code()).isEqualTo(200);

            UrlCheck urlCheck = UrlCheckRepository.getLastCheck(url.getId()).get();
            String title = urlCheck.getTitle();
            String h1 = urlCheck.getH1();
            String description = urlCheck.getDescription();

            assertThat(title).isEqualTo("Test HTML page");
            assertThat(h1).isEqualTo("H1 Test header");
            assertThat(description).isEqualTo("content description");
        });
    }
}
