package org.tdf.sunflower.console;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenFile {
    private String token;
    private int port;
}
