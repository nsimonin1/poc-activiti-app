package net.freelancertech.vaccation.config;

import java.net.InetSocketAddress;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.boolex.OnMarkerEvaluator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.EvaluatorFilter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import io.github.jhipster.config.JHipsterProperties;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;

@Configuration
public class LoggingConfiguration {
	private static final String LOGSTASH_APPENDER_NAME = "LOGSTASH";
	private static final String ASYNC_LOGSTASH_APPENDER_NAME = "ASYNC_LOGSTASH";

	private final Logger log = LoggerFactory.getLogger(LoggingConfiguration.class);

	private final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

	private final String appName;

	private final String serverPort;

	private final String version;

	private final JHipsterProperties jHipsterProperties;

	public LoggingConfiguration(@Value("${spring.application.name}") String appName,
			@Value("${server.port}") String serverPort, JHipsterProperties jHipsterProperties,
			@Value("${info.project.version}") String version) {
		this.appName = appName;
		this.serverPort = serverPort;
		this.jHipsterProperties = jHipsterProperties;
		this.version = version;
		if (jHipsterProperties.getLogging().getLogstash().isEnabled()) {
			addLogstashAppender(context);
			addContextListener(context);
		}
		if (jHipsterProperties.getMetrics().getLogs().isEnabled()) {
			setMetricsMarkerLogbackFilter(context);
		}
	}

	private void addContextListener(LoggerContext context) {
		final LogbackLoggerContextListener loggerContextListener = new LogbackLoggerContextListener();
		loggerContextListener.setContext(context);
		context.addListener(loggerContextListener);
	}

	private void addLogstashAppender(LoggerContext context) {
		log.info("Initializing Logstash logging");

		final LogstashTcpSocketAppender logstashAppender = new LogstashTcpSocketAppender();
		logstashAppender.setName("LOGSTASH");
		logstashAppender.setContext(context);
		final String customFields = "{\"app_name\":\"" + appName + "\",\"version\":\"" + version + "\",\"app_port\":\""
				+ serverPort + "\"}";

		// More documentation is available at:
		// https://github.com/logstash/logstash-logback-encoder
		final LogstashEncoder logstashEncoder = new LogstashEncoder();
		// Set the Logstash appender config from JHipster properties
		logstashEncoder.setCustomFields(customFields);
		// Set the Logstash appender config from JHipster properties
		logstashAppender.addDestinations(new InetSocketAddress(jHipsterProperties.getLogging().getLogstash().getHost(),
				jHipsterProperties.getLogging().getLogstash().getPort()));

		final ShortenedThrowableConverter throwableConverter = new ShortenedThrowableConverter();
		throwableConverter.setRootCauseFirst(true);
		logstashEncoder.setThrowableConverter(throwableConverter);
		logstashEncoder.setCustomFields(customFields);

		logstashAppender.setEncoder(logstashEncoder);
		logstashAppender.start();

		// Wrap the appender in an Async appender for performance
		final AsyncAppender asyncLogstashAppender = new AsyncAppender();
		asyncLogstashAppender.setContext(context);
		asyncLogstashAppender.setName("ASYNC_LOGSTASH");
		asyncLogstashAppender.setQueueSize(jHipsterProperties.getLogging().getLogstash().getQueueSize());
		asyncLogstashAppender.addAppender(logstashAppender);
		asyncLogstashAppender.start();

		context.getLogger("ROOT").addAppender(asyncLogstashAppender);
	}

	// Configure a log filter to remove "metrics" logs from all appenders except the
	// "LOGSTASH" appender
	private void setMetricsMarkerLogbackFilter(LoggerContext context) {
		log.info("Filtering metrics logs from all appenders except the {} appender", LOGSTASH_APPENDER_NAME);
		final OnMarkerEvaluator onMarkerMetricsEvaluator = new OnMarkerEvaluator();
		onMarkerMetricsEvaluator.setContext(context);
		onMarkerMetricsEvaluator.addMarker("metrics");
		onMarkerMetricsEvaluator.start();
		final EvaluatorFilter<ILoggingEvent> metricsFilter = new EvaluatorFilter<>();
		metricsFilter.setContext(context);
		metricsFilter.setEvaluator(onMarkerMetricsEvaluator);
		metricsFilter.setOnMatch(FilterReply.DENY);
		metricsFilter.start();

		for (final ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
			for (final Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders(); it.hasNext();) {
				final Appender<ILoggingEvent> appender = it.next();
				if (!appender.getName().equals(ASYNC_LOGSTASH_APPENDER_NAME)) {
					log.debug("Filter metrics logs from the {} appender", appender.getName());
					appender.setContext(context);
					appender.addFilter(metricsFilter);
					appender.start();
				}
			}
		}
	}

	/**
	 * Logback configuration is achieved by configuration file and API. When
	 * configuration file change is detected, the configuration is reset. This
	 * listener ensures that the programmatic configuration is also re-applied after
	 * reset.
	 */
	class LogbackLoggerContextListener extends ContextAwareBase implements LoggerContextListener {

		@Override
		public boolean isResetResistant() {
			return true;
		}

		@Override
		public void onStart(LoggerContext context) {
			addLogstashAppender(context);
		}

		@Override
		public void onReset(LoggerContext context) {
			addLogstashAppender(context);
		}

		@Override
		public void onStop(LoggerContext context) {
			// Nothing to do.
		}

		@Override
		public void onLevelChange(ch.qos.logback.classic.Logger logger, Level level) {
			// Nothing to do.
		}
	}

}
