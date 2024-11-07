/**
 * Copyright (c) 2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.universal.http;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpObservationInterceptor;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import okhttp3.logging.HttpLoggingInterceptor.Logger;

@Slf4j
public class HttpClient {
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final ObservationRegistry observationRegistry = ObservationRegistry.create();
    private static final OkHttpClient sharedOkHttpClient = new OkHttpClient();

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final boolean DEFAULT_USE_PROXY = false;
    private static final String DEFAULT_HTTP_PROXY_ADDR = "localhost";
    private static final int DEFAULT_PROXY_PORT = 19193;
    private static final long RETRY_INTERVAL = 500;

    @Getter private final OkHttpClient okHttpClient;

    public HttpClient() {
        this(null, null, null, null, null, null);
    }

    @Builder(access = AccessLevel.PUBLIC)
    public HttpClient(
            Integer maxRetries,
            Long retryInterval,
            Boolean useProxy,
            String httpProxyAddr,
            Integer httpProxyPort,
            Supplier<String> authorizationSupplier) {
        if (maxRetries == null) {
            maxRetries = DEFAULT_MAX_RETRIES;
        }
        if (useProxy == null) {
            useProxy = DEFAULT_USE_PROXY;
        }
        if (httpProxyAddr == null) {
            httpProxyAddr = DEFAULT_HTTP_PROXY_ADDR;
        }
        if (httpProxyPort == null) {
            httpProxyPort = DEFAULT_PROXY_PORT;
        }
        if (retryInterval == null) {
            retryInterval = RETRY_INTERVAL;
        }

        observationRegistry
                .observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry));

        OkHttpClient.Builder clientBuilder =
                sharedOkHttpClient
                        .newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .readTimeout(Duration.ofSeconds(15))
                        .addInterceptor(createHttpLoggingInterceptor())
                        .addInterceptor(observationInterceptorBuilder().build())
                        .addInterceptor(new RetryInterceptor(maxRetries, retryInterval));
        if (useProxy) {
            clientBuilder.proxy(
                    new Proxy(
                            Proxy.Type.HTTP, new InetSocketAddress(httpProxyAddr, httpProxyPort)));
        }
        if (authorizationSupplier != null) {
            clientBuilder.addInterceptor(
                    (chain) -> {
                        Request request = chain.request();
                        return chain.proceed(
                                request.newBuilder()
                                        .header("Authorization", authorizationSupplier.get())
                                        .build());
                    });
        }
        okHttpClient = clientBuilder.build();
    }

    private static HttpLoggingInterceptor createHttpLoggingInterceptor() {
        HttpLoggingInterceptor logging =
                new HttpLoggingInterceptor(
                        new Logger() {
                            @Override
                            public void log(String message) {
                                if (log.isTraceEnabled()) {
                                    log.trace(message);
                                } else {
                                    log.debug(message);
                                }
                            }
                        });
        if (log.isTraceEnabled()) {
            logging.setLevel(Level.BODY);
        } else if (log.isDebugEnabled()) {
            logging.setLevel(Level.BASIC);
        } else {
            logging.setLevel(Level.NONE);
        }
        logging.redactHeader("Authorization");
        logging.redactHeader("Cookie");
        return logging;
    }

    private static OkHttpObservationInterceptor.Builder observationInterceptorBuilder() {
        return OkHttpObservationInterceptor.builder(observationRegistry, "okhttp");
    }

    public String get(String url, Map<String, String> params, Map<String, String> headers)
            throws IOException {
        return makeCall(createRequestBuilder(url, headers).build());
    }

    public String post(String url, String body, Map<String, String> headers) throws IOException {
        return makeCall(createRequestBuilder(url, headers).post(buildJsonBody(body)).build());
    }

    public String put(String url, String body, Map<String, String> headers) throws IOException {
        return makeCall(createRequestBuilder(url, headers).put(buildJsonBody(body)).build());
    }

    public String delete(String url, String body, Map<String, String> headers) throws IOException {
        return makeCall(createRequestBuilder(url, headers).delete(buildJsonBody(body)).build());
    }

    private Request.Builder createRequestBuilder(String url, Map<String, String> headers) {
        return new Request.Builder().url(buildUrl(url, null)).headers(buildHeaders(headers));
    }

    public Headers buildHeaders(Map<String, String> rawHeaders) {
        Headers.Builder headersBuilder = new Headers.Builder();
        if (rawHeaders != null) {
            rawHeaders.forEach(headersBuilder::set);
        }
        return headersBuilder.build();
    }

    public HttpUrl buildUrl(String url, Map<String, String> params) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            params.forEach(urlBuilder::addQueryParameter);
        }
        return urlBuilder.build();
    }

    public RequestBody buildJsonBody(String json) {
        if (json == null) {
            return null;
        }
        return RequestBody.create(json, MEDIA_TYPE_JSON);
    }

    String makeCall(Request request) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            int responseCode = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";

            if (response.isSuccessful()) {
                return responseBody;
            } else if (responseCode >= 400 && responseCode < 500) {
                throw new ClientErrorException(responseBody, responseCode);
            } else {
                throw new ServerErrorException(responseBody, responseCode);
            }
        }
    }
}
