@import hexlet.code.dto.urls.UrlsPage
@import hexlet.code.model.Url
@import hexlet.code.utils.NamedRoutes
@import java.time.format.DateTimeFormatter
@param UrlsPage page

!{DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");}

@template.layout.page(page = page, content = @`
    <div class="container-lg mt-5">
        <h1>Сайты</h1>

        <table class="table table-bordered table-hover mt-3">
            <thead>
            <tr>
                <th class="col-1">ID</th>
                <th>Имя</th>
                <th class="col-2">Последняя проверка</th>
                <th class="col-1">Код ответа</th>
            </tr>
            </thead>
            <tbody>
            @for(Url url : page.getUrls())
                <tr>
                    <td>
                        ${url.getId()}
                    </td>
                    <td>
                        <a href="${NamedRoutes.urlPath(url.getId().toString())}">${url.getName()}</a>
                    </td>
                    <td>
                        @if(page.getLastCheck().get(url.getId()) != null)
                            ${page.getLastCheck().get(url.getId()).getCreatedAt().toLocalDateTime().format(formatter)}
                        @endif
                    </td>
                    <td>
                        @if(page.getLastCheck().get(url.getId()) != null)
                            ${page.getLastCheck().get(url.getId()).getStatusCode()}
                        @endif
                    </td>
                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`
)
