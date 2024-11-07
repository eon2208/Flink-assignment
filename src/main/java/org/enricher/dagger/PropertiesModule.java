package org.enricher.dagger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import dagger.Module;
import dagger.Provides;
import org.apache.flink.api.java.utils.ParameterTool;
import org.enricher.config.ExternalServiceProperties;
import org.enricher.config.KafkaProperties;

import javax.inject.Singleton;

@Module
public class PropertiesModule {

    private static final String FLINK_ENVIRONMENT = "flink.environment";

    private final String envPropsPath;

    public PropertiesModule(String... args) {
        ParameterTool programArgs = ParameterTool.fromArgs(args);
        String environment = programArgs.get(FLINK_ENVIRONMENT);
        envPropsPath = String.format("envs/%s.properties", environment);
    }

    @Provides
    @Singleton
    public Config providesJobConfig() {
        return ConfigFactory.load(envPropsPath);
    }

    @Provides
    @Singleton
    public KafkaProperties providesKafkaProperties(Config config) {
        return ConfigBeanFactory.create(config.getConfig("kafka"), KafkaProperties.class);
    }

    @Provides
    @Singleton
    public ExternalServiceProperties providesExternalServiceProperties(Config config) {
        return ConfigBeanFactory.create(config.getConfig("externalService"), ExternalServiceProperties.class);
    }
}
