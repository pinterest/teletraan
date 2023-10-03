package com.pinterest.teletraan.universal.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.config.validate.Validated;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PinStatsConfigTest {
  private final Map<String, String> props = new HashMap<>();
  private static final String URI_KEY = "mm.uri";
  private static final String IS_REQUIRED = "is required";

  private final PinStatsConfig config = props::get;

  @BeforeEach
  void setUp() {
    props.clear();
  }

  @Test
  void invalid_uri() {
    props.put(URI_KEY, ":not-a-uri");

    assertLinesMatch(
        Stream.of("must be a valid URI", "must be a valid URI"),
        config.validate().failures().stream().map(Validated.Invalid::getMessage));
  }

  @Test
  void invalid_emptyUri() {
    props.put(URI_KEY, "");

    assertLinesMatch(
        Stream.of(IS_REQUIRED, IS_REQUIRED),
        config.validate().failures().stream().map(Validated.Invalid::getMessage));
  }

  @Test
  void invalid_noPort() {
    props.put(URI_KEY, "tcp://localhost");

    assertLinesMatch(
        Stream.of(IS_REQUIRED),
        config.validate().failures().stream().map(Validated.Invalid::getMessage));
  }

  @Test
  void valid() {
    String host = "example.com";
    int port = 223;
    props.put(URI_KEY, String.format("tcp://%s:%d", host, port));

    assertTrue(config.validate().isValid());
    assertEquals(host, config.host());
    assertEquals(port, config.port());
  }

  @Test
  void namePrefix() {
    String namePrefix = "rodimus.prod";
    props.put("mm.namePrefix", namePrefix);

    assertTrue(config.validate().isValid());
    assertEquals(namePrefix, config.namePrefix());
  }

  @Test
  void defaultConfig() {
    PinStatsConfig defaultConfig = PinStatsConfig.DEFAULT;
    assertTrue(defaultConfig.validate().isValid());
    assertEquals("localhost", defaultConfig.host());
    assertEquals(18126, defaultConfig.port());
    assertEquals("mm.", defaultConfig.namePrefix());
  }

  @Test
  void noConfig_defaultValuesUsed() {
    PinStatsConfig defaultConfig = PinStatsConfig.DEFAULT;
    assertTrue(config.validate().isValid());
    assertEquals(defaultConfig.host(), config.host());
    assertEquals(defaultConfig.port(), config.port());
    assertEquals(defaultConfig.namePrefix(), config.namePrefix());
  }
}
