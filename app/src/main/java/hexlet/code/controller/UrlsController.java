package hexlet.code.controller;

import hexlet.code.dto.urls.UrlPage;
import hexlet.code.dto.urls.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlsRepository;
import hexlet.code.utils.NamedRoutes;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.javalin.rendering.template.TemplateUtil.model;

@Slf4j
public class UrlsController {

    public static void index(final Context ctx) throws SQLException {

        List<Url> urls = UrlsRepository.getEntities();
        Map<Long, UrlCheck> check = UrlCheckRepository.getLastChecks();
        UrlsPage page = new UrlsPage(urls, check);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("urls/index.jte", model("page", page));
    }

    public static void show(final Context ctx) throws SQLException {

        long id = ctx.pathParamAsClass("id", Long.class).get();
        Url url = UrlsRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));
        List<UrlCheck> urlCheck = UrlCheckRepository.getEntities(id);
        UrlPage page = new UrlPage(url, urlCheck);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("urls/show.jte", Collections.singletonMap("page", page));
    }

    public static void create(final Context ctx)
            throws SQLException {

        String inputUrl = ctx.formParam("url");
        URL parseUrl;
        try {
            parseUrl = new URI(inputUrl.trim().toLowerCase()).toURL();
        } catch (URISyntaxException | MalformedURLException
                 | IllegalArgumentException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flashType", "alert-danger");
            ctx.redirect(NamedRoutes.rootPath());
            return;
        }
        String normalUrl = String.format(
                "%s://%s%s",
                parseUrl.getProtocol() == null ? "http" : parseUrl.getProtocol(),
                parseUrl.getHost(),
                parseUrl.getPort() == -1 ? "" : ":" + parseUrl.getPort()
        );
        if (UrlsRepository.existsByName(normalUrl)) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "alert-danger");
            ctx.redirect(NamedRoutes.rootPath());
        } else {
            Url url = new Url(normalUrl);
            UrlsRepository.save(url);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "alert-success");
            ctx.redirect(NamedRoutes.urlsPath());
        }
    }

    public static void check(final Context ctx)
            throws SQLException {

        long id = ctx.pathParamAsClass("id", Long.class).get();

        Url url = UrlsRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));

        HttpResponse<String> httpResponse;
        try {
            httpResponse = Unirest.get(url.getName()).asString();
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Страница не проверена");
            ctx.sessionAttribute("flash-type", "alert-danger");
            ctx.redirect(NamedRoutes.urlsPath() + "/" + id);
            return;
        }
        int statusCode = httpResponse.getStatus();
        String body = httpResponse.getBody();
        Document document = Jsoup.parse(body);
        long urlId = url.getId();
        String h1 = document.select("h1").text();
        String title = document.title();
        String description = document.select("meta[name=description]").attr("content");
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());

        UrlCheck urlCheck = new UrlCheck(urlId, statusCode, h1, title, description);
        UrlCheckRepository.save(urlCheck);

        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.sessionAttribute("flash-type", "alert-success");
        ctx.redirect(NamedRoutes.urlsPath() + "/" + id);
    }
}
