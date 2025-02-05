/**
 * Copyright (c) 2023-2025 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.metrics.micrometer;

import io.micrometer.common.lang.Nullable;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.util.DoubleFormat;
import io.micrometer.opentsdb.OpenTSDBDistributionSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

// Most of the code is derived from from OpenTSDBMeterRegistry
// https://github.com/micrometer-metrics/micrometer/blob/c959eb3e7629363080c4628e70b31f28d044ef3c/implementations/micrometer-registry-opentsdb/src/main/java/io/micrometer/opentsdb/OpenTSDBMeterRegistry.java
@AllArgsConstructor
public class PinStatsPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(PinStatsPublisher.class);
    private static final String COUNT = "count";
    private final PinStatsConfig config;
    private final Clock clock;
    private final TimeUnit baseTimeUnit;
    private final NamingConvention namingConvention;

    protected List<Tag> getConventionTags(Meter.Id id) {
        return id.getConventionTags(namingConvention);
    }

    protected String getConventionName(Meter.Id id) {
        return id.getConventionName(namingConvention);
    }

    /** Convert a double to its string representation in Go. */
    private static String doubleToGoString(double d) {
        if (d == Double.POSITIVE_INFINITY || d == Double.MAX_VALUE || d == Long.MAX_VALUE) {
            return "+Inf";
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return "-Inf";
        }
        if (Double.isNaN(d)) {
            return "NaN";
        }
        return Double.toString(d);
    }

    protected Stream<String> writeSummary(DistributionSummary summary) {
        long wallTime = clock.wallTime();

        final ValueAtPercentile[] percentileValues = summary.takeSnapshot().percentileValues();
        final CountAtBucket[] histogramCounts =
                ((OpenTSDBDistributionSummary) summary).histogramCounts();
        double count = summary.count();

        List<String> metrics = new ArrayList<>();

        metrics.add(writeMetricWithSuffix(summary.getId(), COUNT, wallTime, count));
        metrics.add(writeMetricWithSuffix(summary.getId(), "sum", wallTime, summary.totalAmount()));
        metrics.add(writeMetricWithSuffix(summary.getId(), "max", wallTime, summary.max()));

        if (percentileValues.length > 0) {
            metrics.addAll(writePercentiles(summary, wallTime, percentileValues));
        }

        if (histogramCounts.length > 0) {
            metrics.addAll(writeHistogram(wallTime, summary, histogramCounts, count, baseTimeUnit));
        }

        return metrics.stream();
    }

    protected Stream<String> writeFunctionTimer(FunctionTimer timer) {
        long wallTime = clock.wallTime();

        return Stream.of(
                writeMetricWithSuffix(timer.getId(), COUNT, wallTime, timer.count()),
                // not applicable
                // writeMetricWithSuffix(timer.getId(), "avg", wallTime,
                // timer.mean(baseTimeUnit)),
                writeMetricWithSuffix(
                        timer.getId(), "sum", wallTime, timer.totalTime(baseTimeUnit)));
    }

    protected Stream<String> writeTimer(Timer timer) {
        long wallTime = clock.wallTime();

        HistogramSnapshot histogramSnapshot = timer.takeSnapshot();
        final ValueAtPercentile[] percentileValues = histogramSnapshot.percentileValues();
        final CountAtBucket[] histogramCounts = histogramSnapshot.histogramCounts();
        double count = timer.count();

        List<String> metrics = new ArrayList<>();

        metrics.add(writeMetricWithSuffix(timer.getId(), COUNT, wallTime, count));
        metrics.add(
                writeMetricWithSuffix(
                        timer.getId(), "sum", wallTime, timer.totalTime(baseTimeUnit)));
        metrics.add(writeMetricWithSuffix(timer.getId(), "max", wallTime, timer.max(baseTimeUnit)));

        if (percentileValues.length > 0) {
            metrics.addAll(writePercentiles(timer, wallTime, percentileValues));
        }

        if (histogramCounts.length > 0) {
            metrics.addAll(writeHistogram(wallTime, timer, histogramCounts, count, baseTimeUnit));
        }

        return metrics.stream();
    }

    private List<String> writePercentiles(
            Meter meter, long wallTime, ValueAtPercentile[] percentileValues) {
        List<String> metrics = new ArrayList<>(percentileValues.length);

        boolean forTimer = meter instanceof Timer;
        // satisfies https://prometheus.io/docs/concepts/metric_types/#summary
        for (ValueAtPercentile v : percentileValues) {
            metrics.add(
                    writeMetric(
                            meter.getId()
                                    .withTag(
                                            new ImmutableTag(
                                                    "quantile", doubleToGoString(v.percentile()))),
                            wallTime,
                            (forTimer ? v.value(baseTimeUnit) : v.value())));
        }

        return metrics;
    }

    private List<String> writeHistogram(
            long wallTime,
            Meter meter,
            CountAtBucket[] histogramCounts,
            double count,
            @Nullable TimeUnit timeUnit) {
        List<String> metrics = new ArrayList<>(histogramCounts.length);

        // satisfies https://prometheus.io/docs/concepts/metric_types/#histogram,
        // which is at least SOME standard histogram format to follow
        for (CountAtBucket c : histogramCounts) {
            metrics.add(
                    writeMetricWithSuffix(
                            meter.getId()
                                    .withTag(
                                            new ImmutableTag(
                                                    "le",
                                                    doubleToGoString(
                                                            timeUnit == null
                                                                    ? c.bucket()
                                                                    : c.bucket(timeUnit)))),
                            "bucket",
                            wallTime,
                            c.count()));
        }

        // the +Inf bucket should always equal `count`
        metrics.add(
                writeMetricWithSuffix(
                        meter.getId().withTag(new ImmutableTag("le", "+Inf")),
                        "bucket",
                        wallTime,
                        count));

        return metrics;
    }

    protected Stream<String> writeFunctionCounter(FunctionCounter counter) {
        double count = counter.count();
        if (Double.isFinite(count)) {
            return Stream.of(writeMetric(counter.getId(), clock.wallTime(), count));
        }
        return Stream.empty();
    }

    protected Stream<String> writeCounter(Counter counter) {
        return Stream.of(writeMetric(counter.getId(), clock.wallTime(), counter.count()));
    }

    protected Stream<String> writeGauge(Gauge gauge) {
        double value = gauge.value();
        if (Double.isFinite(value)) {
            return Stream.of(writeMetric(gauge.getId(), clock.wallTime(), value));
        }
        return Stream.empty();
    }

    protected Stream<String> writeTimeGauge(TimeGauge timeGauge) {
        double value = timeGauge.value(baseTimeUnit);
        if (Double.isFinite(value)) {
            return Stream.of(writeMetric(timeGauge.getId(), clock.wallTime(), value));
        }
        return Stream.empty();
    }

    protected Stream<String> writeLongTaskTimer(LongTaskTimer timer) {
        long wallTime = clock.wallTime();

        HistogramSnapshot histogramSnapshot = timer.takeSnapshot();
        final ValueAtPercentile[] percentileValues = histogramSnapshot.percentileValues();
        final CountAtBucket[] histogramCounts = histogramSnapshot.histogramCounts();
        double count = timer.activeTasks();

        List<String> metrics = new ArrayList<>();

        metrics.add(writeMetricWithSuffix(timer.getId(), "active.count", wallTime, count));
        metrics.add(
                writeMetricWithSuffix(
                        timer.getId(), "duration.sum", wallTime, timer.duration(baseTimeUnit)));
        metrics.add(writeMetricWithSuffix(timer.getId(), "max", wallTime, timer.max(baseTimeUnit)));

        if (percentileValues.length > 0) {
            metrics.addAll(writePercentiles(timer, wallTime, percentileValues));
        }

        if (histogramCounts.length > 0) {
            metrics.addAll(writeHistogram(wallTime, timer, histogramCounts, count, baseTimeUnit));
        }

        return metrics.stream();
    }

    protected Stream<String> writeCustomMetric(Meter meter) {
        long wallTime = clock.wallTime();

        List<Tag> tags = getConventionTags(meter.getId());

        return StreamSupport.stream(meter.measure().spliterator(), false)
                .map(
                        ms -> {
                            Tags localTags =
                                    Tags.concat(tags, "statistics", ms.getStatistic().toString());
                            String name = getConventionName(meter.getId());

                            switch (ms.getStatistic()) {
                                case TOTAL:
                                case TOTAL_TIME:
                                    name += ".sum";
                                    break;
                                case MAX:
                                    name += ".max";
                                    break;
                                case ACTIVE_TASKS:
                                    name += ".active.count";
                                    break;
                                case DURATION:
                                    name += ".duration.sum";
                                    break;
                                default:
                                    break;
                            }

                            return new PutFormatMetricBuilder()
                                    .name(name)
                                    .datapoints(wallTime, ms.getValue())
                                    .tags(localTags)
                                    .build();
                        });
    }

    protected String writeMetricWithSuffix(
            Meter.Id id, String suffix, long wallTime, double value) {
        // usually tagKeys and metricNames naming rules are the same
        // but we can't call getConventionName again after adding suffix
        return new PutFormatMetricBuilder()
                .name(
                        suffix.isEmpty()
                                ? getConventionName(id)
                                : getConventionName(id) + "." + namingConvention.tagKey(suffix))
                .datapoints(wallTime, value)
                .tags(getConventionTags(id))
                .build();
    }

    protected String writeMetric(Meter.Id id, long wallTime, double value) {
        return writeMetricWithSuffix(id, "", wallTime, value);
    }

    private static class PutFormatMetricBuilder {

        private final StringBuilder sb = new StringBuilder();

        PutFormatMetricBuilder name(String name) {
            sb.append("put ").append(name);
            return this;
        }

        PutFormatMetricBuilder field(String key, String value) {
            if (key.trim().isEmpty() || value.trim().isEmpty()) {
                return this;
            }
            if (sb.length() > 1) {
                sb.append(' ');
            }
            sb.append(key.trim()).append("=").append(value.trim());
            return this;
        }

        PutFormatMetricBuilder datapoints(long wallTime, double value) {
            sb.append(' ').append(wallTime).append(' ').append(DoubleFormat.wholeOrDecimal(value));
            return this;
        }

        PutFormatMetricBuilder tags(Iterable<Tag> tags) {
            PutFormatMetricBuilder tagBuilder = new PutFormatMetricBuilder();
            for (Tag tag : tags) {
                tagBuilder.field(tag.getKey(), tag.getValue());
            }

            sb.append(' ').append(tagBuilder.build());
            return this;
        }

        String build() {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            return sb.toString();
        }
    }

    /**
     * A function that publishes metrics to a socket using netty and reactor.
     *
     * @param metrics to send
     */
    public void publish(List<Meter> metrics) {
        Flux<String> metricsFlux =
                Flux.fromIterable(metrics)
                        .flatMap(
                                m ->
                                        Flux.fromStream(
                                                m.match(
                                                        this::writeGauge,
                                                        this::writeCounter,
                                                        this::writeTimer,
                                                        this::writeSummary,
                                                        this::writeLongTaskTimer,
                                                        this::writeTimeGauge,
                                                        this::writeFunctionCounter,
                                                        this::writeFunctionTimer,
                                                        this::writeCustomMetric)))
                        .onErrorResume(
                                e -> {
                                    LOG.error("failed to write metric", e);
                                    return Mono.empty();
                                });

        Connection connection =
                TcpClient.create()
                        .host(config.host())
                        .port(config.port())
                        .handle(
                                (inbound, outbound) ->
                                        outbound.sendString(
                                                metricsFlux
                                                        .onErrorResume(
                                                                e -> {
                                                                    LOG.warn(
                                                                            "failed to send to metrics agent",
                                                                            e);
                                                                    return Mono.empty();
                                                                })
                                                        .doOnComplete(
                                                                () ->
                                                                        LOG.debug(
                                                                                "sent {} metrics",
                                                                                metrics.size()))))
                        .connectNow();
        connection.onDispose().block();
    }
}
