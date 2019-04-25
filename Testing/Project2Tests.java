import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import simpledb.remote.SimpleDriver;

public class Project2Tests {
    // CS4432: lowered this to make it work
    final static int maxSize=10000;
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Connection conn=null;
        Driver d = new SimpleDriver();
        String host = "localhost"; //you may change it if your SimpleDB server is running on a different machine
        String url = "jdbc:simpledb://" + host;
        String qry;
        Random rand=null;
        Statement s=null;
        try {
            conn = d.connect(url, null);
            s=conn.createStatement();
            s.executeUpdate("Create table test1" +
                    "( a1 int," +
                    "  a2 int"+
                    ")");
            s.executeUpdate("Create table test2" +
                    "( a1 int," +
                    "  a2 int"+
                    ")");
            s.executeUpdate("Create table test3" +
                    "( a1 int," +
                    "  a2 int"+
                    ")");
            s.executeUpdate("Create table test4" +
                    "( a1 int," +
                    "  a2 int"+
                    ")");
            s.executeUpdate("Create table test5" +
                    "( a1 int," +
                    "  a2 int"+
                    ")");

            s.executeUpdate("create sh index idx1 on test1 (a1)");
            //s.executeUpdate("create ex index idx2 on test2 (a1)");
            s.executeUpdate("create bt index idx3 on test3 (a1)");
            for(int i=1;i<6;i++)
            {
                if(i!=5)
                {
                    rand=new Random(1);// ensure every table gets the same data
                    for(int j=0;j<maxSize;j++)
                    {
                        s.executeUpdate("insert into test"+i+" (a1,a2) values("+rand.nextInt(1000)+","+rand.nextInt(1000)+ ")");
                    }
                }
                else//case where i=5
                {
                    for(int j=0;j<maxSize/2;j++)// insert 10000 records into test5
                    {
                        s.executeUpdate("insert into test"+i+" (a1,a2) values("+j+","+j+ ")");
                    }
                }
            }

            // Run tests
            long start;
            long finish;
            long timeElapsed;

            // Simple Queries
            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test1 "
                    + "where a1 = 1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test1 Query took: " + timeElapsed);

            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test2 "
                    + "where a1 = 1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test2 Query took: " + timeElapsed);

            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test3 "
                    + "where a1 = 1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test3 Query took: " + timeElapsed);

            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test4 "
                    + "where a1 = 1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test4 Query took: " + timeElapsed);

            // Join testing
            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test5 as test5, test1 as test1 "
                    + "where test5.a1 = test1.a1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test1 Join took: " + timeElapsed);

            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test5 as test5, test2 as test2 "
                    + "where test5.a1 = test2.a1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test2 Join took: " + timeElapsed);

            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test5 as test5, test3 as test3 "
                    + "where test5.a1 = test3.a1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test3 Join took: " + timeElapsed);

            start = System.currentTimeMillis();
            qry = "select a1, a2 "
                    + "from test5 as test5, test4 as test4 "
                    + "where test5.a1 = test4.a1 ";
            s.executeQuery(qry);
            finish = System.currentTimeMillis();
            timeElapsed = finish - start;
            System.out.println("Test4 Join took: " + timeElapsed);

            conn.close();

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally
        {
            try {
                conn.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}


