package org.tdf.sunflower;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.shell.SpringShellAutoConfiguration;
import org.springframework.shell.jcommander.JCommanderParameterResolverAutoConfiguration;
import org.springframework.shell.jline.JLineShellAutoConfiguration;
import org.springframework.shell.legacy.LegacyAdapterAutoConfiguration;
import org.springframework.shell.standard.StandardAPIAutoConfiguration;
import org.springframework.shell.standard.commands.StandardCommandsAutoConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.annotation.PostConstruct;

// disable spring shell auto configuration to avoid NPE
// test cache
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        SpringShellAutoConfiguration.class,
        JLineShellAutoConfiguration.class,
        // Various Resolvers
        JCommanderParameterResolverAutoConfiguration.class,
        LegacyAdapterAutoConfiguration.class,
        StandardAPIAutoConfiguration.class,
        // Built-In Commands
        StandardCommandsAutoConfiguration.class,
        // Allows ${} support
        PropertyPlaceholderAutoConfiguration.class,
})
@EnableWebSocket
@ComponentScan(excludeFilters = { @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public class TestContext {
    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void init(){
        Start.loadCryptoContext(context.getEnvironment());
    }
}
