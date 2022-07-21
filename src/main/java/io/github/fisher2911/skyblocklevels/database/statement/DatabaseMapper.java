package io.github.fisher2911.skyblocklevels.database.statement;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface DatabaseMapper<T> {

    T map(ResultSet resultSet) throws SQLException;

}
