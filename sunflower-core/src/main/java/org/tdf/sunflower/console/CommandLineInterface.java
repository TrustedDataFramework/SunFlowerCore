package org.tdf.sunflower.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@Slf4j
public class CommandLineInterface {

    @ShellMethod(value = "shutdown")
    public void shutdown() {
        log.info("application will shutdown soon");
        System.exit(0);
    }
}
