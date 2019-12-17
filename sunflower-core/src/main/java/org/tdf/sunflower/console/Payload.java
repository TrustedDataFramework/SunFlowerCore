package org.tdf.sunflower.console;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Payload {
    // token = keccak256 uuid(ascii) + createdAt(ascii)
    private String token;
    private String createdAt;
}
