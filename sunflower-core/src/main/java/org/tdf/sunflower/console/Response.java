package org.tdf.sunflower.console;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    static final int OK = 200;
    static final int ERROR = 500;

    private int code;
    // normal output
    private String info;
    // error output
    private String error;
}
