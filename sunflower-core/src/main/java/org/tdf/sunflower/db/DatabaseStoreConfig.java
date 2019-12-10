package org.tdf.sunflower.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseStoreConfig {
    private String name;
    @JsonProperty("max-open-files")
    private int dataMaxOpenFiles;
    private String directory;
    private boolean reset;
}