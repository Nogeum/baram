import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCrypt;
public class CheckPortalAccounts {
    public static void main(String[] args) throws Exception {
        var p=new Properties();
        try(var r=Files.newBufferedReader(Path.of("config/application-oracle-local.properties"),StandardCharsets.UTF_8)){p.load(r);}
        var c=new Properties();
        c.setProperty("user",p.getProperty("spring.datasource.username"));
        c.setProperty("password",p.getProperty("spring.datasource.password"));
        c.setProperty("oracle.jdbc.timezoneAsRegion","false");
        try(var conn=DriverManager.getConnection(p.getProperty("spring.datasource.url"),c);
            var stmt=conn.prepareStatement("select login_id, role, active from portal_employee where login_id in ('admin','employee','ADMIN','EMPLOYEE','admin01','user01') order by login_id");
            var rows=stmt.executeQuery()) {
            while(rows.next()) System.out.println("login="+rows.getString(1)+", role="+rows.getString(2)+", active="+rows.getInt(3));
        }
    }
}
