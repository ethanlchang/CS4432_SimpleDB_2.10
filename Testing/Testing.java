import java.sql.*;
import java.util.ArrayList;
import simpledb.remote.SimpleDriver;

public class Testing {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            Driver d = new SimpleDriver();
            conn = d.connect("jdbc:simpledb://localhost", null);
            Statement stmt = conn.createStatement();
            String s;

            //Clear out SQL Tables
            s = "delete from PERSON ";
            stmt.executeUpdate(s);
            System.out.println("Table PERSON deleted.");
            s = "delete from ACCOUNT ";
            stmt.executeUpdate(s);
            System.out.println("Table ACCOUNT deleted.");

            /* Person
            SSN - int
            Name - vachar(50)
            AccountNumber - int
             */
            s = "create table PERSON(SSN int, Name varchar(50), AccountNumber int)";
            stmt.executeUpdate(s);
            System.out.println("Table PERSON created.");

            s = "insert into PERSON(SSN , Name, AccountNumber) values ";
            String[] personVals = {
                    "(1234567890 , 'John Smith', 1)",
                    "(0987654321 , 'Joe Shmoe', 2)",
                    "(2468013579 , 'Amy Sue', 3)",
                    "(1029384756 , 'Bob Lee', 4)",
                    "(6758493021 , 'Max Fax', 5)"};
            for (int i=0; i<personVals.length; i++)
                stmt.executeUpdate(s + personVals[i]);
            System.out.println("PERSON records inserted.");

            /* Bank Account
            AccountNumber - int
            Balance - float
            OwnerSSN - int
            Bank Branch - varchar(25)
             */
            s = "create table ACCOUNT(AccountNumber int, Balance int, OwnerSSN int, BankBranch varchar(25))";
            stmt.executeUpdate(s);
            System.out.println("Table ACCOUNT created.");

            s = "insert into ACCOUNT(AccountNumber, Balance, OwnerSSN, BankBranch) values ";
            String[] accountVals = {
                    "(1, 47, 1234567890, 'A')",
                    "(2, 22225, 0987654321, 'B')",
                    "(3, 39584, 2468013579, 'A')",
                    "(4, 9583, 1029384756, 'C')",
                    "(5, 64357, 6758493021, 'C')"};
            for (int i=0; i<accountVals.length; i++)
                stmt.executeUpdate(s + accountVals[i]);
            System.out.println("ACCOUNT records inserted.");


            //SQL queries
            String qry;
            ResultSet rs;

            // Testing Query of find 'John Smith' for correctness
            System.out.println("\nTest 1: (Query - Full name of John)");
            qry = "select name "
                    + "from person "
                    + "where name =  'John Smith'";
            rs = stmt.executeQuery(qry);

            while (rs.next()) {
                String name = rs.getString("name");
                //System.out.println("John's name is: " + name);
                String expected = "John Smith";
                if (name.equals(expected))
                    System.out.println("Test 1 - Passed");
                else {
                    System.out.println("Test 1 - Failed");
                    System.out.println("Expected: " + expected + ", Actual: " + name);
                }

            }
            rs.close();

            //Find all accounts in branch 'A' with balance 47
            System.out.println("\nTest 2: (Query - Accounts in branch A with balance of 47)");
            qry = "select accountnumber, balance, bankbranch "
                    + "from account "
                    + "where bankbranch =  'A' "
                    + "and balance = 47 ";
            rs = stmt.executeQuery(qry);

            while (rs.next()) {
                int accountNum = rs.getInt("accountnumber");
                int balance = rs.getInt("balance");
                String branch = rs.getString("bankbranch");
                int expectedNumber = 1;
                int expectedBalance = 47;
                String expectedBranch = "A";
                //System.out.println("Account Number: " + accountNum + "\nBalance: "  + balance + "\nBranch: " + branch);

                if (accountNum == expectedNumber && balance == expectedBalance && branch.equals(expectedBranch))
                    System.out.println("Test 2 - Passed");
                else {
                    System.out.println("Test 2 - Failed");
                    System.out.println("Expected: " + accountNum + ", " + balance + ", " + branch + " , Actual: " + expectedNumber + ", " + expectedBalance + ", " + expectedBranch);
                }
            }
            rs.close();

            //Find all accounts in branch 'C'
            System.out.println("\nTest 3: (Query - Accounts in branch C)");
            qry = "select accountnumber, balance, bankbranch "
                    + "from account "
                    + "where bankbranch =  'C' ";
            rs = stmt.executeQuery(qry);

            ArrayList<Integer> results = new ArrayList<Integer>();
            while (rs.next()) {
                int accountNum = rs.getInt("accountnumber");
                int balance = rs.getInt("balance");
                String branch = rs.getString("bankbranch");
                results.add(accountNum);
                //System.out.println("Account Number: " + accountNum + "\nBalance: "  + balance + "\nBranch: " + branch);
            }
            ArrayList<Integer> expected = new ArrayList<Integer>();
            expected.add(4);
            expected.add(5);
            if (results.equals(expected))
                System.out.println("Test 3 - Passed");
            else {
                System.out.println("Test 3 - Failed");
                System.out.println("Expected: " + expected + ", Actual: " + results);
            }
            rs.close();

        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (conn != null)
                    conn.close();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }

        //Clear Tables
        //
    }
}
