package org.searchordsmen.chorussync.lib.test;

import org.searchordsmen.chorussync.lib.HttpUtils;

import com.google.guiceberry.GuiceBerryModule;
import com.google.inject.AbstractModule;

public class TestEnv extends AbstractModule {
    /*
    private final class LoggingResponseInterceptor implements HttpResponseInterceptor {
        public void process(HttpResponse resp, HttpContext ctx) throws HttpException, IOException {
            HttpEntity entity = resp.getEntity();
            String contents = EntityUtils.toString(entity);
            resp.setEntity(new StringEntity(contents, ContentType.get(entity)));
            System.out.println("Response:");
            System.out.println(contents);
        }
    }
    */
    @Override
    protected void configure() {
        install(new GuiceBerryModule());
        HttpUtils.setupCookieManager();
    }
    /*
    @Provides @Singleton HttpClient getHttpClient() {
        HttpClient client = HttpClientBuilder.create().addInterceptorLast(new LoggingResponseInterceptor()).build();
        return client;
    }
    */
}
