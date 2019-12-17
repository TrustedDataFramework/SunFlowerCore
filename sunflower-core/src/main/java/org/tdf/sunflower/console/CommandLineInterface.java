package org.tdf.sunflower.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@ConditionalOnProperty(value = "sunflower.console.disabled", havingValue = "false")
@Slf4j
public class CommandLineInterface {

    @ShellMethod(value = "shutdown", key = "shutdown")
    public void shutdown() {
        log.info("application will shutdown soon");
        System.exit(0);
    }
}
