package org.tdf.sunflower.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.tdf.sunflower.SignalHandler;

import static org.tdf.sunflower.ApplicationConstants.SHUTDOWN_SIGNAL;

@ShellComponent
@Slf4j
public class CommandLineInterface {
    @Autowired
    private SignalHandler handler;

    @ShellMethod(value = "shutdown")
    public void shutdown() {
        log.info("application will shutdown soon");
        handler.emit(SHUTDOWN_SIGNAL);
    }
}
