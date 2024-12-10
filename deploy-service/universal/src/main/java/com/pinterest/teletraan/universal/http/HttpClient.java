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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
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
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);

    @Getter private final OkHttpClient okHttpClient;

    @Builder(buildMethodName = "buildInternal")
    private HttpClient(
            int maxRetries,
            long retryInterval,
            boolean useProxy,
            String httpProxyAddr,
            int httpProxyPort,
            Duration callTimeout,
            Supplier<String> authorizationSupplier) {
        observationRegistry
                .observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry));

        callTimeout = callTimeout == null ? DEFAULT_TIMEOUT : callTimeout;

        OkHttpClient.Builder clientBuilder =
                sharedOkHttpClient
                        .newBuilder()
                        .callTimeout(callTimeout)
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

    // Partial builder implementation to allow for default values and custom build method
    // lombok will generate the rest of the builder
    public static class HttpClientBuilder {
        private int maxRetries = 3;
        private long retryInterval = 500;
        private boolean useProxy = false;
        private String httpProxyAddr = "localhost";
        private int httpProxyPort = 19193;

        public HttpClient build() {
            log.info("building HttpClient with configs: {}", this.toString());
            return buildInternal();
        }
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
        return makeCall(createRequestBuilder(url, params, headers).build());
    }

    public String post(String url, String body, Map<String, String> headers) throws IOException {
        return makeCall(createRequestBuilder(url, null, headers).post(buildJsonBody(body)).build());
    }

    public String put(String url, String body, Map<String, String> headers) throws IOException {
        return makeCall(createRequestBuilder(url, null, headers).put(buildJsonBody(body)).build());
    }

    public String delete(String url, String body, Map<String, String> headers) throws IOException {
        return makeCall(
                createRequestBuilder(url, null, headers).delete(buildJsonBody(body)).build());
    }

    private Request.Builder createRequestBuilder(
            String url, Map<String, String> params, Map<String, String> headers) {
        return new Request.Builder().url(buildUrl(url, params)).headers(buildHeaders(headers));
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

            if (!response.isSuccessful()) {
                mapResponseToException(responseCode, responseBody);
            }
            return responseBody;
        }
    }

    protected void mapResponseToException(int responseCode, String responseBody)
            throws WebApplicationException {
        log.info(
                "Egress HTTP failed with response code: {}, response body: {}",
                responseCode,
                responseBody);

        switch (responseCode) {
            case 400:
                throw new BadRequestException(responseBody);
            case 401:
                throw new ForbiddenException(responseBody);
            case 403:
                throw new NotAuthorizedException(responseBody);
            case 404:
                throw new NotFoundException(responseBody);
            default:
                if (responseCode < 500) {
                    throw new ClientErrorException(responseBody, responseCode);
                } else if (responseCode < 600) {
                    throw new ServerErrorException(responseBody, responseCode);
                } else {
                    throw new WebApplicationException(responseBody, responseCode);
                }
        }
    }
}
