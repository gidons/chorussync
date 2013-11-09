package org.searchordsmen.chorussync.lib.test;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.guiceberry.GuiceBerryModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TestEnv extends AbstractModule {
    
    private final class LoggingResponseInterceptor implements HttpResponseInterceptor {
        public void process(HttpResponse resp, HttpContext ctx) throws HttpException, IOException {
            HttpEntity entity = resp.getEntity();
            String contents = EntityUtils.toString(entity);
            resp.setEntity(new StringEntity(contents, ContentType.get(entity)));
            System.out.println("Response:");
            System.out.println(contents);
        }
    }
    
    @Override
    protected void configure() {
        install(new GuiceBerryModule());
    }
    
    @Provides @Singleton HttpClient getHttpClient() {
        HttpClient client = HttpClientBuilder.create().addInterceptorLast(new LoggingResponseInterceptor()).build();
        return client;
    }
}
