new_local_repository(
    name = "pypi",
    path = "bazel-venv/lib/python2.7/site-packages",
    build_file_content = """
package(default_visibility = ["//visibility:public"])
filegroup(
    name = "daemon",
    srcs = glob(["daemon/**/*"]),
)
filegroup(
    name = "lockfile",
    srcs = glob(["lockfile/**/*"]),
)
filegroup(
    name = "boto",
    srcs = glob(["boto/**/*"]),
)
filegroup(
    name = "requests",
    srcs = glob(["requests/**/*"]),
)
filegroup(
    name = "gevent",
    srcs = glob(["gevent/**/*", "greenlet.so"]),
)
filegroup(
    name = "mock",
    srcs = glob(["mock.py"]),
)
filegroup(
    name = "nose",
    srcs = glob(["nose/**/*"]),
)
filegroup(
    name = "socketwhitelist",
    srcs = glob(["socketwhitelist/**/*"]),
)
filegroup(
    name = "django",
    srcs = glob(["django/**/*"]),
)
filegroup(
    name = "oauthlib",
    srcs = glob(["oauthlib/**/*"]),
)
filegroup(
    name = "six",
    srcs = glob(["six.py"]),
)
filegroup(
    name = "dateutil",
    srcs = glob(["dateutil/**/*"]),
)
filegroup(
    name = "pytz",
    srcs = glob(["pytz/**/*"]),
)
filegroup(
    name = "diff_match_patch",
    srcs = glob(["diff_match_patch/**/*"]),
)
""",
)

