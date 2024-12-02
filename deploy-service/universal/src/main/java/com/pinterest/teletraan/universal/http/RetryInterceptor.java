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

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private static final ImmutableList<Integer> RETRY_CODES =
            ImmutableList.of(429, 500, 502, 503, 504);
    private final int maxRetries;
    private final long retryInterval;

    public RetryInterceptor(int maxRetries, long retryInterval) {
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        int tryCount = 1;

        while (shouldRetry(response) && tryCount < maxRetries) {
            long backoff = (long) Math.pow(2, (tryCount - 1)) * retryInterval;
            response.close();
            try {
                TimeUnit.MILLISECONDS.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Retry interrupted", e);
            }
            response = chain.proceed(request);
            tryCount++;
        }

        return response;
    }

    private boolean shouldRetry(Response response) {
        return RETRY_CODES.contains(response.code());
    }
}
