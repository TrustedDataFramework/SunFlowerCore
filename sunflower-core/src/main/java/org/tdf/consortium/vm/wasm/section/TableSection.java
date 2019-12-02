package org.wisdom.consortium.vm.wasm.section;

import lombok.Getter;
import org.wisdom.consortium.vm.wasm.BytesReader;
import org.wisdom.consortium.vm.wasm.types.TableType;

import java.util.List;

/**
 * The table section has the id 4. It decodes into a vector of tables that represent the tables component of a module.
 */
public class TableSection extends Section {

    @Getter
    private List<TableType> tableTypes;

    public TableSection(SectionID id, long size, BytesReader payload) {
        super(id, size, payload);
    }

    @Override
    void readPayload() throws RuntimeException {
        BytesReader reader = getPayload();
        tableTypes = TableType.readTableTypesFrom(reader);
    }

}
