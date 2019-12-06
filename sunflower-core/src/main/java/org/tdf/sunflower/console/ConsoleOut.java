package org.tdf.sunflower.console;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsoleOut {
    static final int OK = 200;
    static final int ERROR = 500;
    private int code;
    // normal output
    private String output;
    // error output
    private String error;
}
