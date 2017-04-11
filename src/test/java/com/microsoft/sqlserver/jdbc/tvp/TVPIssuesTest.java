/*
 * Microsoft JDBC Driver for SQL Server
 * 
 * Copyright(c) Microsoft Corporation All rights reserved.
 * 
 * This program is made available under the terms of the MIT License. See the LICENSE file in the project root for more information.
 */
package com.microsoft.sqlserver.jdbc.tvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;
import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import com.microsoft.sqlserver.testframework.AbstractTest;
import com.microsoft.sqlserver.testframework.Utils;

@RunWith(JUnitPlatform.class)
public class TVPIssuesTest extends AbstractTest {

    static Connection connection = null;
    static Statement stmt = null;
    private static String tvpName = "tryTVP_RS_varcharMax_4001_Issue";
    private static String srcTable = "tryTVP_RS_varcharMax_4001_Issue_src";
    private static String desTable = "tryTVP_RS_varcharMax_4001_Issue_dest";

    @Test
    public void tryTVP_RS_varcharMax_4001_Issue() throws Exception {

        setup();

        SQLServerStatement st = (SQLServerStatement) connection.createStatement();
        ResultSet rs = st.executeQuery("select * from " + srcTable);

        SQLServerPreparedStatement pstmt = (SQLServerPreparedStatement) connection.prepareStatement("INSERT INTO " + desTable + " select * from ? ;");

        pstmt.setStructured(1, tvpName, rs);
        pstmt.execute();

        testDestinationTable();
    }

    private void testDestinationTable() throws SQLException, IOException {
        ResultSet rs = connection.createStatement().executeQuery("select * from " + desTable);
        while (rs.next()) {
            assertEquals(rs.getString(1).length(), 4001, " The inserted length is truncated or not correct!");
        }
        if (null != rs) {
            rs.close();
        }
    }

    private static void populateSourceTable() throws SQLException {
        String sql = "insert into " + srcTable + " values (?)";

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 4001; i++) {
            sb.append("a");
        }
        String value = sb.toString();

        SQLServerPreparedStatement pstmt = (SQLServerPreparedStatement) connection.prepareStatement(sql);

        pstmt.setString(1, value);
        pstmt.execute();
    }

    @BeforeAll
    public static void beforeAll() throws SQLException {

        connection = DriverManager.getConnection(connectionString);
        stmt = connection.createStatement();

        Utils.dropTableIfExists(tvpName, stmt);
        Utils.dropTableIfExists(srcTable, stmt);
        Utils.dropTableIfExists(desTable, stmt);

        String sql = "create table " + srcTable + " (c1 varchar(max) null);";
        stmt.execute(sql);

        sql = "create table " + desTable + " (c1 varchar(max) null);";
        stmt.execute(sql);

        String TVPCreateCmd = "CREATE TYPE " + tvpName + " as table (c1 varchar(max) null)";
        stmt.executeUpdate(TVPCreateCmd);

        populateSourceTable();
    }

    @AfterAll
    public static void terminateVariation() throws SQLException {
        Utils.dropTableIfExists(tvpName, stmt);
        Utils.dropTableIfExists(srcTable, stmt);
        Utils.dropTableIfExists(desTable, stmt);
        if (null != connection) {
            connection.close();
        }
        if (null != stmt) {
            stmt.close();
        }
    }
}