maven_jar(name = "c3p0_c3p0_0_9_1_1", artifact = "c3p0:c3p0:jar:0.9.1.1")
maven_jar(name = "ch_qos_logback_logback_classic_1_1_2", artifact = "ch.qos.logback:logback-classic:jar:1.1.2")
maven_jar(name = "ch_qos_logback_logback_core_1_1_2", artifact = "ch.qos.logback:logback-core:jar:1.1.2")
maven_jar(name = "com_fasterxml_classmate_1_0_0", artifact = "com.fasterxml:classmate:jar:1.0.0")
maven_jar(name = "com_fasterxml_jackson_core_jackson_annotations_2_4_5", artifact = "com.fasterxml.jackson.core:jackson-annotations:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_core_jackson_annotations_2_5_0", artifact = "com.fasterxml.jackson.core:jackson-annotations:jar:2.5.0")
maven_jar(name = "com_fasterxml_jackson_core_jackson_core_2_4_5", artifact = "com.fasterxml.jackson.core:jackson-core:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_core_jackson_core_2_5_1", artifact = "com.fasterxml.jackson.core:jackson-core:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_core_jackson_core_2_5_3", artifact = "com.fasterxml.jackson.core:jackson-core:jar:2.5.3")
maven_jar(name = "com_fasterxml_jackson_core_jackson_databind_2_4_5", artifact = "com.fasterxml.jackson.core:jackson-databind:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_core_jackson_databind_2_5_1", artifact = "com.fasterxml.jackson.core:jackson-databind:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_core_jackson_databind_2_5_3", artifact = "com.fasterxml.jackson.core:jackson-databind:jar:2.5.3")
maven_jar(name = "com_fasterxml_jackson_dataformat_jackson_dataformat_xml_2_4_5", artifact = "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_dataformat_jackson_dataformat_yaml_2_4_5", artifact = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_dataformat_jackson_dataformat_yaml_2_5_1", artifact = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_datatype_jackson_datatype_guava_2_5_1", artifact = "com.fasterxml.jackson.datatype:jackson-datatype-guava:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_datatype_jackson_datatype_jdk7_2_5_1", artifact = "com.fasterxml.jackson.datatype:jackson-datatype-jdk7:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_datatype_jackson_datatype_joda_2_4_5", artifact = "com.fasterxml.jackson.datatype:jackson-datatype-joda:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_datatype_jackson_datatype_joda_2_5_1", artifact = "com.fasterxml.jackson.datatype:jackson-datatype-joda:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_jaxrs_jackson_jaxrs_base_2_4_5", artifact = "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_jaxrs_jackson_jaxrs_base_2_5_1", artifact = "com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_jaxrs_jackson_jaxrs_json_provider_2_4_5", artifact = "com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_jaxrs_jackson_jaxrs_json_provider_2_5_1", artifact = "com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_module_jackson_module_afterburner_2_5_1", artifact = "com.fasterxml.jackson.module:jackson-module-afterburner:jar:2.5.1")
maven_jar(name = "com_fasterxml_jackson_module_jackson_module_jaxb_annotations_2_4_5", artifact = "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.4.5")
maven_jar(name = "com_fasterxml_jackson_module_jackson_module_jaxb_annotations_2_5_1", artifact = "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.5.1")
maven_jar(name = "com_google_code_findbugs_annotations_2_0_1", artifact = "com.google.code.findbugs:annotations:jar:2.0.1")
maven_jar(name = "com_google_code_findbugs_jsr305_1_3_9", artifact = "com.google.code.findbugs:jsr305:jar:1.3.9")
maven_jar(name = "com_google_code_findbugs_jsr305_3_0_0", artifact = "com.google.code.findbugs:jsr305:jar:3.0.0")
maven_jar(name = "com_google_code_gson_gson_2_2_4", artifact = "com.google.code.gson:gson:jar:2.2.4")
maven_jar(name = "com_google_code_gson_gson_2_3_1", artifact = "com.google.code.gson:gson:jar:2.3.1")
maven_jar(name = "com_google_guava_guava_18_0", artifact = "com.google.guava:guava:jar:18.0")
maven_jar(name = "com_google_http_client_google_http_client_1_19_0", artifact = "com.google.http-client:google-http-client:jar:1.19.0")
maven_jar(name = "com_google_http_client_google_http_client_gson_1_19_0", artifact = "com.google.http-client:google-http-client-gson:jar:1.19.0")
maven_jar(name = "com_ibatis_ibatis2_common_2_1_7_597", artifact = "com.ibatis:ibatis2-common:jar:2.1.7.597")
maven_jar(name = "com_jayway_jsonpath_json_path_0_9_1", artifact = "com.jayway.jsonpath:json-path:jar:0.9.1")
maven_jar(name = "commons_beanutils_commons_beanutils_1_9_1", artifact = "commons-beanutils:commons-beanutils:jar:1.9.1")
maven_jar(name = "commons_codec_commons_codec_1_3", artifact = "commons-codec:commons-codec:jar:1.3")
maven_jar(name = "commons_codec_commons_codec_1_9", artifact = "commons-codec:commons-codec:jar:1.9")
maven_jar(name = "commons_collections_commons_collections_3_2_1", artifact = "commons-collections:commons-collections:jar:3.2.1")
maven_jar(name = "commons_configuration_commons_configuration_1_9", artifact = "commons-configuration:commons-configuration:jar:1.9")
maven_jar(name = "commons_dbcp_commons_dbcp_1_4", artifact = "commons-dbcp:commons-dbcp:jar:1.4")
maven_jar(name = "commons_dbutils_commons_dbutils_1_6", artifact = "commons-dbutils:commons-dbutils:jar:1.6")
maven_jar(name = "commons_io_commons_io_2_4", artifact = "commons-io:commons-io:jar:2.4")
maven_jar(name = "commons_lang_commons_lang_2_6", artifact = "commons-lang:commons-lang:jar:2.6")
maven_jar(name = "commons_logging_commons_logging_1_1_1", artifact = "commons-logging:commons-logging:jar:1.1.1")
maven_jar(name = "commons_pool_commons_pool_1_5_4", artifact = "commons-pool:commons-pool:jar:1.5.4")
maven_jar(name = "com_sun_mail_javax_mail_1_5_4", artifact = "com.sun.mail:javax.mail:jar:1.5.4")
maven_jar(name = "in_ashwanthkumar_slack_java_webhook_0_0_3", artifact = "in.ashwanthkumar:slack-java-webhook:jar:0.0.3")
maven_jar(name = "io_dropwizard_dropwizard_auth_0_8_1", artifact = "io.dropwizard:dropwizard-auth:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_configuration_0_8_1", artifact = "io.dropwizard:dropwizard-configuration:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_core_0_8_1", artifact = "io.dropwizard:dropwizard-core:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_jackson_0_8_1", artifact = "io.dropwizard:dropwizard-jackson:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_jersey_0_8_1", artifact = "io.dropwizard:dropwizard-jersey:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_jetty_0_8_1", artifact = "io.dropwizard:dropwizard-jetty:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_lifecycle_0_8_1", artifact = "io.dropwizard:dropwizard-lifecycle:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_logging_0_8_1", artifact = "io.dropwizard:dropwizard-logging:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_metrics_0_8_1", artifact = "io.dropwizard:dropwizard-metrics:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_servlets_0_8_1", artifact = "io.dropwizard:dropwizard-servlets:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_util_0_8_1", artifact = "io.dropwizard:dropwizard-util:jar:0.8.1")
maven_jar(name = "io_dropwizard_dropwizard_validation_0_8_1", artifact = "io.dropwizard:dropwizard-validation:jar:0.8.1")
maven_jar(name = "io_dropwizard_metrics_metrics_annotation_3_1_1", artifact = "io.dropwizard.metrics:metrics-annotation:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_core_3_1_1", artifact = "io.dropwizard.metrics:metrics-core:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_healthchecks_3_1_1", artifact = "io.dropwizard.metrics:metrics-healthchecks:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_jersey2_3_1_1", artifact = "io.dropwizard.metrics:metrics-jersey2:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_jetty9_3_1_1", artifact = "io.dropwizard.metrics:metrics-jetty9:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_json_3_1_1", artifact = "io.dropwizard.metrics:metrics-json:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_jvm_3_1_1", artifact = "io.dropwizard.metrics:metrics-jvm:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_logback_3_1_1", artifact = "io.dropwizard.metrics:metrics-logback:jar:3.1.1")
maven_jar(name = "io_dropwizard_metrics_metrics_servlets_3_1_1", artifact = "io.dropwizard.metrics:metrics-servlets:jar:3.1.1")
maven_jar(name = "io_swagger_swagger_annotations_1_5_5", artifact = "io.swagger:swagger-annotations:jar:1.5.5")
maven_jar(name = "io_swagger_swagger_core_1_5_5", artifact = "io.swagger:swagger-core:jar:1.5.5")
maven_jar(name = "io_swagger_swagger_jaxrs_1_5_5", artifact = "io.swagger:swagger-jaxrs:jar:1.5.5")
maven_jar(name = "io_swagger_swagger_jersey2_jaxrs_1_5_5", artifact = "io.swagger:swagger-jersey2-jaxrs:jar:1.5.5")
maven_jar(name = "io_swagger_swagger_models_1_5_5", artifact = "io.swagger:swagger-models:jar:1.5.5")
maven_jar(name = "javax_activation_activation_1_1", artifact = "javax.activation:activation:jar:1.1")
maven_jar(name = "javax_annotation_javax_annotation_api_1_2", artifact = "javax.annotation:javax.annotation-api:jar:1.2")
maven_jar(name = "javax_servlet_javax_servlet_api_3_1_0", artifact = "javax.servlet:javax.servlet-api:jar:3.1.0")
maven_jar(name = "javax_validation_validation_api_1_1_0_Final", artifact = "javax.validation:validation-api:jar:1.1.0.Final")
maven_jar(name = "javax_ws_rs_javax_ws_rs_api_2_0_1", artifact = "javax.ws.rs:javax.ws.rs-api:jar:2.0.1")
maven_jar(name = "javax_ws_rs_javax_ws_rs_api_2_0", artifact = "javax.ws.rs:javax.ws.rs-api:jar:2.0")
maven_jar(name = "joda_time_joda_time_2_2", artifact = "joda-time:joda-time:jar:2.2")
maven_jar(name = "joda_time_joda_time_2_7", artifact = "joda-time:joda-time:jar:2.7")
maven_jar(name = "joda_time_joda_time_2_9_4", artifact = "joda-time:joda-time:jar:2.9.4")
maven_jar(name = "junit_junit_4_12", artifact = "junit:junit:jar:4.12")
maven_jar(name = "log4j_log4j_1_2_17", artifact = "log4j:log4j:jar:1.2.17")
maven_jar(name = "mysql_mysql_connector_java_5_1_17", artifact = "mysql:mysql-connector-java:jar:5.1.17")
maven_jar(name = "mysql_mysql_connector_java_5_1_22", artifact = "mysql:mysql-connector-java:jar:5.1.22")
maven_jar(name = "mysql_mysql_connector_mxj_5_0_12", artifact = "mysql:mysql-connector-mxj:jar:5.0.12")
maven_jar(name = "mysql_mysql_connector_mxj_db_files_5_0_12", artifact = "mysql:mysql-connector-mxj-db-files:jar:5.0.12")
maven_jar(name = "net_minidev_json_smart_1_2", artifact = "net.minidev:json-smart:jar:1.2")
maven_jar(name = "net_sf_ehcache_ehcache_2_8_1", artifact = "net.sf.ehcache:ehcache:jar:2.8.1")
maven_jar(name = "net_sourceforge_argparse4j_argparse4j_0_4_4", artifact = "net.sourceforge.argparse4j:argparse4j:jar:0.4.4")
maven_jar(name = "org_apache_commons_commons_compress_1_8", artifact = "org.apache.commons:commons-compress:jar:1.8")
maven_jar(name = "org_apache_commons_commons_lang3_3_2_1", artifact = "org.apache.commons:commons-lang3:jar:3.2.1")
maven_jar(name = "org_apache_commons_commons_lang3_3_3_2", artifact = "org.apache.commons:commons-lang3:jar:3.3.2")
maven_jar(name = "org_apache_httpcomponents_httpclient_4_0_1", artifact = "org.apache.httpcomponents:httpclient:jar:4.0.1")
maven_jar(name = "org_apache_httpcomponents_httpcore_4_0_1", artifact = "org.apache.httpcomponents:httpcore:jar:4.0.1")
maven_jar(name = "org_apache_maven_maven_artifact_3_0", artifact = "org.apache.maven:maven-artifact:jar:3.0")
maven_jar(name = "org_apache_maven_plugin_tools_maven_plugin_annotations_3_1", artifact = "org.apache.maven.plugin-tools:maven-plugin-annotations:jar:3.1")
maven_jar(name = "org_codehaus_plexus_plexus_utils_3_0", artifact = "org.codehaus.plexus:plexus-utils:jar:3.0")
maven_jar(name = "org_codehaus_woodstox_stax2_api_3_1_4", artifact = "org.codehaus.woodstox:stax2-api:jar:3.1.4")
maven_jar(name = "org_eclipse_jetty_jetty_continuation_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-continuation:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_http_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-http:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_io_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-io:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_security_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-security:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_server_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-server:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_servlet_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-servlet:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_servlets_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-servlets:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_util_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-util:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_webapp_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-webapp:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_jetty_xml_9_2_9_v20150224", artifact = "org.eclipse.jetty:jetty-xml:jar:9.2.9.v20150224")
maven_jar(name = "org_eclipse_jetty_toolchain_setuid_jetty_setuid_java_1_0_2", artifact = "org.eclipse.jetty.toolchain.setuid:jetty-setuid-java:jar:1.0.2")
maven_jar(name = "org_glassfish_hk2_external_aopalliance_repackaged_2_4_0_b10", artifact = "org.glassfish.hk2.external:aopalliance-repackaged:jar:2.4.0-b10")
maven_jar(name = "org_glassfish_hk2_external_asm_all_repackaged_2_2_0_b10", artifact = "org.glassfish.hk2.external:asm-all-repackaged:jar:2.2.0-b10")
maven_jar(name = "org_glassfish_hk2_external_cglib_2_2_0_b10", artifact = "org.glassfish.hk2.external:cglib:jar:2.2.0-b10")
maven_jar(name = "org_glassfish_hk2_external_javax_inject_2_2_0_b10", artifact = "org.glassfish.hk2.external:javax.inject:jar:2.2.0-b10")
maven_jar(name = "org_glassfish_hk2_external_javax_inject_2_4_0_b10", artifact = "org.glassfish.hk2.external:javax.inject:jar:2.4.0-b10")
maven_jar(name = "org_glassfish_hk2_hk2_api_2_2_0_b10", artifact = "org.glassfish.hk2:hk2-api:jar:2.2.0-b10")
maven_jar(name = "org_glassfish_hk2_hk2_api_2_4_0_b10", artifact = "org.glassfish.hk2:hk2-api:jar:2.4.0-b10")
maven_jar(name = "org_glassfish_hk2_hk2_locator_2_2_0_b10", artifact = "org.glassfish.hk2:hk2-locator:jar:2.2.0-b10")
maven_jar(name = "org_glassfish_hk2_hk2_locator_2_4_0_b10", artifact = "org.glassfish.hk2:hk2-locator:jar:2.4.0-b10")
maven_jar(name = "org_glassfish_hk2_hk2_utils_2_2_0_b10", artifact = "org.glassfish.hk2:hk2-utils:jar:2.2.0-b10")
maven_jar(name = "org_glassfish_hk2_hk2_utils_2_4_0_b10", artifact = "org.glassfish.hk2:hk2-utils:jar:2.4.0-b10")
maven_jar(name = "org_glassfish_hk2_osgi_resource_locator_1_0_1", artifact = "org.glassfish.hk2:osgi-resource-locator:jar:1.0.1")
maven_jar(name = "org_glassfish_javax_el_3_0_0", artifact = "org.glassfish:javax.el:jar:3.0.0")
maven_jar(name = "org_glassfish_jersey_bundles_repackaged_jersey_guava_2_17", artifact = "org.glassfish.jersey.bundles.repackaged:jersey-guava:jar:2.17")
maven_jar(name = "org_glassfish_jersey_containers_jersey_container_servlet_2_17", artifact = "org.glassfish.jersey.containers:jersey-container-servlet:jar:2.17")
maven_jar(name = "org_glassfish_jersey_containers_jersey_container_servlet_core_2_17", artifact = "org.glassfish.jersey.containers:jersey-container-servlet-core:jar:2.17")
maven_jar(name = "org_glassfish_jersey_containers_jersey_container_servlet_core_2_1", artifact = "org.glassfish.jersey.containers:jersey-container-servlet-core:jar:2.1")
maven_jar(name = "org_glassfish_jersey_core_jersey_client_2_17", artifact = "org.glassfish.jersey.core:jersey-client:jar:2.17")
maven_jar(name = "org_glassfish_jersey_core_jersey_client_2_1", artifact = "org.glassfish.jersey.core:jersey-client:jar:2.1")
maven_jar(name = "org_glassfish_jersey_core_jersey_common_2_17", artifact = "org.glassfish.jersey.core:jersey-common:jar:2.17")
maven_jar(name = "org_glassfish_jersey_core_jersey_common_2_1", artifact = "org.glassfish.jersey.core:jersey-common:jar:2.1")
maven_jar(name = "org_glassfish_jersey_core_jersey_server_2_17", artifact = "org.glassfish.jersey.core:jersey-server:jar:2.17")
maven_jar(name = "org_glassfish_jersey_core_jersey_server_2_1", artifact = "org.glassfish.jersey.core:jersey-server:jar:2.1")
maven_jar(name = "org_glassfish_jersey_ext_jersey_metainf_services_2_17", artifact = "org.glassfish.jersey.ext:jersey-metainf-services:jar:2.17")
maven_jar(name = "org_glassfish_jersey_media_jersey_media_jaxb_2_17", artifact = "org.glassfish.jersey.media:jersey-media-jaxb:jar:2.17")
maven_jar(name = "org_glassfish_jersey_media_jersey_media_multipart_2_1", artifact = "org.glassfish.jersey.media:jersey-media-multipart:jar:2.1")
maven_jar(name = "org_hamcrest_hamcrest_core_1_1", artifact = "org.hamcrest:hamcrest-core:jar:1.1")
maven_jar(name = "org_hamcrest_hamcrest_core_1_3", artifact = "org.hamcrest:hamcrest-core:jar:1.3")
maven_jar(name = "org_hibernate_hibernate_validator_5_1_3_Final", artifact = "org.hibernate:hibernate-validator:jar:5.1.3.Final")
maven_jar(name = "org_javassist_javassist_3_18_1_GA", artifact = "org.javassist:javassist:jar:3.18.1-GA")
maven_jar(name = "org_javassist_javassist_3_19_0_GA", artifact = "org.javassist:javassist:jar:3.19.0-GA")
maven_jar(name = "org_jboss_logging_jboss_logging_3_1_3_GA", artifact = "org.jboss.logging:jboss-logging:jar:3.1.3.GA")
maven_jar(name = "org_jvnet_mimepull_mimepull_1_8", artifact = "org.jvnet.mimepull:mimepull:jar:1.8")
maven_jar(name = "org_mockito_mockito_core_1_10_8", artifact = "org.mockito:mockito-core:jar:1.10.8")
maven_jar(name = "org_objenesis_objenesis_2_1", artifact = "org.objenesis:objenesis:jar:2.1")
maven_jar(name = "org_quartz_scheduler_quartz_2_2_1", artifact = "org.quartz-scheduler:quartz:jar:2.2.1")
maven_jar(name = "org_reflections_reflections_0_9_10", artifact = "org.reflections:reflections:jar:0.9.10")
maven_jar(name = "org_slf4j_jcl_over_slf4j_1_7_10", artifact = "org.slf4j:jcl-over-slf4j:jar:1.7.10")
maven_jar(name = "org_slf4j_jcl_over_slf4j_1_7_2", artifact = "org.slf4j:jcl-over-slf4j:jar:1.7.2")
maven_jar(name = "org_slf4j_jul_to_slf4j_1_7_10", artifact = "org.slf4j:jul-to-slf4j:jar:1.7.10")
maven_jar(name = "org_slf4j_log4j_over_slf4j_1_7_10", artifact = "org.slf4j:log4j-over-slf4j:jar:1.7.10")
maven_jar(name = "org_slf4j_slf4j_api_1_6_3", artifact = "org.slf4j:slf4j-api:jar:1.6.3")
maven_jar(name = "org_slf4j_slf4j_api_1_6_6", artifact = "org.slf4j:slf4j-api:jar:1.6.6")
maven_jar(name = "org_slf4j_slf4j_api_1_7_10", artifact = "org.slf4j:slf4j-api:jar:1.7.10")
maven_jar(name = "org_slf4j_slf4j_api_1_7_2", artifact = "org.slf4j:slf4j-api:jar:1.7.2")
maven_jar(name = "org_slf4j_slf4j_api_1_7_5", artifact = "org.slf4j:slf4j-api:jar:1.7.5")
maven_jar(name = "org_slf4j_slf4j_log4j12_1_7_2", artifact = "org.slf4j:slf4j-log4j12:jar:1.7.2")
maven_jar(name = "org_tukaani_xz_1_5", artifact = "org.tukaani:xz:jar:1.5")
maven_jar(name = "org_yaml_snakeyaml_1_12", artifact = "org.yaml:snakeyaml:jar:1.12")
