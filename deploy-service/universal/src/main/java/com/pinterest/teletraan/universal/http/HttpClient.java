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
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private static final ObservationRegistry observationRegistry = ObservationRegistry.create();
    private static final OkHttpClient sharedOkHttpClient = new OkHttpClient();
    /**
     * The HTTP client used for making network requests. This client is an instance of {@link
     * OkHttpClient}.
     */
    @Getter private final OkHttpClient httpClient;

    public HttpClient() {
        this(false, null, 0, null);
    }

    public HttpClient(Supplier<String> authorizationSupplier) {
        this(false, null, 0, authorizationSupplier);
    }

    public HttpClient(boolean useProxy, String httpProxyAddr, int httpProxyPort) {
        this(useProxy, httpProxyAddr, httpProxyPort, null);
    }

    public HttpClient(
            boolean useProxy,
            String httpProxyAddr,
            int httpProxyPort,
            Supplier<String> authorizationSupplier) {
        observationRegistry
                .observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(Metrics.globalRegistry));

        OkHttpClient.Builder clientBuilder =
                sharedOkHttpClient
                        .newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .readTimeout(Duration.ofSeconds(15))
                        .addInterceptor(observationInterceptorBuilder().build());
        if (useProxy) {
            String proxyAddr = httpProxyAddr != null ? httpProxyAddr : "localhost";
            int proxyPort = httpProxyPort > 0 ? httpProxyPort : 19193;
            clientBuilder.proxy(
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddr, proxyPort)));
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
        httpClient = clientBuilder.build();
    }

    private static OkHttpObservationInterceptor.Builder observationInterceptorBuilder() {
        return OkHttpObservationInterceptor.builder(observationRegistry, "okhttp.requests");
    }

    public String get(String url, Map<String, String> params, Map<String, String> headers)
            throws IOException {
        Request request =
                new Request.Builder()
                        .url(buildUrl(url, params))
                        .headers(buildHeaders(headers))
                        .build();
        return makeCall(request);
    }

    public String post(String url, String body, Map<String, String> headers) throws IOException {
        Request request =
                new Request.Builder()
                        .url(buildUrl(url, null))
                        .headers(buildHeaders(headers))
                        .post(RequestBody.create(body, MEDIA_TYPE_JSON))
                        .build();
        return makeCall(request);
    }

    public String put(String url, String body, Map<String, String> headers) throws IOException {
        Request request =
                new Request.Builder()
                        .url(buildUrl(url, null))
                        .headers(buildHeaders(headers))
                        .put(RequestBody.create(body, MEDIA_TYPE_JSON))
                        .build();
        return makeCall(request);
    }

    public String delete(String url, String body, Map<String, String> headers) throws IOException {
        Request request =
                new Request.Builder()
                        .url(buildUrl(url, null))
                        .headers(buildHeaders(headers))
                        .delete(RequestBody.create(body, MEDIA_TYPE_JSON))
                        .build();
        return makeCall(request);
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

    String makeCall(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }
}
